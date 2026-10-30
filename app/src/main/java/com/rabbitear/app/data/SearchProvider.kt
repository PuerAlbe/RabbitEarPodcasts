package com.rabbitear.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

interface SearchProvider {
    val name: String
    suspend fun search(query: String): List<Podcast>
    suspend fun fetchTrending(): List<Podcast>
}

class ITunesSearchProvider(private val http: OkHttpClient = OkHttpClient()) : SearchProvider {
    override val name: String = "iTunes"

    override suspend fun search(query: String): List<Podcast> = withContext(Dispatchers.IO) {
        val podcasts = mutableListOf<Podcast>()
        try {
            val url = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
                .addQueryParameter("media", "podcast")
                .addQueryParameter("limit", "30")
                .addQueryParameter("term", query)
                .build()
            http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (r.isSuccessful && r.body != null) {
                    val arr = JSONObject(r.body!!.string()).optJSONArray("results") ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val f = o.optString("feedUrl")
                        if (f.isNotEmpty()) {
                            podcasts.add(Podcast(f, o.optString("collectionName"), o.optString("artistName"), o.optString("artworkUrl600")))
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        podcasts.distinctBy { it.feedUrl }
    }

    override suspend fun fetchTrending(): List<Podcast> = withContext(Dispatchers.IO) {
        search("podcast")
    }
}

class GPodderSearchProvider(private val http: OkHttpClient = OkHttpClient()) : SearchProvider {
    override val name: String = "gPodder"

    override suspend fun search(query: String): List<Podcast> = withContext(Dispatchers.IO) {
        val podcasts = mutableListOf<Podcast>()
        try {
            val url = "https://gpodder.net/search.json".toHttpUrl().newBuilder()
                .addQueryParameter("q", query)
                .build()
            http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (r.isSuccessful && r.body != null) {
                    val arr = JSONArray(r.body!!.string())
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val f = o.optString("url")
                        if (f.isNotEmpty()) {
                            podcasts.add(Podcast(f, o.optString("title"), o.optString("author"), o.optString("scaled_logo_url")))
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        podcasts.distinctBy { it.feedUrl }
    }

    override suspend fun fetchTrending(): List<Podcast> = withContext(Dispatchers.IO) {
        val podcasts = mutableListOf<Podcast>()
        try {
            val url = "https://gpodder.net/toplist/100.json"
            http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (r.isSuccessful && r.body != null) {
                    val arr = JSONArray(r.body!!.string())
                    for (i in 0 until arr.length().coerceAtMost(30)) {
                        val o = arr.optJSONObject(i) ?: continue
                        val f = o.optString("url")
                        if (f.isNotEmpty()) {
                            podcasts.add(Podcast(f, o.optString("title"), o.optString("author"), o.optString("scaled_logo_url")))
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        podcasts
    }
}
