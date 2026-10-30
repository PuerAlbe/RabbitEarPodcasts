package com.rabbitear.app.data

import org.junit.Assert.*
import org.junit.Test

class TranscriptParserTest {

    @Test
    fun testParseVttTranscript() {
        val vtt = """
            WEBVTT

            00:00:01.000 --> 00:00:05.500
            <v Host>Welcome to the RabbitEar podcast!

            00:00:06.000 --> 00:00:10.000
            <v Guest>Thanks for having me on the show.
        """.trimIndent()

        val lines = TranscriptParser.parseContent(vtt, "text/vtt")
        assertEquals(2, lines.size)

        assertEquals(1000L, lines[0].startMs)
        assertEquals(5500L, lines[0].endMs)
        assertEquals("Welcome to the RabbitEar podcast!", lines[0].text)
        assertEquals("Host", lines[0].speaker)

        assertEquals(6000L, lines[1].startMs)
        assertEquals(10000L, lines[1].endMs)
        assertEquals("Thanks for having me on the show.", lines[1].text)
        assertEquals("Guest", lines[1].speaker)
    }
}