package com.example.rabbitear.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class TranscriptLine(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val speaker: String? = null
)

object TranscriptParser {
    suspend fun fetchAndParse(
        url: String,
        type: String?,
        client: OkHttpClient = OkHttpClient()
    ): List<TranscriptLine> = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext emptyList()
        try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val content = resp.body?.string() ?: return@withContext emptyList()
                parseContent(content, type ?: "")
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseContent(content: String, type: String): List<TranscriptLine> {
        val lines = mutableListOf<TranscriptLine>()
        if (content.isBlank()) return lines

        val vttRegex = Regex("""(?:(\d{2}):)?(\d{2}):(\d{2})[\.,](\d{3})\s*-->\s*(?:(\d{2}):)?(\d{2}):(\d{2})[\.,](\d{3})""")
        val textLines = content.lines()
        var i = 0
        while (i < textLines.size) {
            val line = textLines[i].trim()
            val match = vttRegex.find(line)
            if (match != null) {
                val startMs = parseVttTime(match.groupValues[1], match.groupValues[2], match.groupValues[3], match.groupValues[4])
                val endMs = parseVttTime(match.groupValues[5], match.groupValues[6], match.groupValues[7], match.groupValues[8])
                val textBuilder = StringBuilder()
                i++
                while (i < textLines.size && textLines[i].isNotBlank()) {
                    textBuilder.append(textLines[i].trim()).append(" ")
                    i++
                }
                val rawText = textBuilder.toString().replace(Regex("<[^>]*>"), "").trim()
                if (rawText.isNotEmpty()) {
                    val speakerMatch = Regex("""^<v\s+([^>]+)>(.+)$""").find(rawText)
                        ?: Regex("""^([^:]+):\s*(.+)$""").find(rawText)
                    if (speakerMatch != null) {
                        lines.add(TranscriptLine(startMs, endMs, speakerMatch.groupValues[2].trim(), speakerMatch.groupValues[1].trim()))
                    } else {
                        lines.add(TranscriptLine(startMs, endMs, rawText, null))
                    }
                }
            }
            i++
        }
        return lines.sortedBy { it.startMs }
    }

    private fun parseVttTime(hrStr: String, minStr: String, secStr: String, msStr: String): Long {
        val hr = hrStr.toIntOrNull() ?: 0
        val min = minStr.toIntOrNull() ?: 0
        val sec = secStr.toIntOrNull() ?: 0
        val ms = msStr.toIntOrNull() ?: 0
        return (hr * 3600 + min * 60 + sec) * 1000L + ms
    }
}