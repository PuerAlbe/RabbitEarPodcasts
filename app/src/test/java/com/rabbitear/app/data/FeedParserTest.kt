package com.rabbitear.app.data

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class FeedParserTest {

    @Test
    fun testParseFeedWithPodcasting20Tags() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xmlns:podcast="https://podcastindex.org/namespace/1.0">
                <channel>
                    <title>Test Podcasting 2.0 Feed</title>
                    <itunes:author>RabbitEar Studio</itunes:author>
                    <itunes:image href="https://example.com/cover.jpg"/>
                    <podcast:guid>f81d4fae-7dec-11d0-a765-00a0c91e6bf6</podcast:guid>
                    <podcast:funding url="https://example.com/donate">Support Show</podcast:funding>
                    <item>
                        <title>Episode 1: The Beginning</title>
                        <enclosure url="https://example.com/ep1.mp3" type="audio/mpeg" length="1234567"/>
                        <guid>ep1-guid</guid>
                        <pubDate>Mon, 01 Jan 2024 12:00:00 +0000</pubDate>
                        <itunes:duration>01:30:00</itunes:duration>
                        <itunes:episode>1</itunes:episode>
                        <itunes:season>1</itunes:season>
                        <itunes:episodeType>serial</itunes:episodeType>
                        <podcast:transcript url="https://example.com/ep1.vtt" type="text/vtt"/>
                        <podcast:chapters url="https://example.com/ep1_chapters.json"/>
                        <podcast:person>Host Name</podcast:person>
                        <podcast:location>New York, NY</podcast:location>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8))
        val (podcast, episodes) = parseFeed("https://example.com/feed.xml", inputStream)

        assertEquals("Test Podcasting 2.0 Feed", podcast.title)
        assertEquals("RabbitEar Studio", podcast.author)
        assertEquals("f81d4fae-7dec-11d0-a765-00a0c91e6bf6", podcast.podcastGuid)
        assertEquals("https://example.com/donate", podcast.fundingUrl)

        assertEquals(1, episodes.size)
        val ep = episodes[0]
        assertEquals("Episode 1: The Beginning", ep.title)
        assertEquals("https://example.com/ep1.mp3", ep.audioUrl)
        assertEquals(5400000L, ep.durationMs)
        assertEquals(1, ep.episodeNumber)
        assertEquals(1, ep.seasonNumber)
        assertEquals("serial", ep.episodeType)
        assertEquals("https://example.com/ep1.vtt", ep.transcriptUrl)
        assertEquals("text/vtt", ep.transcriptType)
        assertEquals("https://example.com/ep1_chapters.json", ep.chaptersUrl)
        assertEquals("Host Name", ep.personName)
        assertEquals("New York, NY", ep.locationName)
    }

    @Test
    fun testParseMalformedXmlGracefully() {
        val xml = """
            <rss><channel><title>Malformed Feed</title>
            <item><title>Broken Item
        """.trimIndent()

        val inputStream = ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8))
        val (podcast, episodes) = parseFeed("https://example.com/broken.xml", inputStream)

        assertEquals("Malformed Feed", podcast.title)
        assertNotNull(episodes)
    }
}