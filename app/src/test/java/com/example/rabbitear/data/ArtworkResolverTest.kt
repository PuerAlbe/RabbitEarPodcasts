package com.example.rabbitear.data

import org.junit.Assert.*
import org.junit.Test

class ArtworkResolverTest {

    @Test
    fun testRelativeUrlResolution() {
        val normalized = ArtworkResolver.normalizeUrl("../images/artwork.png", "https://example.com/podcasts/feed.xml")
        assertEquals("https://example.com/images/artwork.png", normalized)
    }

    @Test
    fun testProtocolRelativeUrlResolution() {
        val normalized = ArtworkResolver.normalizeUrl("//cdn.example.com/cover.jpg", "https://example.com")
        assertEquals("https://cdn.example.com/cover.jpg", normalized)
    }

    @Test
    fun testMissingItunesImageButPresentImageTag() {
        val candidates = ArtworkCandidates(
            feedItunesImage = null,
            feedImageUrl = "https://example.com/rss_image.png"
        )
        val resolver = ArtworkResolver()
        val result = kotlinx.coroutines.runBlocking {
            resolver.resolveArtwork("https://example.com/feed.xml", candidates, validateHttp = false)
        }
        assertTrue(result is ResolutionResult.Success)
        val success = result as ResolutionResult.Success
        assertEquals("https://example.com/rss_image.png", success.url)
        assertEquals("b. Feed <image><url>", success.source)
    }

    @Test
    fun testHttpUpgradeToHttps() {
        val normalized = ArtworkResolver.normalizeUrl("http://example.com/image.jpg", null)
        assertEquals("https://example.com/image.jpg", normalized)
    }

    @Test
    fun testAllLevelsFailReturnsNotFound() {
        val candidates = ArtworkCandidates()
        val resolver = ArtworkResolver()
        val result = kotlinx.coroutines.runBlocking {
            resolver.resolveArtwork("https://example.com/feed.xml", candidates, validateHttp = false)
        }
        assertTrue(result is ResolutionResult.NotFound)
    }
}