package com.rabbitear.app.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity
data class Podcast(
    @PrimaryKey val feedUrl: String,
    val title: String,
    val author: String,
    val image: String,
    val subscribed: Boolean = false,
    val podcastGuid: String? = null,
    val fundingUrl: String? = null,
    val fundingText: String? = null,
    val etag: String? = null,
    val lastModified: String? = null,
    val lastSuccessTime: Long = 0L,
    val lastErrorMsg: String? = null,
    val basicAuthUsername: String? = null,
    val basicAuthPasswordEncrypted: String? = null
)

@Entity(indices = [Index("podcastUrl")])
data class Episode(
    @PrimaryKey val guid: String,
    val podcastUrl: String,
    val title: String,
    val audioUrl: String,
    val pubDate: Long,
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    val played: Boolean = false,
    val localPath: String? = null,
    val downloadedBySyndication: Boolean = false,
    val image: String? = null,
    val description: String? = null,
    val favorite: Boolean = false,
    val inSlate: Boolean = false,
    val slateAddedTime: Long = 0L,
    val slatePosition: Int = 0,
    val transcriptUrl: String? = null,
    val transcriptType: String? = null,
    val chaptersUrl: String? = null,
    val personName: String? = null,
    val locationName: String? = null,
    val episodeNumber: Int? = null,
    val seasonNumber: Int? = null,
    val episodeType: String? = null,
    val isExplicit: Boolean = false
)

@Entity(tableName = "podcast_settings")
data class PodcastSettings(
    @PrimaryKey val feedUrl: String,
    val excludeFromSchedule: Boolean = false,
    val autoDownloadPolicy: String = "NEVER",
    val notifyNewEpisodes: Boolean = true,
    val customSortOrder: Int = 0,
    val playbackSpeedOverride: Float? = null,
    val skipIntroSec: Int = 0,
    val skipOutroSec: Int = 0
)

@Entity(tableName = "profiles")
data class PodcastProfile(
    @PrimaryKey val feedUrl: String,
    val speed: Float = 1.0f,
    val skipIntroSec: Int = 0,
    val skipOutroSec: Int = 0,
    val enabled: Boolean = true
)

@Entity(tableName = "schedule_state")
data class ScheduleState(
    @PrimaryKey val mode: String, // "OLDEST" or "NEWEST"
    val lastShowFeedUrl: String? = null,
    val currentEpisodeGuid: String? = null,
    val rotationTurnCount: Int = 0
)

@Dao
interface RabbitEarDao {
    @Query("SELECT * FROM Podcast WHERE subscribed ORDER BY title") fun subscriptions(): Flow<List<Podcast>>
    @Query("SELECT * FROM Podcast WHERE subscribed ORDER BY title") suspend fun subscriptionsNow(): List<Podcast>
    @Query("SELECT * FROM Podcast WHERE feedUrl=:u") fun podcastFlow(u: String): Flow<Podcast?>
    @Query("SELECT * FROM Podcast WHERE feedUrl=:u") suspend fun podcast(u: String): Podcast?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addPodcast(p: Podcast)
    @Query("UPDATE Podcast SET subscribed=:on WHERE feedUrl=:u") suspend fun setSubscribed(u: String, on: Boolean)
    @Query("UPDATE Podcast SET etag=:etag, lastModified=:lastModified, lastSuccessTime=:lastSuccessTime, lastErrorMsg=:lastErrorMsg WHERE feedUrl=:u")
    suspend fun updateFeedSyncState(u: String, etag: String?, lastModified: String?, lastSuccessTime: Long, lastErrorMsg: String?)

    // Show settings
    @Query("SELECT * FROM podcast_settings WHERE feedUrl=:u") fun settingsFlow(u: String): Flow<PodcastSettings?>
    @Query("SELECT * FROM podcast_settings WHERE feedUrl=:u") suspend fun settings(u: String): PodcastSettings?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSettings(s: PodcastSettings)

    // Profiles
    @Query("SELECT * FROM profiles WHERE feedUrl=:u") fun profileFlow(u: String): Flow<PodcastProfile?>
    @Query("SELECT * FROM profiles WHERE feedUrl=:u") suspend fun profile(u: String): PodcastProfile?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveProfile(p: PodcastProfile)

    // Schedule State
    @Query("SELECT * FROM schedule_state WHERE mode=:mode") fun scheduleStateFlow(mode: String): Flow<ScheduleState?>
    @Query("SELECT * FROM schedule_state WHERE mode=:mode") suspend fun scheduleState(mode: String): ScheduleState?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveScheduleState(state: ScheduleState)

