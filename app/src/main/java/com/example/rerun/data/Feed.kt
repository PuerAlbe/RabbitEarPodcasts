package com.example.rerun.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

private val dateFormats = listOf("EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss zzz", "dd MMM yyyy HH:mm:ss Z")

private fun parseDate(s: String): Long {
    for (f in dateFormats) try { return SimpleDateFormat(f, Locale.US).parse(s.trim())!!.time } catch (_: Exception) {}
    return 0L
}

private fun parseDur(s: String): Long {
    val parts = s.trim().split(":").map { it.toDoubleOrNull() ?: return 0L }
    return (parts.fold(0.0) { a, b -> a * 60 + b } * 1000).toLong()
}

/** Minimal RSS + iTunes-namespace parser. Episode ids are namespaced by feed URL. */
fun parseFeed(feedUrl: String, input: InputStream): Pair<Podcast, List<Episode>> {
    val p = Xml.newPullParser()
    p.setInput(input, null)
    var title = ""; var author = ""; var image = ""
    val eps = mutableListOf<Episode>()
    var inItem = false
    var t = ""; var u = ""; var g = ""; var d = 0L; var dur = 0L; var epImg: String? = null
    var ev = p.eventType
    while (ev != XmlPullParser.END_DOCUMENT) {
        if (ev == XmlPullParser.START_TAG) {
            when (p.name) {
                "item" -> { inItem = true; t = ""; u = ""; g = ""; d = 0L; dur = 0L; epImg = null }
                "title" -> { val x = p.nextText().trim(); if (inItem) t = x else if (title.isEmpty()) title = x }
                "enclosure" -> if (inItem) u = p.getAttributeValue(null, "url") ?: ""
                "guid" -> if (inItem) g = p.nextText().trim()
                "pubDate" -> if (inItem) d = parseDate(p.nextText())
                "itunes:duration" -> if (inItem) dur = parseDur(p.nextText())
                "itunes:author" -> if (!inItem && author.isEmpty()) author = p.nextText().trim()
                "itunes:image" -> {
                    val href = p.getAttributeValue(null, "href") ?: ""
                    if (inItem) epImg = href else if (image.isEmpty()) image = href
                }
                "url" -> if (!inItem && image.isEmpty()) image = p.nextText().trim()
            }
        } else if (ev == XmlPullParser.END_TAG && p.name == "item") {
            inItem = false
            if (u.isNotEmpty()) eps += Episode("$feedUrl#${g.ifEmpty { u }}", feedUrl, t.ifEmpty { "Untitled" }, u, d, dur, image = epImg)
        }
        ev = p.next()
    }
    return Podcast(feedUrl, title.ifEmpty { feedUrl }, author, image) to eps.distinctBy { it.guid }
}
