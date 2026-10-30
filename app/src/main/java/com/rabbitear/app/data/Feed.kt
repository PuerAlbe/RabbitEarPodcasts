package com.rabbitear.app.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

private val dateFormats = listOf(
    "EEE, dd MMM yyyy HH:mm:ss Z",
    "EEE, dd MMM yyyy HH:mm:ss zzz",
    "dd MMM yyyy HH:mm:ss Z",
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    "yyyy-MM-dd'T'HH:mm:ssXXX"
)

private fun parseDate(s: String): Long {
    for (f in dateFormats) try { return SimpleDateFormat(f, Locale.US).parse(s.trim())!!.time } catch (_: Exception) {}
    return 0L
}

private fun parseDur(s: String): Long {
    val parts = s.trim().split(":").map { it.toDoubleOrNull() ?: return 0L }
    return (parts.fold(0.0) { a, b -> a * 60 + b } * 1000).toLong()
}

private fun safeNextText(p: XmlPullParser): String {
    return try { p.nextText().trim() } catch (_: Exception) { "" }
}

/** RSS + iTunes + Podcasting 2.0 namespace parser. */
fun parseFeed(feedUrl: String, input: InputStream): Pair<Podcast, List<Episode>> {
    val factory = try {
        org.xmlpull.v1.XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
    } catch (_: Throwable) { null }
    val p = (try { Xml.newPullParser() } catch (_: Throwable) { null })
        ?: factory?.newPullParser()
        ?: org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser()

    p.setInput(input, null)
    var title = ""; var author = ""
    var podcastGuid: String? = null
    var fundingUrl: String? = null
    var fundingText: String? = null

    var feedItunesImage: String? = null
    var feedImageUrl: String? = null
    var feedMediaImage: String? = null
    var feedLink: String? = null

    var firstEpItunesImage: String? = null
    var firstEpMediaThumb: String? = null
    var firstEpEnclosure: String? = null

    val eps = mutableListOf<Episode>()
    var inItem = false
    var t = ""; var u = ""; var g = ""; var d = 0L; var dur = 0L; var epImg: String? = null; var desc = ""
    var transcriptUrl: String? = null
    var transcriptType: String? = null
    var chaptersUrl: String? = null
    var personName: String? = null
    var locationName: String? = null
    var episodeNumber: Int? = null
    var seasonNumber: Int? = null
    var episodeType: String? = null
    var isExplicit = false

    var ev = try { p.eventType } catch (_: Exception) { XmlPullParser.END_DOCUMENT }

    while (ev != XmlPullParser.END_DOCUMENT) {
        if (ev == XmlPullParser.START_TAG) {
            val name = p.name
            when (name) {
                "item" -> {
                    inItem = true
                    t = ""; u = ""; g = ""; d = 0L; dur = 0L; epImg = null; desc = ""
                    transcriptUrl = null; transcriptType = null; chaptersUrl = null; personName = null; locationName = null
                    episodeNumber = null; seasonNumber = null; episodeType = null; isExplicit = false
                }
                "title" -> { val x = safeNextText(p); if (inItem) t = x else if (title.isEmpty()) title = x }
                "link" -> if (!inItem && feedLink.isNullOrEmpty()) feedLink = safeNextText(p)
                "enclosure" -> {
                    if (inItem) {
                        val encUrl = p.getAttributeValue(null, "url") ?: ""
                        val encType = p.getAttributeValue(null, "type") ?: ""
                        if (encType.startsWith("image/", ignoreCase = true) && firstEpEnclosure == null) {
                            firstEpEnclosure = encUrl
                        }
                        if (u.isEmpty() && !encType.startsWith("image/", ignoreCase = true)) {
                            u = encUrl
                        }
                    }
                }
                "guid" -> if (inItem) g = safeNextText(p)
                "pubDate" -> if (inItem) d = parseDate(safeNextText(p))
                "description", "itunes:summary", "content:encoded" -> {
                    if (inItem && desc.isEmpty()) desc = safeNextText(p)
                }
                "itunes:duration", "duration" -> if (inItem) dur = parseDur(safeNextText(p))
                "itunes:episode", "episode" -> if (inItem) episodeNumber = safeNextText(p).toIntOrNull()
                "itunes:season", "season" -> if (inItem) seasonNumber = safeNextText(p).toIntOrNull()
                "itunes:episodeType", "episodeType" -> if (inItem) episodeType = safeNextText(p)
                "itunes:explicit", "explicit" -> if (inItem) isExplicit = safeNextText(p).lowercase() in listOf("yes", "true", "explicit")
                "itunes:author", "author" -> if (!inItem && author.isEmpty()) author = safeNextText(p)
                "itunes:image", "image" -> {
                    val href = p.getAttributeValue(null, "href") ?: ""
                    if (inItem) {
                        epImg = href
                        if (firstEpItunesImage == null) firstEpItunesImage = href
                    } else if (feedItunesImage == null) feedItunesImage = href
                }
                "media:thumbnail", "media:content" -> {
                    val mediaUrl = p.getAttributeValue(null, "url") ?: ""
                    val medium = p.getAttributeValue(null, "medium") ?: ""
                    if (mediaUrl.isNotEmpty()) {
                        if (inItem) {
                            if (firstEpMediaThumb == null) firstEpMediaThumb = mediaUrl
                        } else if (medium == "image" || p.name == "media:thumbnail") {
                            if (feedMediaImage == null) feedMediaImage = mediaUrl
                        }
                    }
                }
                "url" -> if (!inItem && feedImageUrl == null) feedImageUrl = safeNextText(p)

                // Podcasting 2.0 namespace tags
                "podcast:guid" -> if (!inItem) podcastGuid = safeNextText(p)
                "podcast:funding" -> {
                    if (!inItem) {
                        fundingUrl = p.getAttributeValue(null, "url")
                        fundingText = safeNextText(p)
                    }
                }
                "podcast:transcript" -> {
                    if (inItem && transcriptUrl == null) {
                        transcriptUrl = p.getAttributeValue(null, "url")
                        transcriptType = p.getAttributeValue(null, "type")
                    }
                }
                "podcast:chapters" -> {
                    if (inItem && chaptersUrl == null) {
                        chaptersUrl = p.getAttributeValue(null, "url")
                    }
                }
                "podcast:person" -> {
                    if (inItem && personName == null) {
                        personName = safeNextText(p)
                    }
                }
                "podcast:location" -> {
                    if (inItem && locationName == null) {
                        locationName = safeNextText(p)
                    }
                }
            }
        } else if (ev == XmlPullParser.END_TAG && p.name == "item") {
            inItem = false
            if (u.isNotEmpty()) {
                eps += Episode(
                    guid = "$feedUrl#${g.ifEmpty { u }}",
                    podcastUrl = feedUrl,
                    title = t.ifEmpty { "Untitled" },
                    audioUrl = u,
                    pubDate = d,
                    durationMs = dur,
                    image = epImg,
                    description = desc.ifEmpty { null },
                    transcriptUrl = transcriptUrl,
                    transcriptType = transcriptType,
                    chaptersUrl = chaptersUrl,
                    personName = personName,
                    locationName = locationName,
                    episodeNumber = episodeNumber,
                    seasonNumber = seasonNumber,
                    episodeType = episodeType,
                    isExplicit = isExplicit
                )
            }
        }
        ev = try { p.next() } catch (_: Exception) { XmlPullParser.END_DOCUMENT }
    }

    val candidates = ArtworkCandidates(
        feedItunesImage = feedItunesImage,
        feedImageUrl = feedImageUrl,
        feedMediaImage = feedMediaImage,
        recentEpisodeItunesImage = firstEpItunesImage,
        recentEpisodeMediaThumbnail = firstEpMediaThumb,
        recentEpisodeImageEnclosure = firstEpEnclosure,
        feedLinkHtmlPageUrl = feedLink
    )

    val resolvedUrl = ArtworkResolver.normalizeUrl(
        candidates.feedItunesImage
            ?: candidates.feedImageUrl
            ?: candidates.feedMediaImage
            ?: candidates.recentEpisodeItunesImage
            ?: candidates.recentEpisodeMediaThumbnail
            ?: candidates.recentEpisodeImageEnclosure,
        feedUrl
    ) ?: ""

    return Podcast(
        feedUrl = feedUrl,
        title = title.ifEmpty { feedUrl },
        author = author,
        image = resolvedUrl,
        podcastGuid = podcastGuid,
        fundingUrl = fundingUrl,
        fundingText = fundingText
    ) to eps.distinctBy { it.guid }
}