    @Query("""
        UPDATE Episode SET 
            title = :title, 
            audioUrl = :audioUrl, 
            durationMs = CASE WHEN :dur > 0 THEN :dur ELSE durationMs END, 
            image = COALESCE(:image, image), 
            description = COALESCE(:description, description), 
            transcriptUrl = COALESCE(:transcriptUrl, transcriptUrl), 
            transcriptType = COALESCE(:transcriptType, transcriptType), 
            chaptersUrl = COALESCE(:chaptersUrl, chaptersUrl), 
            personName = COALESCE(:personName, personName), 
            locationName = COALESCE(:locationName, locationName), 
            episodeNumber = COALESCE(:episodeNumber, episodeNumber), 
            seasonNumber = COALESCE(:seasonNumber, seasonNumber), 
            episodeType = COALESCE(:episodeType, episodeType), 
            isExplicit = :isExplicit 
        WHERE guid = :guid
    """)
    suspend fun updateEpisodeMetadata(
        guid: String,
        title: String,
        audioUrl: String,
        dur: Long,
        image: String?,
        description: String?,
        transcriptUrl: String?,
        transcriptType: String?,
        chaptersUrl: String?,
        personName: String?,
        locationName: String?,
        episodeNumber: Int?,
        seasonNumber: Int?,
        episodeType: String?,
        isExplicit: Boolean
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertEpisodes(e: List<Episode>)
    @Query("SELECT * FROM Episode WHERE podcastUrl=:u ORDER BY pubDate DESC") fun episodes(u: String): Flow<List<Episode>>
    @Query("SELECT * FROM Episode WHERE guid=:g") suspend fun episode(g: String): Episode?
    @Query("SELECT * FROM Episode WHERE played = 1 OR positionMs > 0") suspend fun playedEpisodesNow(): List<Episode>
    @Query("SELECT * FROM Episode WHERE played = 1 OR positionMs > 0 OR favorite = 1 OR inSlate = 1") suspend fun allEpisodeStatesNow(): List<Episode>
    @Query("SELECT * FROM Episode WHERE favorite = 1 ORDER BY pubDate DESC") fun favoriteEpisodes(): Flow<List<Episode>>
    @Query("SELECT * FROM Episode WHERE favorite = 1 ORDER BY pubDate DESC") suspend fun favoriteEpisodesNow(): List<Episode>
    
    // Slate
    @Query("SELECT * FROM Episode WHERE inSlate = 1 ORDER BY slatePosition ASC, slateAddedTime ASC") fun slateEpisodes(): Flow<List<Episode>>
    @Query("SELECT * FROM Episode WHERE inSlate = 1 ORDER BY slatePosition ASC, slateAddedTime ASC") suspend fun slateEpisodesNow(): List<Episode>
    @Query("UPDATE Episode SET slatePosition = :position WHERE guid = :guid") suspend fun updateSlatePosition(guid: String, position: Int)
    @Query("UPDATE Episode SET inSlate = 0 WHERE inSlate = 1 AND played = 1") suspend fun clearPlayedSlateEpisodes()

    // Cereal Mode Rotation Queries (with Serial ordering support!)
    @Query("SELECT * FROM Episode WHERE podcastUrl=:u AND played=0 ORDER BY CASE WHEN seasonNumber IS NOT NULL AND episodeNumber IS NOT NULL THEN (seasonNumber * 10000 + episodeNumber) ELSE pubDate END ASC LIMIT 1") suspend fun oldestUnplayed(u: String): Episode?
    @Query("SELECT * FROM Episode WHERE podcastUrl=:u AND played=0 ORDER BY pubDate DESC LIMIT 1") suspend fun newestUnplayed(u: String): Episode?
    @Query("SELECT * FROM Episode WHERE podcastUrl=:u AND played=0 ORDER BY CASE WHEN seasonNumber IS NOT NULL AND episodeNumber IS NOT NULL THEN (seasonNumber * 10000 + episodeNumber) ELSE pubDate END ASC LIMIT 1 OFFSET :offset") suspend fun oldestUnplayedWithOffset(u: String, offset: Int): Episode?
    @Query("SELECT * FROM Episode WHERE podcastUrl=:u AND played=0 ORDER BY pubDate DESC LIMIT 1 OFFSET :offset") suspend fun newestUnplayedWithOffset(u: String, offset: Int): Episode?
    @Query("SELECT * FROM Episode WHERE podcastUrl=:u ORDER BY pubDate DESC LIMIT 1") suspend fun newestEpisode(u: String): Episode?
    
    @Query("SELECT * FROM Episode WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY pubDate DESC LIMIT 50")
    suspend fun searchEpisodes(query: String): List<Episode>
    @Query("UPDATE Episode SET positionMs=:pos, durationMs=CASE WHEN :dur>0 THEN :dur ELSE durationMs END WHERE guid=:g")
    suspend fun progress(g: String, pos: Long, dur: Long)
    @Query("UPDATE Episode SET played=:v, positionMs=0 WHERE guid=:g") suspend fun setPlayed(g: String, v: Boolean)
    @Query("UPDATE Episode SET favorite=:v WHERE guid=:g") suspend fun setFavorite(g: String, v: Boolean)
    @Query("UPDATE Episode SET inSlate=:inSlate, slateAddedTime=:addedTime, slatePosition=:position WHERE guid=:g") suspend fun setInSlateWithPosition(g: String, inSlate: Boolean, addedTime: Long = System.currentTimeMillis(), position: Int = 0)
    @Query("UPDATE Episode SET inSlate=:inSlate, slateAddedTime=:addedTime WHERE guid=:g") suspend fun setInSlate(g: String, inSlate: Boolean, addedTime: Long = System.currentTimeMillis())
    @Query("UPDATE Episode SET localPath=:path, downloadedBySyndication=:bySyndication WHERE guid=:g")
    suspend fun setLocal(g: String, path: String?, bySyndication: Boolean = false)

    @Query("UPDATE Episode SET played=:played, positionMs=:pos, durationMs=CASE WHEN :dur>0 THEN :dur ELSE durationMs END, favorite=:fav, inSlate=:inSlate, slateAddedTime=:addedTime WHERE guid=:g")
    suspend fun restoreStateByGuid(g: String, played: Boolean, pos: Long, dur: Long, fav: Boolean, inSlate: Boolean, addedTime: Long): Int

    @Query("UPDATE Episode SET played=:played, positionMs=:pos, durationMs=CASE WHEN :dur>0 THEN :dur ELSE durationMs END, favorite=:fav, inSlate=:inSlate, slateAddedTime=:addedTime WHERE audioUrl=:url")
    suspend fun restoreStateByAudioUrl(url: String, played: Boolean, pos: Long, dur: Long, fav: Boolean, inSlate: Boolean, addedTime: Long): Int

    @Query("DELETE FROM Episode WHERE played = 1 AND favorite = 0 AND inSlate = 0 AND localPath IS NULL AND pubDate < :cutoffDate")
    suspend fun pruneOldPlayedEpisodes(cutoffDate: Long): Int
}

val MIGRATION_1_2 = object : Migration(1, 2) { override fun migrate(db: SupportSQLiteDatabase) {} }
val MIGRATION_2_3 = object : Migration(2, 3) { override fun migrate(db: SupportSQLiteDatabase) {} }
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try { db.execSQL("ALTER TABLE Episode ADD COLUMN description TEXT DEFAULT NULL") } catch (_: Exception) {}
    }
}
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try { db.execSQL("ALTER TABLE Episode ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0") } catch (_: Exception) {}
    }
}
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE Episode ADD COLUMN inSlate INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE Episode ADD COLUMN slateAddedTime INTEGER NOT NULL DEFAULT 0")
        } catch (_: Exception) {}
    }
}
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `profiles` (`feedUrl` TEXT NOT NULL, `speed` REAL NOT NULL DEFAULT 1.0, `skipIntroSec` INTEGER NOT NULL DEFAULT 0, `skipOutroSec` INTEGER NOT NULL DEFAULT 0, `enabled` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`feedUrl`))"
            )
        } catch (_: Exception) {}
    }
}
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE Podcast ADD COLUMN podcastGuid TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE Podcast ADD COLUMN fundingUrl TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE Podcast ADD COLUMN fundingText TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE Episode ADD COLUMN transcriptUrl TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE Episode ADD COLUMN transcriptType TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE Episode ADD COLUMN chaptersUrl TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE Episode ADD COLUMN personName TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE Episode ADD COLUMN locationName TEXT DEFAULT NULL")
        } catch (_: Exception) {}
    }
}
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE Podcast ADD COLUMN etag TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE Podcast ADD COLUMN lastModified TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE Podcast ADD COLUMN lastSuccessTime INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE Podcast ADD COLUMN lastErrorMsg TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE Podcast ADD COLUMN basicAuthUsername TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE Podcast ADD COLUMN basicAuthPasswordEncrypted TEXT DEFAULT NULL")

            db.execSQL("ALTER TABLE Episode ADD COLUMN episodeNumber INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE Episode ADD COLUMN seasonNumber INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE Episode ADD COLUMN episodeType TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE Episode ADD COLUMN isExplicit INTEGER NOT NULL DEFAULT 0")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `podcast_settings` (`feedUrl` TEXT NOT NULL, `excludeFromSchedule` INTEGER NOT NULL DEFAULT 0, `autoDownloadPolicy` TEXT NOT NULL DEFAULT 'NEVER', `notifyNewEpisodes` INTEGER NOT NULL DEFAULT 1, `customSortOrder` INTEGER NOT NULL DEFAULT 0, `playbackSpeedOverride` REAL DEFAULT NULL, `skipIntroSec` INTEGER NOT NULL DEFAULT 0, `skipOutroSec` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`feedUrl`))"
            )
        } catch (_: Exception) {}
    }
}
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE Episode ADD COLUMN slatePosition INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `schedule_state` (`mode` TEXT NOT NULL, `lastShowFeedUrl` TEXT DEFAULT NULL, `currentEpisodeGuid` TEXT DEFAULT NULL, `rotationTurnCount` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`mode`))"
            )
        } catch (_: Exception) {}
    }
}

@Database(entities = [Podcast::class, Episode::class, PodcastProfile::class, PodcastSettings::class, ScheduleState::class], version = 10, exportSchema = true)
abstract class Db : RoomDatabase() { abstract fun dao(): RabbitEarDao }
