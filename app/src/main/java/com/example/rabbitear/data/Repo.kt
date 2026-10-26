package com.example.rabbitear.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.Room
import androidx.work.*
import com.example.rabbitear.DownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class Mode { OLDEST, NEWEST }

class Repo(private val ctx: Context) {
    val dao = Room.databaseBuilder(ctx, Db::class.java, "rabbitear.db")
        .fallbackToDestructiveMigration()
        .build().dao()
    val http = OkHttpClient()
    private val prefs = ctx.getSharedPreferences("rabbitear", Context.MODE_PRIVATE)

    // ---------- discovery: iTunes directory (what AntennaPod uses by default) or a pasted RSS URL ----------
    suspend fun search(q: String): List<Podcast> = withContext(Dispatchers.IO) {
        val query = q.trim()
        if (query.isEmpty()) return@withContext emptyList()
        try {
            if (query.startsWith("http", ignoreCase = true)) {
                val direct = try {
                    http.newCall(Request.Builder().url(query).build()).execute().use { r ->
                        val body = r.body
                        if (r.isSuccessful && body != null) {
                            val p = parseFeed(query, body.byteStream()).first
                            if (p.title.isNotEmpty()) listOf(p) else emptyList()
                        } else emptyList()
                    }
                } catch (_: Exception) { emptyList() }
                if (direct.isNotEmpty()) return@withContext direct
            }
            val url = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
                .addQueryParameter("media", "podcast").addQueryParameter("limit", "30").addQueryParameter("term", query).build()
            http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                val body = r.body
                if (!r.isSuccessful || body == null) return@withContext emptyList()
                val arr = JSONObject(body.string()).optJSONArray("results") ?: return@withContext emptyList()
                (0 until arr.length()).mapNotNull {
                    val o = arr.optJSONObject(it) ?: return@mapNotNull null
                    val f = o.optString("feedUrl")
                    if (f.isEmpty()) null else Podcast(f, o.optString("collectionName"), o.optString("artistName"), o.optString("artworkUrl600"))
                }.distinctBy { it.feedUrl }
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun refresh(feedUrl: String) = withContext(Dispatchers.IO) {
        try {
            http.newCall(Request.Builder().url(feedUrl).build()).execute().use {
                dao.insertEpisodes(parseFeed(feedUrl, it.body!!.byteStream()).second)
            }
        } catch (_: Exception) {}
    }
    suspend fun open(p: Podcast) { dao.addPodcast(p); refresh(p.feedUrl) }
    suspend fun refreshAll() { dao.subscriptionsNow().forEach { refresh(it.feedUrl) } }

    // ---------- the syndication schedule ----------
    fun mode(): Mode? = prefs.getString("mode", null)?.let { Mode.valueOf(it) }
    fun setMode(m: Mode?) = prefs.edit { if (m == null) remove("mode") else putString("mode", m.name) }
    fun currentGuid(): String? {
        val m = mode()
        return if (m != null) {
            prefs.getString("current_${m.name}", null) ?: prefs.getString("current", null)
        } else {
            prefs.getString("current", null)
        }
    }

    suspend fun nextScheduled(m: Mode): Episode? {
        val subs = dao.subscriptionsNow()
        if (subs.isEmpty()) return null
        val lastShowKey = "lastShow_${m.name}"
        val currentKey = "current_${m.name}"
        val lastShowVal = prefs.getString(lastShowKey, null)
        val startIdx = if (lastShowVal != null) {
            val idx = subs.indexOfFirst { it.feedUrl == lastShowVal }
            if (idx >= 0) idx + 1 else 0
        } else 0

        for (i in subs.indices) {
            val s = subs[(startIdx + i) % subs.size]
            val ep = if (m == Mode.OLDEST) dao.oldestUnplayed(s.feedUrl) else dao.newestUnplayed(s.feedUrl)
            if (ep != null) {
                prefs.edit {
                    putString(lastShowKey, s.feedUrl)
                    putString(currentKey, ep.guid)
                    putString("current", ep.guid)
                }
                return ep
            }
        }
        return null
    }

    /** Resumes a half-finished scheduled episode if there is one for this specific mode, otherwise takes the next in rotation.
     * Also triggers downloads for the next unplayed episode of every subscription. */
    suspend fun startSchedule(m: Mode): Episode? {
        setMode(m)
        val currentKey = "current_${m.name}"
        val curGuid = prefs.getString(currentKey, null)
        val cur = curGuid?.let { dao.episode(it) }
        val epToPlay = if (cur != null && !cur.played) cur else nextScheduled(m)
        triggerSyndicatedDownloads(m)
        return epToPlay
    }

    suspend fun triggerSyndicatedDownloads(m: Mode) = withContext(Dispatchers.IO) {
        val subs = dao.subscriptionsNow()
        for (s in subs) {
            val ep = if (m == Mode.OLDEST) dao.oldestUnplayed(s.feedUrl) else dao.newestUnplayed(s.feedUrl)
            if (ep != null && ep.localPath == null) {
                enqueueDownload(ep.guid, bySyndication = true)
            }
        }
    }

    fun enqueueDownload(guid: String, bySyndication: Boolean) {
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "dl-$guid", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workDataOf("guid" to guid, "bySyndication" to bySyndication))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        )
    }

