package com.example.rabbitear.data

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
    val fundingText: String? = null
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
    val transcriptUrl: String? = null,
    val transcriptType: String? = null,
    val chaptersUrl: String? = null,
    val personName: String? = null,
    val locationName: String? = null
)

@Entity(tableName = "profiles")
data class PodcastProfile(
    @PrimaryKey val feedUrl: String, // "GLOBAL" for global profile, or podcast feedUrl
    val speed: Float = 1.0f,
    val skipIntroSec: Int = 0,
    val skipOutroSec: Int = 0,
    val enabled: Boolean = true
)

@Dao
interface RabbitEarDao {
    @Query("SELECT * FROM Podcast WHERE subscribed ORDER BY title") fun subscriptions(): Flow<List<Podcast>>
    @Query("SELECT * FROM Podcast WHERE subscribed ORDER BY title") suspend fun subscriptionsNow(): List<Podcast>
    @Query("SELECT * FROM Podcast WHERE feedUrl=:u") fun podcastFlow(u: String): Flow<Podcast?>
    @Query("SELECT * FROM Podcast WHERE feedUrl=:u") suspend fun podcast(u: String): Podcast?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addPodcast(p: Podcast)
    @Query("UPDATE Podcast SET subscribed=:on WHERE feedUrl=:u") suspend fun setSubscribed(u: String, on: Boolean)

    // Profiles
    @Query("SELECT * FROM profiles WHERE feedUrl=:u") fun profileFlow(u: String): Flow<PodcastProfile?>
    @Query("SELECT * FROM profiles WHERE feedUrl=:u") suspend fun profile(u: String): PodcastProfile?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveProfile(p: PodcastProfile)

    // IGNORE keeps play progress and downloads intact when a feed is refreshed
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertEpisodes(e: List<Episode>)
    @Query("SELECT * FROM Episode WHERE podcastUrl=:u ORDER BY pubDate DESC") fun episodes(u: String): Flow<List<Episode>>
    @Query("SELECT * FROM Episode WHERE guid=:g") suspend fun episode(g: String): Episode?
    @Query("SELECT * FROM Episode WHERE played = 1 OR positionMs > 0") suspend fun playedEpisodesNow(): List<Episode>
    @Query("SELECT * FROM Episode WHERE played = 1 OR positionMs > 0 OR favorite = 1 OR inSlate = 1") suspend fun allEpisodeStatesNow(): List<Episode>
    @Query("SELECT * FROM Episode WHERE favorite = 1 ORDER BY pubDate DESC") fun favoriteEpisodes(): Flow<List<Episode>>
    @Query("SELECT * FROM Episode WHERE favorite = 1 ORDER BY pubDate DESC") suspend fun favoriteEpisodesNow(): List<Episode>
    @Query("SELECT * FROM Episode WHERE inSlate = 1 ORDER BY slateAddedTime ASC") fun slateEpisodes(): Flow<List<Episode>>
    @Query("SELECT * FROM Episode WHERE inSlate = 1 ORDER BY slateAddedTime ASC") suspend fun slateEpisodesNow(): List<Episode>
    @Query("SELECT * FROM Episode WHERE podcastUrl=:u AND played=0 ORDER BY pubDate ASC LIMIT 1") suspend fun oldestUnplayed(u: String): Episode?
    @Query("SELECT * FROM Episode WHERE podcastUrl=:u AND played=0 ORDER BY pubDate DESC LIMIT 1") suspend fun newestUnplayed(u: String): Episode?
    @Query("SELECT * FROM Episode WHERE podcastUrl=:u AND played=0 ORDER BY pubDate ASC LIMIT 1 OFFSET :offset") suspend fun oldestUnplayedWithOffset(u: String, offset: Int): Episode?
    @Query("SELECT * FROM Episode WHERE podcastUrl=:u AND played=0 ORDER BY pubDate DESC LIMIT 1 OFFSET :offset") suspend fun newestUnplayedWithOffset(u: String, offset: Int): Episode?
    @Query("SELECT * FROM Episode WHERE podcastUrl=:u ORDER BY pubDate DESC LIMIT 1") suspend fun newestEpisode(u: String): Episode?
    @Query("SELECT * FROM Episode WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY pubDate DESC LIMIT 50")
    suspend fun searchEpisodes(query: String): List<Episode>
    @Query("UPDATE Episode SET positionMs=:pos, durationMs=CASE WHEN :dur>0 THEN :dur ELSE durationMs END WHERE guid=:g")
    suspend fun progress(g: String, pos: Long, dur: Long)
    @Query("UPDATE Episode SET played=:v, positionMs=0 WHERE guid=:g") suspend fun setPlayed(g: String, v: Boolean)
    @Query("UPDATE Episode SET favorite=:v WHERE guid=:g") suspend fun setFavorite(g: String, v: Boolean)
    @Query("UPDATE Episode SET inSlate=:inSlate, slateAddedTime=:addedTime WHERE guid=:g") suspend fun setInSlate(g: String, inSlate: Boolean, addedTime: Long = System.currentTimeMillis())
    @Query("UPDATE Episode SET localPath=:path, downloadedBySyndication=:bySyndication WHERE guid=:g")
    suspend fun setLocal(g: String, path: String?, bySyndication: Boolean = false)

    @Query("UPDATE Episode SET played=:played, positionMs=:pos, durationMs=CASE WHEN :dur>0 THEN :dur ELSE durationMs END, favorite=:fav, inSlate=:inSlate, slateAddedTime=:addedTime WHERE guid=:g")
    suspend fun restoreStateByGuid(g: String, played: Boolean, pos: Long, dur: Long, fav: Boolean, inSlate: Boolean, addedTime: Long): Int

    @Query("UPDATE Episode SET played=:played, positionMs=:pos, durationMs=CASE WHEN :dur>0 THEN :dur ELSE durationMs END, favorite=:fav, inSlate=:inSlate, slateAddedTime=:addedTime WHERE audioUrl=:url")
    suspend fun restoreStateByAudioUrl(url: String, played: Boolean, pos: Long, dur: Long, fav: Boolean, inSlate: Boolean, addedTime: Long): Int
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

@Database(entities = [Podcast::class, Episode::class, PodcastProfile::class], version = 8, exportSchema = false)
abstract class Db : RoomDatabase() { abstract fun dao(): RabbitEarDao }
