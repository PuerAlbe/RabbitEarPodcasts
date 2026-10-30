package com.rabbitear.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

sealed interface ResolutionResult {
    data class Success(val url: String, val source: String) : ResolutionResult
    data object NotFound : ResolutionResult
    data class NetworkError(val cause: Throwable) : ResolutionResult
}

data class ArtworkCandidates(
    val feedItunesImage: String? = null,
    val feedImageUrl: String? = null,
    val feedMediaImage: String? = null,
    val recentEpisodeItunesImage: String? = null,
    val recentEpisodeMediaThumbnail: String? = null,
    val recentEpisodeImageEnclosure: String? = null,
    val feedLinkHtmlPageUrl: String? = null
)

class ArtworkResolver(
    private val client: OkHttpClient = defaultArtworkClient()
) {
    companion object {
        private const val TAG = "ArtworkResolver"
        const val USER_AGENT = "RabbitEar/1.0 (Android Podcast Player; +https://github.com/rabbitear)"

        fun defaultArtworkClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .build()
                    chain.proceed(request)
                }
                .build()
        }

        fun normalizeUrl(candidate: String?, baseUrl: String?): String? {
            if (candidate.isNullOrBlank()) return null
            var trimmed = candidate.trim().trim('"', '\'', '`')
            if (trimmed.isEmpty()) return null

            if (trimmed.startsWith("data:", ignoreCase = true)) {
                return if (trimmed.startsWith("data:image/", ignoreCase = true)) trimmed else null
            }

            if (trimmed.startsWith("//")) {
                trimmed = "https:$trimmed"
            }

            if (!trimmed.startsWith("http://", ignoreCase = true) &&
                !trimmed.startsWith("https://", ignoreCase = true)
            ) {
                if (!baseUrl.isNullOrBlank()) {
                    try {
                        val baseUri = URI(baseUrl.trim())
                        val resolved = baseUri.resolve(trimmed)
                        trimmed = resolved.toString()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to resolve relative URL '$trimmed' against '$baseUrl': ${e.message}")
                    }
                }
            }

            if (trimmed.startsWith("http://", ignoreCase = true)) {
                Log.i(TAG, "Upgrading HTTP URL to HTTPS: '$trimmed'")
                trimmed = "https" + trimmed.substring(4)
            }

            return try {
                val uri = URI(trimmed)
                val scheme = uri.scheme?.lowercase()
                val host = uri.host
                if ((scheme == "http" || scheme == "https") && !host.isNullOrBlank()) {
                    trimmed
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun resolveArtwork(
        feedUrl: String,
        candidates: ArtworkCandidates,
        validateHttp: Boolean = false
    ): ResolutionResult = withContext(Dispatchers.IO) {
        val attempts = listOf(
            "a. Feed <itunes:image>" to candidates.feedItunesImage,
            "b. Feed <image><url>" to candidates.feedImageUrl,
            "c. Feed <media:content/thumbnail>" to candidates.feedMediaImage,
            "d. Episode <itunes:image>" to candidates.recentEpisodeItunesImage,
            "e. Episode <media:thumbnail>" to candidates.recentEpisodeMediaThumbnail,
            "f. Episode <enclosure image/*>" to candidates.recentEpisodeImageEnclosure
        )

        for ((sourceName, rawUrl) in attempts) {
            val normalized = normalizeUrl(rawUrl, feedUrl)
            Log.d(TAG, "Attempting [$sourceName] Candidate: '$rawUrl' -> Normalized: '$normalized'")
            if (normalized != null) {
                if (!validateHttp || validateImageHttp(normalized, sourceName)) {
                    Log.i(TAG, "SUCCESS [$sourceName]: Resolved artwork URL '$normalized'")
                    return@withContext ResolutionResult.Success(normalized, sourceName)
                }
            }
        }

        if (!candidates.feedLinkHtmlPageUrl.isNullOrBlank()) {
            val htmlPageUrl = normalizeUrl(candidates.feedLinkHtmlPageUrl, feedUrl)
            if (htmlPageUrl != null) {
                Log.d(TAG, "Attempting [g. HTML Scrape]: Scraping '$htmlPageUrl' for og:image/twitter:image")
                val scrapedUrl = scrapeHtmlOgImage(htmlPageUrl)
                if (scrapedUrl != null) {
                    val normalizedScraped = normalizeUrl(scrapedUrl, htmlPageUrl)
                    if (normalizedScraped != null) {
                        if (!validateHttp || validateImageHttp(normalizedScraped, "g. HTML Scrape")) {
                            Log.i(TAG, "SUCCESS [g. HTML Scrape]: Resolved artwork URL '$normalizedScraped'")
                            return@withContext ResolutionResult.Success(normalizedScraped, "g. HTML Scrape")
                        }
                    }
                }
            }
        }

        Log.w(TAG, "ALL LEVELS FAILED for feed '$feedUrl': Returning NotFound")
        ResolutionResult.NotFound
    }

    private fun validateImageHttp(url: String, sourceName: String): Boolean {
        return try {
            val request = Request.Builder().url(url).head().build()
            client.newCall(request).execute().use { response ->
                val contentType = response.header("Content-Type") ?: ""
                val code = response.code
                Log.d(TAG, "HTTP Check [$sourceName] Status: $code, Content-Type: '$contentType'")
                if (response.isSuccessful && contentType.startsWith("image/", ignoreCase = true)) {
                    true
                } else {
                    if (code == 405 || code == 403 || !contentType.startsWith("image/", ignoreCase = true)) {
                        val getReq = Request.Builder().url(url).get().build()
                        client.newCall(getReq).execute().use { getResp ->
                            val getCT = getResp.header("Content-Type") ?: ""
                            val getCode = getResp.code
                            Log.d(TAG, "HTTP GET Retry [$sourceName] Status: $getCode, Content-Type: '$getCT'")
                            getResp.isSuccessful && getCT.startsWith("image/", ignoreCase = true)
                        }
                    } else false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP validation failed for [$sourceName] '$url': ${e.message}")
            false
        }
    }

    private fun scrapeHtmlOgImage(pageUrl: String): String? {
        return try {
            val request = Request.Builder().url(pageUrl).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "HTML Scrape failed for '$pageUrl', Status: ${response.code}")
                    return null
                }
                val html = response.body?.string() ?: return null

                val ogRegex = Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                val ogMatch = ogRegex.find(html)?.groupValues?.get(1)
                if (!ogMatch.isNullOrBlank()) return ogMatch

                val ogAltRegex = Regex("""<meta\s+content=["']([^"']+)["']\s+property=["']og:image["']""", RegexOption.IGNORE_CASE)
                val ogAltMatch = ogAltRegex.find(html)?.groupValues?.get(1)
                if (!ogAltMatch.isNullOrBlank()) return ogAltMatch

                val twRegex = Regex("""<meta\s+name=["']twitter:image["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                val twMatch = twRegex.find(html)?.groupValues?.get(1)
                if (!twMatch.isNullOrBlank()) return twMatch

                val twAltRegex = Regex("""<meta\s+content=["']([^"']+)["']\s+name=["']twitter:image["']""", RegexOption.IGNORE_CASE)
                twAltRegex.find(html)?.groupValues?.get(1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTML Scrape exception for '$pageUrl': ${e.message}")
            null
        }
    }
}
