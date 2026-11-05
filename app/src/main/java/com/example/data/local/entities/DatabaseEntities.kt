package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "jarvis_tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "General",
    val isCompleted: Boolean = false,
    val priority: String = "Normal", // Low, Normal, High, Critical
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "jarvis_notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "jarvis_memory")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "Preference", // UserInfo, Preference, Project, Context
    val confidence: Float = 1.0f,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "jarvis_action_logs")
data class ActionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String,
    val actionDescription: String,
    val status: String, // SUCCESS, REJECTED, FAILED, PENDING
    val riskLevel: String, // LOW, HIGH
    val wasHumanApproved: Boolean = false,
    val details: String = ""
)

@Entity(tableName = "jarvis_plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goal: String,
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val status: String = "COMPLETED", // PENDING, IN_PROGRESS, COMPLETED, FAILED, WAITING_APPROVAL
    val reasoning: String = "",
    val stepsJson: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

// Legacy music entities kept for database compatibility
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId"])]
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val mediaUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val isVideo: Boolean = false,
    val albumArtUri: String? = null,
    val trackOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val mediaUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val isVideo: Boolean = false,
    val albumArtUri: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey
    val mediaUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val isVideo: Boolean = false,
    val albumArtUri: String? = null,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val lastPositionMs: Long = 0L
)
