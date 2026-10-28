package com.example

import android.app.Application
import com.example.data.local.NovaDatabase
import com.example.data.repository.MediaRepository
import com.example.data.repository.PreferencesRepository
import com.example.player.PlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NovaApplication : Application() {
    private val applicationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    lateinit var database: NovaDatabase
        private set

    lateinit var mediaRepository: MediaRepository
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = NovaDatabase.getInstance(this)
        preferencesRepository = PreferencesRepository(this)
        mediaRepository = MediaRepository(this, database)

        PlayerManager.initialize(this, mediaRepository, preferencesRepository)

        // Initial library scan
        applicationScope.launch {
            mediaRepository.refreshLibrary()
        }
    }
}
