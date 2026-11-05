package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM jarvis_tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM jarvis_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM jarvis_tasks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    suspend fun getPendingTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM jarvis_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE jarvis_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, completed: Boolean)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM jarvis_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM jarvis_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM jarvis_notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    suspend fun searchNotes(query: String): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM jarvis_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM jarvis_memory ORDER BY updatedAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM jarvis_memory WHERE `key` = :key LIMIT 1")
    suspend fun getMemoryByKey(key: String): MemoryEntity?

    @Query("SELECT * FROM jarvis_memory WHERE category = :category ORDER BY updatedAt DESC")
    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Query("DELETE FROM jarvis_memory WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM jarvis_memory WHERE `key` = :key")
    suspend fun deleteMemoryByKey(key: String)
}

@Dao
interface ActionLogDao {
    @Query("SELECT * FROM jarvis_action_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<ActionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActionLogEntity): Long

    @Query("DELETE FROM jarvis_action_logs")
    suspend fun clearLogs()
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM jarvis_plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM jarvis_plans ORDER BY createdAt DESC LIMIT 1")
    fun getLatestPlan(): Flow<PlanEntity?>

    @Query("SELECT * FROM jarvis_plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: Long): PlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)
}

// Legacy music DAOs
@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY trackOrder ASC, addedAt ASC")
    fun getTracksForPlaylist(playlistId: Long): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY trackOrder ASC, addedAt ASC")
    suspend fun getTracksForPlaylistDirect(playlistId: Long): List<PlaylistTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: PlaylistTrackEntity): Long

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND mediaUri = :mediaUri")
    suspend fun removeTrackFromPlaylist(playlistId: Long, mediaUri: String)

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getTrackCount(playlistId: Long): Flow<Int>
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaUri = :mediaUri LIMIT 1)")
    fun isFavorite(mediaUri: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaUri = :mediaUri")
    suspend fun removeFavorite(mediaUri: String)
}

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE mediaUri = :mediaUri LIMIT 1")
    suspend fun getHistoryItem(mediaUri: String): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE mediaUri = :mediaUri")
    suspend fun deleteHistory(mediaUri: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}
