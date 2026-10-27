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
import com.example.rabbitear.RefreshWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import android.os.Environment
import java.io.File
import java.util.concurrent.TimeUnit

enum class Mode { OLDEST, NEWEST }

data class StateToRestore(
    val guid: String,
    val podcastUrl: String,
    val audioUrl: String,
    val played: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val favorite: Boolean,
    val inSlate: Boolean = false,
    val slateAddedTime: Long = 0L
)

class Repo(private val ctx: Context) {
    val dao = Room.databaseBuilder(ctx, Db::class.java, "rabbitear.db")
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
     * Also triggers downloads for the next 6 unplayed episodes in rotation. */
    suspend fun startSchedule(m: Mode): Episode? {
        setMode(m)
        val currentKey = "current_${m.name}"
        val curGuid = prefs.getString(currentKey, null)
        val cur = curGuid?.let { dao.episode(it) }
        val epToPlay = if (cur != null && !cur.played) cur else nextScheduled(m)
        triggerSyndicatedDownloads(m)
        return epToPlay
    }

    suspend fun peekNextScheduledRotation(m: Mode, count: Int = 6): List<Episode> {
        val subs = dao.subscriptionsNow()
        if (subs.isEmpty() || count <= 0) return emptyList()

        val lastShowKey = "lastShow_${m.name}"
        val lastShowVal = prefs.getString(lastShowKey, null)
        val startIdx = if (lastShowVal != null) {
            val idx = subs.indexOfFirst { it.feedUrl == lastShowVal }
            if (idx >= 0) idx else 0
        } else 0

        val result = mutableListOf<Episode>()
        val offsetMap = mutableMapOf<String, Int>()

        var attempts = 0
        val maxAttempts = subs.size * 50

        var currentSubIdx = startIdx
        while (result.size < count && attempts < maxAttempts) {
            attempts++
            val s = subs[currentSubIdx % subs.size]
            val offset = offsetMap.getOrDefault(s.feedUrl, 0)

            val ep = if (m == Mode.OLDEST) {
                dao.oldestUnplayedWithOffset(s.feedUrl, offset)
            } else {
                dao.newestUnplayedWithOffset(s.feedUrl, offset)
            }

            if (ep != null) {
                result.add(ep)
                offsetMap[s.feedUrl] = offset + 1
            }

            currentSubIdx++
        }

        return result
    }

    suspend fun triggerSyndicatedDownloads(m: Mode) = withContext(Dispatchers.IO) {
        val next6 = peekNextScheduledRotation(m, 6)
        for (ep in next6) {
            if (ep.localPath == null) {
                enqueueDownload(ep.guid, bySyndication = true)
            }
        }
    }

    suspend fun getLatestEpisodesForSubscriptions(): List<Pair<Podcast, Episode>> = withContext(Dispatchers.IO) {
        val subs = dao.subscriptionsNow()
        subs.mapNotNull { p ->
            val ep = dao.newestEpisode(p.feedUrl)
            if (ep != null) p to ep else null
        }.sortedByDescending { it.second.pubDate }
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

    // ---------- check for new episodes setting ----------
    fun refreshIntervalHours(): Int = prefs.getInt("refreshIntervalHours", 24)

    fun setRefreshIntervalHours(h: Int) {
        val bounded = h.coerceIn(1, 720)
        prefs.edit { putInt("refreshIntervalHours", bounded) }
    }

    fun sleepTimerMinutes(): Int = prefs.getInt("sleepTimerMinutes", 20)

    fun setSleepTimerMinutes(minutes: Int) {
        val bounded = minutes.coerceIn(0, 60)
        prefs.edit { putInt("sleepTimerMinutes", bounded) }
    }

    fun updateRefreshSchedule(ctx: Context, hours: Int) {
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            "refresh",
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            PeriodicWorkRequestBuilder<RefreshWorker>(hours.toLong(), TimeUnit.HOURS)
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
        dao.setInSlate(guid, false)
        if (ep != null && ep.downloadedBySyndication && ep.localPath != null) {
            deleteDownload(ep)
        } else {
            mode()?.let { triggerSyndicatedDownloads(it) }
        }
    }

    suspend fun deleteDownload(e: Episode) {
        e.localPath?.let { File(it).delete() }
        dao.setLocal(e.guid, null, false)
        mode()?.let { triggerSyndicatedDownloads(it) }
    }

    fun getSettingsFile(c: Context = ctx): File {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!docsDir.exists()) {
            try { docsDir.mkdirs() } catch (_: Exception) {}
        }
        val targetDir = if (docsDir.exists() && docsDir.canWrite()) docsDir else c.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: c.filesDir
        return File(targetDir, "rabbitear_settings.json")
    }

