package com.rabbitear.app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.Room
import androidx.work.*
import com.rabbitear.app.DownloadWorker
import com.rabbitear.app.RefreshWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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

data class SearchResults(
    val podcasts: List<Podcast>,
    val episodes: List<Pair<Podcast, Episode>>
)

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
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
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

    suspend fun deepSearch(q: String): SearchResults = withContext(Dispatchers.IO) {
        val query = q.trim()
        if (query.isEmpty()) return@withContext SearchResults(emptyList(), emptyList())

        val lowerQuery = query.lowercase(java.util.Locale.US)

        // 1. Fetch iTunes online results & RSS direct feed if URL
        val onlinePods = mutableListOf<Podcast>()
        try {
            if (query.startsWith("http", ignoreCase = true)) {
                try {
                    http.newCall(Request.Builder().url(query).build()).execute().use { r ->
                        val body = r.body
                        if (r.isSuccessful && body != null) {
                            val p = parseFeed(query, body.byteStream()).first
                            if (p.title.isNotEmpty()) onlinePods.add(p)
                        }
                    }
                } catch (_: Exception) {}
            }

            val url = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
                .addQueryParameter("media", "podcast").addQueryParameter("limit", "30").addQueryParameter("term", query).build()
            http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                val body = r.body
                if (r.isSuccessful && body != null) {
                    val arr = JSONObject(body.string()).optJSONArray("results") ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val f = o.optString("feedUrl")
                        if (f.isNotEmpty()) {
                            onlinePods.add(Podcast(f, o.optString("collectionName"), o.optString("artistName"), o.optString("artworkUrl600")))
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Subscribed & Cached Podcasts
        val subs = dao.subscriptionsNow()
        val allPods = (subs + onlinePods).distinctBy { it.feedUrl }

        // Rank Podcasts: Title > Author
        val rankedPodcasts = allPods.map { p ->
            var score = 0
            val titleLower = p.title.lowercase(java.util.Locale.US)
            val authorLower = p.author.lowercase(java.util.Locale.US)

            if (titleLower == lowerQuery) score += 200
            else if (titleLower.startsWith(lowerQuery)) score += 150
            else if (titleLower.contains(lowerQuery)) score += 100

            if (authorLower.contains(lowerQuery)) score += 80

            p to score
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }

        // 3. Search Episodes across local database
        val matchingEps = dao.searchEpisodes(query)
        val episodePairs = matchingEps.mapNotNull { ep ->
            val pod = dao.podcast(ep.podcastUrl)
                ?: Podcast(ep.podcastUrl, ep.podcastUrl.substringAfter("://").substringBefore("/"), "", ep.image ?: "")
            pod to ep
        }

        SearchResults(rankedPodcasts, episodePairs)
    }

    suspend fun refresh(feedUrl: String) = withContext(Dispatchers.IO) {
        try {
            val pod = dao.podcast(feedUrl)
            val reqBuilder = Request.Builder().url(feedUrl)
            if (pod?.etag != null) {
                reqBuilder.header("If-None-Match", pod.etag)
            }
            if (pod?.lastModified != null) {
                reqBuilder.header("If-Modified-Since", pod.lastModified)
            }

            http.newCall(reqBuilder.build()).execute().use { resp ->
                val code = resp.code
                if (code == 304) {
                    dao.updateFeedSyncState(feedUrl, pod?.etag, pod?.lastModified, System.currentTimeMillis(), null)
                    return@withContext
                }

                if (!resp.isSuccessful) {
                    dao.updateFeedSyncState(feedUrl, pod?.etag, pod?.lastModified, pod?.lastSuccessTime ?: 0L, "HTTP $code ${resp.message}")
                    return@withContext
                }

                val etag = resp.header("ETag")
                val lastMod = resp.header("Last-Modified")
                val bodyStream = resp.body?.byteStream() ?: return@withContext

                val (parsedPod, eps) = parseFeed(feedUrl, bodyStream)
                val updatedPod = (pod ?: parsedPod).copy(
                    title = parsedPod.title,
                    author = parsedPod.author,
                    image = if (parsedPod.image.isNotEmpty()) parsedPod.image else (pod?.image ?: ""),
                    etag = etag ?: pod?.etag,
                    lastModified = lastMod ?: pod?.lastModified,
                    lastSuccessTime = System.currentTimeMillis(),
                    lastErrorMsg = null
                )
                dao.addPodcast(updatedPod)

                dao.insertEpisodes(eps)
                eps.forEach { ep ->
                    dao.updateEpisodeMetadata(
                        guid = ep.guid,
                        title = ep.title,
                        audioUrl = ep.audioUrl,
                        dur = ep.durationMs,
                        image = ep.image,
                        description = ep.description,
                        transcriptUrl = ep.transcriptUrl,
                        transcriptType = ep.transcriptType,
                        chaptersUrl = ep.chaptersUrl,
                        personName = ep.personName,
                        locationName = ep.locationName,
                        episodeNumber = ep.episodeNumber,
                        seasonNumber = ep.seasonNumber,
                        episodeType = ep.episodeType,
                        isExplicit = ep.isExplicit
                    )
                }
            }
        } catch (e: Exception) {
            val pod = dao.podcast(feedUrl)
            dao.updateFeedSyncState(feedUrl, pod?.etag, pod?.lastModified, pod?.lastSuccessTime ?: 0L, e.message ?: "Network Exception")
        }
    }

    suspend fun open(p: Podcast) { dao.addPodcast(p); refresh(p.feedUrl) }

    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        val subs = dao.subscriptionsNow()
        val semaphore = kotlinx.coroutines.sync.Semaphore(4)
        subs.map { s ->
            async {
                semaphore.withPermit {
                    refresh(s.feedUrl)
                }
            }
        }.awaitAll()
    }

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

    // ---------- Download & Storage Settings ----------
    fun downloadRequireWifi(): Boolean = prefs.getBoolean("downloadRequireWifi", true)
    fun setDownloadRequireWifi(v: Boolean) = prefs.edit { putBoolean("downloadRequireWifi", v) }

    fun downloadRequireCharging(): Boolean = prefs.getBoolean("downloadRequireCharging", false)
    fun setDownloadRequireCharging(v: Boolean) = prefs.edit { putBoolean("downloadRequireCharging", v) }

    fun syndicationLookaheadCount(): Int = prefs.getInt("syndicationLookaheadCount", 6)
    fun setSyndicationLookaheadCount(count: Int) = prefs.edit { putInt("syndicationLookaheadCount", count.coerceAtLeast(1)) }

    fun storageCapMb(): Int = prefs.getInt("storageCapMb", 5000)
    fun setStorageCapMb(mb: Int) = prefs.edit { putInt("storageCapMb", mb.coerceAtLeast(500)) }

    fun getDownloadFileName(guid: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(guid.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) } + ".audio"
    }

    suspend fun reconcileDownloadsOnStartup() = withContext(Dispatchers.IO) {
        val dir = File(dataDirectoryPath(), "episodes").apply { mkdirs() }
        val downloadedEpisodes = dao.allEpisodeStatesNow().filter { it.localPath != null }

        downloadedEpisodes.forEach { ep ->
            val path = ep.localPath
            if (path != null && !File(path).exists()) {
                dao.setLocal(ep.guid, null, false)
            }
        }

        val validPaths = dao.allEpisodeStatesNow().mapNotNull { it.localPath }.toSet()
        dir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".audio") && !validPaths.contains(file.path)) {
                file.delete()
            }
        }
    }

    suspend fun deleteAllPlayedDownloads() = withContext(Dispatchers.IO) {
        val played = dao.playedEpisodesNow().filter { it.localPath != null }
        played.forEach { ep ->
            deleteDownload(ep)
        }
    }

    // ---------- Network & Data location settings ----------
    fun wifiEnabled(): Boolean = prefs.getBoolean("wifiEnabled", true)
    fun setWifiEnabled(v: Boolean) = prefs.edit { putBoolean("wifiEnabled", v) }

    fun mobileDataEnabled(): Boolean = prefs.getBoolean("mobileDataEnabled", true)
    fun setMobileDataEnabled(v: Boolean) = prefs.edit { putBoolean("mobileDataEnabled", v) }

    // ---------- Playback & Skip Settings ----------
    fun skipBackIntervalSec(): Int = prefs.getInt("skipBackIntervalSec", 10)
    fun setSkipBackIntervalSec(sec: Int) = prefs.edit { putInt("skipBackIntervalSec", sec.coerceAtLeast(1)) }

    fun skipForwardIntervalSec(): Int = prefs.getInt("skipForwardIntervalSec", 30)
    fun setSkipForwardIntervalSec(sec: Int) = prefs.edit { putInt("skipForwardIntervalSec", sec.coerceAtLeast(1)) }

    fun skipSilenceEnabled(): Boolean = prefs.getBoolean("skipSilenceEnabled", false)
    fun setSkipSilenceEnabled(v: Boolean) = prefs.edit { putBoolean("skipSilenceEnabled", v) }

    fun nextButtonBehavior(): String = prefs.getString("nextButtonBehavior", "MARK_PLAYED") ?: "MARK_PLAYED"
    fun setNextButtonBehavior(behavior: String) = prefs.edit { putString("nextButtonBehavior", behavior) }

    fun playedThresholdSec(): Int = prefs.getInt("playedThresholdSec", 30)
    fun setPlayedThresholdSec(sec: Int) = prefs.edit { putInt("playedThresholdSec", sec.coerceAtLeast(0)) }

    // ---------- Slate Queue Management ----------
    fun slatePriority(): String = prefs.getString("slatePriority", "SLATE_FIRST") ?: "SLATE_FIRST"
    fun setSlatePriority(priority: String) = prefs.edit { putString("slatePriority", priority) }

    suspend fun reorderSlate(guids: List<String>) = withContext(Dispatchers.IO) {
        guids.forEachIndexed { index, guid ->
            dao.updateSlatePosition(guid, index)
        }
    }

    suspend fun playSlateNext(guid: String) = withContext(Dispatchers.IO) {
        val currentSlate = dao.slateEpisodesNow()
        val minPos = currentSlate.minOfOrNull { it.slatePosition } ?: 0
        dao.setInSlateWithPosition(guid, inSlate = true, position = minPos - 1)
    }

    suspend fun addSlateToEnd(guid: String) = withContext(Dispatchers.IO) {
        val currentSlate = dao.slateEpisodesNow()
        val maxPos = currentSlate.maxOfOrNull { it.slatePosition } ?: 0
        dao.setInSlateWithPosition(guid, inSlate = true, position = maxPos + 1)
    }

    suspend fun clearPlayedSlate() = withContext(Dispatchers.IO) {
        dao.clearPlayedSlateEpisodes()
    }

    // ---------- Theme Settings ----------
    fun themeMode(): com.rabbitear.app.ui.theme.ThemeMode {
        val str = prefs.getString("theme_mode", com.rabbitear.app.ui.theme.ThemeMode.SYSTEM.name)
        return try { com.rabbitear.app.ui.theme.ThemeMode.valueOf(str ?: com.rabbitear.app.ui.theme.ThemeMode.SYSTEM.name) } catch (_: Exception) { com.rabbitear.app.ui.theme.ThemeMode.SYSTEM }
    }

    fun setThemeMode(mode: com.rabbitear.app.ui.theme.ThemeMode) {
        prefs.edit { putString("theme_mode", mode.name) }
    }

    fun dataDirectoryPath(): String {
        val custom = prefs.getString("dataDir", null)
        if (!custom.isNullOrEmpty()) return custom
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        return File(docsDir, "RabbitEarData").path
    }

    fun setDataDirectoryPath(path: String) {
        prefs.edit { putString("dataDir", path) }
    }

    fun dataDirectoryUri(): String {
        val uriStr = prefs.getString("dataDirUri", null)
        if (!uriStr.isNullOrEmpty()) return uriStr
        return dataDirectoryPath()
    }

    fun setDataDirectoryUri(uri: String) {
        prefs.edit { putString("dataDirUri", uri) }
    }

    fun lastPlayedGuid(): String? = prefs.getString("lastPlayedGuid", null)
    fun setLastPlayedGuid(guid: String?) = prefs.edit { if (guid == null) remove("lastPlayedGuid") else putString("lastPlayedGuid", guid) }

    // ---------- Voice Boost & Audio Normalization ----------
    fun voiceBoostEnabled(): Boolean = prefs.getBoolean("voiceBoostEnabled", false)
    fun setVoiceBoostEnabled(v: Boolean) = prefs.edit { putBoolean("voiceBoostEnabled", v) }

    fun audioNormalizationEnabled(): Boolean = prefs.getBoolean("audioNormalizationEnabled", false)
    fun setAudioNormalizationEnabled(v: Boolean) = prefs.edit { putBoolean("audioNormalizationEnabled", v) }

    // ---------- Vehicle & Automotive settings ----------
    fun autoPlayInVehicle(): Boolean = prefs.getBoolean("autoPlayInVehicle", false)
    fun setAutoPlayInVehicle(v: Boolean) = prefs.edit { putBoolean("autoPlayInVehicle", v) }

    // ---------- Profiles ----------
    suspend fun getProfileForPodcast(podcastUrl: String?): PodcastProfile = withContext(Dispatchers.IO) {
        if (podcastUrl != null) {
            val podProfile = dao.profile(podcastUrl)
            if (podProfile != null && podProfile.enabled) {
                return@withContext podProfile
            }
        }
        val global = dao.profile("GLOBAL")
        return@withContext global ?: PodcastProfile(feedUrl = "GLOBAL", speed = 1.0f, skipIntroSec = 0, skipOutroSec = 0, enabled = true)
    }

    suspend fun saveProfile(p: PodcastProfile) = withContext(Dispatchers.IO) {
        dao.saveProfile(p)
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

    fun getBackupDirectory(c: Context = ctx): File {
        val path = dataDirectoryPath()
        val targetDir = File(path).apply { mkdirs() }
        return if (targetDir.exists() && targetDir.canWrite()) targetDir else c.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: c.filesDir
    }

    suspend fun autoBackupAllFormats(c: Context = ctx) = withContext(Dispatchers.IO) {
        try {
            val dir = getBackupDirectory(c)
            File(dir, "rabbitear_settings.json").outputStream().use { exportSettings(it) }
            File(dir, "rabbitear_subscriptions.opml").outputStream().use { exportOpml(it) }
            File(dir, "rabbitear_library.html").outputStream().use { exportHtml(it) }
            exportDatabaseDirect(c)
        } catch (_: Exception) {}
    }

    suspend fun exportOpml(out: java.io.OutputStream) = withContext(Dispatchers.IO) {
        val subs = dao.subscriptionsNow()
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<opml version=\"2.0\">\n")
        sb.append("  <head>\n    <title>RabbitEar Subscriptions</title>\n  </head>\n")
        sb.append("  <body>\n")
        subs.forEach { p ->
            val title = p.title.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;")
            val url = p.feedUrl.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;")
            sb.append("    <outline type=\"rss\" xmlUrl=\"$url\" text=\"$title\" title=\"$title\"/>\n")
        }
        sb.append("  </body>\n")
        sb.append("</opml>\n")
        out.use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
    }

    suspend fun importOpml(input: java.io.InputStream): Int = withContext(Dispatchers.IO) {
        val xml = input.use { it.bufferedReader(Charsets.UTF_8).readText() }
        val xmlUrlRegex = Regex("""xmlUrl=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val urls = xmlUrlRegex.findAll(xml).map { it.groupValues[1] }.distinct().toList()
        var imported = 0
        urls.forEach { feedUrl ->
            try {
                http.newCall(Request.Builder().url(feedUrl).build()).execute().use { r ->
                    val body = r.body
                    if (r.isSuccessful && body != null) {
                        val (p, eps) = parseFeed(feedUrl, body.byteStream())
                        dao.addPodcast(p.copy(subscribed = true))
                        dao.insertEpisodes(eps)
                        dao.setSubscribed(feedUrl, true)
                        imported++
                    }
                }
            } catch (_: Exception) {}
        }
        imported
    }

    suspend fun exportHtml(out: java.io.OutputStream) = withContext(Dispatchers.IO) {
        val subs = dao.subscriptionsNow()
        val episodes = dao.allEpisodeStatesNow()
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>RabbitEar Podcast Library</title>")
        sb.append("<style>body{font-family:monospace;background:#14100c;color:#ebdbb2;padding:20px;}")
        sb.append(".card{background:#262019;border:1px solid #4a3f31;border-left:3px solid #fe8019;padding:12px;margin-bottom:10px;border-radius:8px;}")
        sb.append("h1{color:#fe8019;}h3{margin:0 0 6px 0;color:#ebdbb2;}a{color:#fe8019;text-decoration:none;}</style></head><body>")
        sb.append("<h1>RabbitEar Podcast Library</h1>")
        sb.append("<h2>Subscriptions (${subs.size})</h2>")
        subs.forEach { p ->
            sb.append("<div class=\"card\"><h3>${p.title}</h3><p>Author: ${p.author}</p><p>RSS: <a href=\"${p.feedUrl}\">${p.feedUrl}</a></p></div>")
        }
        sb.append("<h2>Saved & Listened Episodes (${episodes.size})</h2>")
        episodes.forEach { e ->
            sb.append("<div class=\"card\"><h3>${e.title}</h3><p>Played: ${e.played} | Favorite: ${e.favorite} | Slate: ${e.inSlate}</p></div>")
        }
        sb.append("</body></html>")
        out.use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
    }

    suspend fun exportDatabaseDirect(c: Context = ctx): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = c.getDatabasePath("rabbitear.db")
            if (dbFile.exists()) {
                val backupFile = File(getBackupDirectory(c), "rabbitear_backup.db")
                dbFile.inputStream().use { input -> backupFile.outputStream().use { input.copyTo(it) } }
                true
            } else false
        } catch (_: Exception) { false }
    }
}
