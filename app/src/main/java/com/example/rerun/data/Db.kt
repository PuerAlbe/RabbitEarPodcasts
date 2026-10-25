package com.example.rerun.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity
data class Podcast(
    @PrimaryKey val feedUrl: String,
    val title: String,
    val author: String,
    val image: String,
    val subscribed: Boolean = false
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
    val image: String? = null
)

@Dao
interface RerunDao {
    @Query("SELECT * FROM Podcast WHERE subscribed ORDER BY title") fun subscriptions(): Flow<List<Podcast>>
    @Query("SELECT * FROM Podcast WHERE subscribed ORDER BY title") suspend fun subscriptionsNow(): List<Podcast>
    @Query("SELECT * FROM Podcast WHERE feedUrl=:u") fun podcastFlow(u: String): Flow<Podcast?>
    @Query("SELECT * FROM Podcast WHERE feedUrl=:u") suspend fun podcast(u: String): Podcast?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addPodcast(p: Podcast)
    @Query("UPDATE Podcast SET subscribed=:on WHERE feedUrl=:u") suspend fun setSubscribed(u: String, on: Boolean)

    // IGNORE keeps play progress and downloads intact when a feed is refreshed
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertEpisodes(e: List<Episode>)
    @Query("SELECT * FROM Episode WHERE podcastUrl=:u ORDER BY pubDate DESC") fun episodes(u: String): Flow<List<Episode>>
    @Query("SELECT * FROM Episode WHERE guid=:g") suspend fun episode(g: String): Episode?
    @Query("SELECT * FROM Episode WHERE played = 1 OR positionMs > 0") suspend fun playedEpisodesNow(): List<Episode>
    @Query("SELECT * FROM Episode WHERE podcastUrl=:u AND played=0 ORDER BY pubDate ASC LIMIT 1") suspend fun oldestUnplayed(u: String): Episode?
    @Query("SELECT * FROM Episode WHERE podcastUrl=:u AND played=0 ORDER BY pubDate DESC LIMIT 1") suspend fun newestUnplayed(u: String): Episode?
    @Query("UPDATE Episode SET positionMs=:pos, durationMs=CASE WHEN :dur>0 THEN :dur ELSE durationMs END WHERE guid=:g")
    suspend fun progress(g: String, pos: Long, dur: Long)
    @Query("UPDATE Episode SET played=:v, positionMs=0 WHERE guid=:g") suspend fun setPlayed(g: String, v: Boolean)
    @Query("UPDATE Episode SET localPath=:path, downloadedBySyndication=:bySyndication WHERE guid=:g")
    suspend fun setLocal(g: String, path: String?, bySyndication: Boolean = false)
}

@Database(entities = [Podcast::class, Episode::class], version = 3, exportSchema = false)
abstract class Db : RoomDatabase() { abstract fun dao(): RerunDao }