    suspend fun autoRestoreIfNeeded(c: Context = ctx) = withContext(Dispatchers.IO) {
        try {
            val subs = dao.subscriptionsNow()
            if (subs.isEmpty()) {
                val file = getSettingsFile(c)
                if (file.exists() && file.length() > 0) {
                    file.inputStream().use { importSettings(it) }
                }
            }
        } catch (_: Exception) {}
    }

    // ---------- export & import settings ----------
    suspend fun exportSettings(out: java.io.OutputStream) = withContext(Dispatchers.IO) {
        val subs = dao.subscriptionsNow()
        val episodeStates = dao.allEpisodeStatesNow()

        val json = JSONObject()
        json.put("version", 3)

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

        val statesArr = JSONArray()
        episodeStates.forEach { e ->
            val obj = JSONObject().apply {
                put("guid", e.guid)
                put("podcastUrl", e.podcastUrl)
                put("audioUrl", e.audioUrl)
                put("title", e.title)
                put("played", e.played)
                put("positionMs", e.positionMs)
                put("durationMs", e.durationMs)
                put("favorite", e.favorite)
                put("inSlate", e.inSlate)
                put("slateAddedTime", e.slateAddedTime)
            }
            statesArr.put(obj)
        }
        json.put("episodeStates", statesArr)

        // Legacy compatibility keys
        val playedArr = JSONArray()
        episodeStates.filter { it.played || it.positionMs > 0 }.forEach { e ->
            val obj = JSONObject().apply {
                put("guid", e.guid)
                put("podcastUrl", e.podcastUrl)
                put("played", e.played)
                put("positionMs", e.positionMs)
            }
            playedArr.put(obj)
        }
        json.put("playedEpisodes", playedArr)

        val favArr = JSONArray()
        episodeStates.filter { it.favorite }.forEach { e ->
            val obj = JSONObject().apply {
                put("guid", e.guid)
                put("podcastUrl", e.podcastUrl)
                put("favorite", e.favorite)
            }
            favArr.put(obj)
        }
        json.put("favoriteEpisodes", favArr)

        val slateArr = JSONArray()
        episodeStates.filter { it.inSlate }.forEach { e ->
            val obj = JSONObject().apply {
                put("guid", e.guid)
                put("podcastUrl", e.podcastUrl)
                put("inSlate", e.inSlate)
                put("slateAddedTime", e.slateAddedTime)
            }
            slateArr.put(obj)
        }
        json.put("slateEpisodes", slateArr)

        out.use { it.write(json.toString(2).toByteArray(Charsets.UTF_8)) }
    }

    suspend fun importSettings(input: java.io.InputStream): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val content = input.use { it.bufferedReader(Charsets.UTF_8).readText() }
        var importedSubs = 0
        var importedStates = 0

        val urls = mutableSetOf<String>()
        val statesToRestore = mutableListOf<StateToRestore>()

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