    // ---------- playback helpers ----------
    suspend fun item(e: Episode): MediaItem {
        val show = dao.podcast(e.podcastUrl)
        val uri = e.localPath?.takeIf { File(it).exists() }?.let { Uri.fromFile(File(it)) } ?: e.audioUrl.toUri()
        val artworkUri = e.image?.takeIf { it.isNotEmpty() } ?: show?.image?.takeIf { it.isNotEmpty() }
        return MediaItem.Builder().setMediaId(e.guid).setUri(uri).setMediaMetadata(
            MediaMetadata.Builder().setTitle(e.title).setArtist(show?.title ?: "")
                .setArtworkUri(artworkUri?.toUri()).build()
        ).build()
    }
    suspend fun saveProgress(guid: String, pos: Long, dur: Long) = dao.progress(guid, pos, dur)

    suspend fun finish(guid: String) {
        val ep = dao.episode(guid)
        dao.setPlayed(guid, true)
        if (ep != null && ep.downloadedBySyndication && ep.localPath != null) {
            deleteDownload(ep)
        }
    }

    suspend fun deleteDownload(e: Episode) {
        e.localPath?.let { File(it).delete() }
        dao.setLocal(e.guid, null, false)
    }

    // ---------- export & import settings ----------
    suspend fun exportSettings(out: java.io.OutputStream) = withContext(Dispatchers.IO) {
        val subs = dao.subscriptionsNow()
        val played = dao.playedEpisodesNow()

        val json = JSONObject()
        json.put("version", 1)

        val subsArr = JSONArray()
        subs.forEach { p ->
            val obj = JSONObject().apply {
                put("feedUrl", p.feedUrl)
                put("title", p.title)
                put("author", p.author)
                put("image", p.image)
            }
            subsArr.put(obj)
        }
        json.put("subscriptions", subsArr)

        val playedArr = JSONArray()
        played.forEach { e ->
            val obj = JSONObject().apply {
                put("guid", e.guid)
                put("podcastUrl", e.podcastUrl)
                put("played", e.played)
                put("positionMs", e.positionMs)
            }
            playedArr.put(obj)
        }
        json.put("playedEpisodes", playedArr)

        out.use { it.write(json.toString(2).toByteArray(Charsets.UTF_8)) }
    }

    suspend fun importSettings(input: java.io.InputStream): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val content = input.use { it.bufferedReader(Charsets.UTF_8).readText() }
        var importedSubs = 0
        var importedPlayed = 0

        val urls = mutableSetOf<String>()
        val playedGuids = mutableMapOf<String, Boolean>()

        try {
            if (content.trim().startsWith("{")) {
                val json = JSONObject(content)

                val subsArr = json.optJSONArray("subscriptions")
                if (subsArr != null) {
                    for (i in 0 until subsArr.length()) {
                        val obj = subsArr.optJSONObject(i) ?: continue
                        val url = obj.optString("feedUrl")
                        if (url.isNotEmpty()) urls.add(url)
                    }
                }

                val playedArr = json.optJSONArray("playedEpisodes")
                if (playedArr != null) {
                    for (i in 0 until playedArr.length()) {
                        val obj = playedArr.optJSONObject(i) ?: continue
                        val guid = obj.optString("guid")
                        val pUrl = obj.optString("podcastUrl")
                        val isPlayed = obj.optBoolean("played", true)
                        if (guid.isNotEmpty()) {
                            playedGuids[guid] = isPlayed
                            if (pUrl.isNotEmpty()) urls.add(pUrl)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        if (urls.isEmpty()) {
            val xmlUrlRegex = Regex("""(?:xmlUrl|feedUrl|url)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            xmlUrlRegex.findAll(content).forEach { urls.add(it.groupValues[1]) }

            if (urls.isEmpty()) {
                val httpRegex = Regex("""https?://[^\s"'<>\[\]]+""")
                httpRegex.findAll(content).forEach { urls.add(it.value) }
            }
        }

        urls.forEach { feedUrl ->
            try {
                http.newCall(Request.Builder().url(feedUrl).build()).execute().use { r ->
                    val body = r.body
                    if (r.isSuccessful && body != null) {
                        val (p, eps) = parseFeed(feedUrl, body.byteStream())
                        dao.addPodcast(p.copy(subscribed = true))
                        dao.insertEpisodes(eps)
                        dao.setSubscribed(feedUrl, true)
                        importedSubs++
                    }
                }
            } catch (_: Exception) {}
        }

        playedGuids.forEach { (guid, played) ->
            if (played) {
                dao.setPlayed(guid, true)
                importedPlayed++
            }
        }

        importedSubs to importedPlayed
    }
}
