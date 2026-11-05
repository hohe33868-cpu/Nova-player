package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*

@Database(
    entities = [
        TaskEntity::class,
        NoteEntity::class,
        MemoryEntity::class,
        ActionLogEntity::class,
        PlanEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        FavoriteEntity::class,
        PlaybackHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun memoryDao(): MemoryDao
    abstract fun actionLogDao(): ActionLogDao
    abstract fun planDao(): PlanDao

    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): PlaybackHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: NovaDatabase? = null

        fun getInstance(context: Context): NovaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NovaDatabase::class.java,
                    "jarvis_core.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