                val statesArr = json.optJSONArray("episodeStates")
                if (statesArr != null) {
                    for (i in 0 until statesArr.length()) {
                        val obj = statesArr.optJSONObject(i) ?: continue
                        val guid = obj.optString("guid")
                        val pUrl = obj.optString("podcastUrl")
                        val audioUrl = obj.optString("audioUrl")
                        val played = obj.optBoolean("played", false)
                        val pos = obj.optLong("positionMs", 0L)
                        val dur = obj.optLong("durationMs", 0L)
                        val fav = obj.optBoolean("favorite", false)
                        val inSlate = obj.optBoolean("inSlate", false)
                        val slateTime = obj.optLong("slateAddedTime", 0L)
                        if (guid.isNotEmpty() || audioUrl.isNotEmpty()) {
                            statesToRestore.add(StateToRestore(guid, pUrl, audioUrl, played, pos, dur, fav, inSlate, slateTime))
                            if (pUrl.isNotEmpty()) urls.add(pUrl)
                        }
                    }
                } else {
                    // Legacy format fallbacks
                    val playedMap = mutableMapOf<String, Pair<Boolean, Long>>()
                    val playedArr = json.optJSONArray("playedEpisodes")
                    if (playedArr != null) {
                        for (i in 0 until playedArr.length()) {
                            val obj = playedArr.optJSONObject(i) ?: continue
                            val guid = obj.optString("guid")
                            val pUrl = obj.optString("podcastUrl")
                            val isPlayed = obj.optBoolean("played", true)
                            val pos = obj.optLong("positionMs", 0L)
                            if (guid.isNotEmpty()) {
                                playedMap[guid] = isPlayed to pos
                                if (pUrl.isNotEmpty()) urls.add(pUrl)
                            }
                        }
                    }

                    val favMap = mutableMapOf<String, Boolean>()
                    val favArr = json.optJSONArray("favoriteEpisodes")
                    if (favArr != null) {
                        for (i in 0 until favArr.length()) {
                            val obj = favArr.optJSONObject(i) ?: continue
                            val guid = obj.optString("guid")
                            val pUrl = obj.optString("podcastUrl")
                            val isFav = obj.optBoolean("favorite", true)
                            if (guid.isNotEmpty()) {
                                favMap[guid] = isFav
                                if (pUrl.isNotEmpty()) urls.add(pUrl)
                            }
                        }
                    }

                    val slateMap = mutableMapOf<String, Pair<Boolean, Long>>()
                    val slateArr = json.optJSONArray("slateEpisodes")
                    if (slateArr != null) {
                        for (i in 0 until slateArr.length()) {
                            val obj = slateArr.optJSONObject(i) ?: continue
                            val guid = obj.optString("guid")
                            val pUrl = obj.optString("podcastUrl")
                            val inSlate = obj.optBoolean("inSlate", true)
                            val slateTime = obj.optLong("slateAddedTime", System.currentTimeMillis())
                            if (guid.isNotEmpty()) {
                                slateMap[guid] = inSlate to slateTime
                                if (pUrl.isNotEmpty()) urls.add(pUrl)
                            }
                        }
                    }

                    val allGuids = playedMap.keys + favMap.keys + slateMap.keys
                    for (guid in allGuids) {
                        val (played, pos) = playedMap[guid] ?: (false to 0L)
                        val fav = favMap[guid] ?: false
                        val (inSlate, slateTime) = slateMap[guid] ?: (false to 0L)
                        statesToRestore.add(StateToRestore(guid, "", "", played, pos, 0L, fav, inSlate, slateTime))
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

        statesToRestore.forEach { state ->
            var updated = 0
            if (state.guid.isNotEmpty()) {
                updated = dao.restoreStateByGuid(state.guid, state.played, state.positionMs, state.durationMs, state.favorite, state.inSlate, state.slateAddedTime)
            }
            if (updated == 0 && state.audioUrl.isNotEmpty()) {
                dao.restoreStateByAudioUrl(state.audioUrl, state.played, state.positionMs, state.durationMs, state.favorite, state.inSlate, state.slateAddedTime)
            }
            importedStates++
        }

        importedSubs to importedStates
    }
}
