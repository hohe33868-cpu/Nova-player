package com.example.player

import android.content.Context
import android.content.Intent
import android.media.audiofx.Equalizer
import android.os.Build
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.AudioEffectSettings
import com.example.data.model.MediaTrack
import com.example.data.model.RepeatMode
import com.example.data.repository.MediaRepository
import com.example.data.repository.PreferencesRepository
import com.example.service.AudioEffectController
import com.example.service.EqualizerCapabilities
import com.example.service.PlaybackService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlayerManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var appContext: Context? = null
    var mediaRepository: MediaRepository? = null
    var preferencesRepository: PreferencesRepository? = null

    private var exoPlayer: ExoPlayer? = null
    private var service: PlaybackService? = null
    private var effectController: AudioEffectController? = null

    // UI StateFlows
    private val _currentTrack = MutableStateFlow<MediaTrack?>(null)
    val currentTrack: StateFlow<MediaTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaTrack>>(emptyList())
    val queue: StateFlow<List<MediaTrack>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _effectCapabilities = MutableStateFlow(EqualizerCapabilities())
    val effectCapabilities: StateFlow<EqualizerCapabilities> = _effectCapabilities.asStateFlow()

    private val _sleepTimerSecondsRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerSecondsRemaining: StateFlow<Long?> = _sleepTimerSecondsRemaining.asStateFlow()

    private var sleepTimerJob: Job? = null
    private var positionTickerJob: Job? = null

    fun initialize(
        context: Context,
        mediaRepo: MediaRepository,
        prefRepo: PreferencesRepository
    ) {
        appContext = context.applicationContext
        mediaRepository = mediaRepo
        preferencesRepository = prefRepo

        // Restore initial settings
        val prefs = prefRepo.playerPreferences.value
        _shuffleEnabled.value = prefs.shuffleEnabled
        _repeatMode.value = prefs.defaultRepeatMode
        _playbackSpeed.value = prefs.defaultSpeed

        startService()
    }

    private fun startService() {
        val ctx = appContext ?: return
        val intent = Intent(ctx, PlaybackService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("PlayerManager", "Failed to start PlaybackService", e)
        }
    }

    fun bindService(
        service: PlaybackService,
        player: ExoPlayer,
        controller: AudioEffectController
    ) {
        this.service = service
        this.exoPlayer = player
        this.effectController = controller

        // Apply repeat and shuffle
        player.shuffleModeEnabled = _shuffleEnabled.value
        applyExoRepeatMode(_repeatMode.value)
        player.playbackParameters = PlaybackParameters(_playbackSpeed.value)

        // Observe capabilities from effect controller
        scope.launch {
            controller.capabilities.collect { caps ->
                _effectCapabilities.value = caps
            }
        }

        // Attach audio effects if session id ready
        val audioSessionId = player.audioSessionId
        preferencesRepository?.audioEffectSettings?.value?.let { settings ->
            controller.attachAudioSession(audioSessionId, settings)
        }

        startPositionTicker()
    }

    fun unbindService() {
        this.service = null
        this.exoPlayer = null
        this.effectController = null
        stopPositionTicker()
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = scope.launch {
            while (isActive) {
                updatePosition()
                delay(300)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    fun updatePosition() {
        val player = exoPlayer ?: return
        val pos = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration.coerceAtLeast(0L)
        val buf = player.bufferedPosition.coerceAtLeast(0L)

        _currentPositionMs.value = pos
        if (dur > 0L) {
            _durationMs.value = dur
        }
        _bufferedPositionMs.value = buf

        // Save playback progress periodically
        _currentTrack.value?.let { track ->
            if (pos > 0) {
                preferencesRepository?.saveLastPlayedTrack(track.uri, pos)
            }
        }
    }

    fun playTrack(track: MediaTrack, newQueue: List<MediaTrack> = listOf(track)) {
        val index = newQueue.indexOfFirst { it.uri == track.uri }.let { if (it >= 0) it else 0 }
        playQueue(newQueue, index)
    }

    fun playQueue(newQueue: List<MediaTrack>, startIndex: Int = 0) {
        val player = exoPlayer ?: run {
            startService()
            return
        }

        if (newQueue.isEmpty()) return

        _queue.value = newQueue
        val safeIndex = startIndex.coerceIn(0, newQueue.size - 1)
        _currentIndex.value = safeIndex
        val targetTrack = newQueue[safeIndex]
        _currentTrack.value = targetTrack
        _durationMs.value = targetTrack.durationMs

        player.stop()
        player.clearMediaItems()

        val mediaItems = newQueue.map { it.toMediaItem() }
        player.setMediaItems(mediaItems, safeIndex, 0L)
        player.prepare()
        player.play()

        // Attach audio effect session if needed
        val sessionId = player.audioSessionId
        preferencesRepository?.audioEffectSettings?.value?.let { settings ->
            effectController?.attachAudioSession(sessionId, settings)
        }

        // Record history
        scope.launch {
            mediaRepository?.recordPlayback(targetTrack, 0L)
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: run {
            _currentTrack.value?.let { playTrack(it, _queue.value) }
            return
        }
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE && _queue.value.isNotEmpty()) {
                playQueue(_queue.value, _currentIndex.value.coerceAtLeast(0))
            } else {
                player.play()
            }
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun seekForward(seconds: Int = 10) {
        val current = _currentPositionMs.value
        val dur = _durationMs.value
        val target = if (dur > 0) (current + seconds * 1000L).coerceAtMost(dur) else current + seconds * 1000L
        seekTo(target)
    }

    fun seekBackward(seconds: Int = 10) {
        val current = _currentPositionMs.value
        val target = (current - seconds * 1000L).coerceAtLeast(0L)
        seekTo(target)
    }

    fun skipToNext() {
        val player = exoPlayer
        if (player != null && player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (_queue.value.isNotEmpty()) {
            val nextIdx = (_currentIndex.value + 1) % _queue.value.size
            playQueue(_queue.value, nextIdx)
        }
    }

    fun skipToPrevious() {
        val player = exoPlayer
        if (player != null && player.currentPosition > 3000) {
            player.seekTo(0L)
        } else if (player != null && player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else if (_queue.value.isNotEmpty()) {
            val prevIdx = if (_currentIndex.value - 1 < 0) _queue.value.size - 1 else _currentIndex.value - 1
            playQueue(_queue.value, prevIdx)
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_shuffleEnabled.value
        _shuffleEnabled.value = newShuffle
        exoPlayer?.shuffleModeEnabled = newShuffle
        preferencesRepository?.setShuffleEnabled(newShuffle)
    }

    fun cycleRepeatMode() {
        val nextMode = _repeatMode.value.next()
        _repeatMode.value = nextMode
        applyExoRepeatMode(nextMode)
        preferencesRepository?.setDefaultRepeatMode(nextMode)
    }

    private fun applyExoRepeatMode(mode: RepeatMode) {
        val player = exoPlayer ?: return
        when (mode) {
            RepeatMode.OFF -> player.repeatMode = Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> player.repeatMode = Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> player.repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
        preferencesRepository?.setDefaultSpeed(speed)
    }

    fun setStereoBalance(balance: Float) {
        // -1.0 (left only) to 1.0 (right only)
        val clamped = balance.coerceIn(-1.0f, 1.0f)
        val left = if (clamped > 0) 1.0f - clamped else 1.0f
        val right = if (clamped < 0) 1.0f + clamped else 1.0f
        exoPlayer?.setDeviceVolume(0, 0) // No-op fallback
        // Update preference
        preferencesRepository?.audioEffectSettings?.value?.let { current ->
            val updated = current.copy(stereoBalance = clamped)
            preferencesRepository?.updateAudioEffectSettings(updated)
        }
    }

    fun addToQueue(track: MediaTrack) {
        val currentList = _queue.value.toMutableList()
        currentList.add(track)
        _queue.value = currentList
        exoPlayer?.addMediaItem(track.toMediaItem())
    }

    fun playNext(track: MediaTrack) {
        val currentList = _queue.value.toMutableList()
        val insertIndex = (_currentIndex.value + 1).coerceIn(0, currentList.size)
        currentList.add(insertIndex, track)
        _queue.value = currentList
        exoPlayer?.addMediaItem(insertIndex, track.toMediaItem())
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val currentList = _queue.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _queue.value = currentList
            exoPlayer?.moveMediaItem(fromIndex, toIndex)

            // Adjust current index
            val cur = _currentIndex.value
            if (cur == fromIndex) {
                _currentIndex.value = toIndex
            } else if (fromIndex < cur && toIndex >= cur) {
                _currentIndex.value = cur - 1
            } else if (fromIndex > cur && toIndex <= cur) {
                _currentIndex.value = cur + 1
            }
        }
    }

    fun removeQueueItem(index: Int) {
        val currentList = _queue.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _queue.value = currentList
            exoPlayer?.removeMediaItem(index)
            if (_currentIndex.value >= index && _currentIndex.value > 0) {
                _currentIndex.value = _currentIndex.value - 1
            }
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentIndex.value = -1
        _currentTrack.value = null
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
    }

    // Sleep Timer
    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        val totalSeconds = minutes * 60L
        _sleepTimerSecondsRemaining.value = totalSeconds

        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _sleepTimerSecondsRemaining.value = remaining
            }
            // Timer expired: stop playback gracefully
            pause()
            _sleepTimerSecondsRemaining.value = null
        }
    }

    fun extendSleepTimer(minutes: Int) {
        val current = _sleepTimerSecondsRemaining.value ?: 0L
        val newTotal = current + (minutes * 60L)
        startSleepTimer((newTotal / 60).toInt())
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerSecondsRemaining.value = null
    }

    // Audio Effects updates
    fun updateEqualizerSettings(settings: AudioEffectSettings) {
        effectController?.applySettings(settings)
        preferencesRepository?.updateAudioEffectSettings(settings)
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMb: Short) {
        effectController?.setBandLevel(bandIndex, levelMb)
        preferencesRepository?.audioEffectSettings?.value?.let { current ->
            val updatedBands = current.bandLevels.toMutableMap()
            updatedBands[bandIndex] = levelMb
            preferencesRepository?.updateAudioEffectSettings(current.copy(bandLevels = updatedBands, presetIndex = -1))
        }
    }

    fun setEqualizerPreset(presetIndex: Short) {
        effectController?.usePreset(presetIndex)
        preferencesRepository?.audioEffectSettings?.value?.let { current ->
            preferencesRepository?.updateAudioEffectSettings(current.copy(presetIndex = presetIndex))
        }
    }

    fun setBassBoost(strength: Short) {
        preferencesRepository?.audioEffectSettings?.value?.let { current ->
            val updated = current.copy(bassBoostStrength = strength)
            effectController?.applySettings(updated)
            preferencesRepository?.updateAudioEffectSettings(updated)
        }
    }

    fun setVirtualizer(strength: Short) {
        preferencesRepository?.audioEffectSettings?.value?.let { current ->
            val updated = current.copy(virtualizerStrength = strength)
            effectController?.applySettings(updated)
            preferencesRepository?.updateAudioEffectSettings(updated)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        preferencesRepository?.audioEffectSettings?.value?.let { current ->
            val updated = current.copy(equalizerEnabled = enabled)
            effectController?.applySettings(updated)
            preferencesRepository?.updateAudioEffectSettings(updated)
        }
    }

    // Callbacks from ExoPlayer listener
    fun onPlaybackStateChanged(playbackState: Int) {
        _playbackState.value = playbackState
        if (playbackState == Player.STATE_READY) {
            _durationMs.value = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
        }
    }

    fun onIsPlayingChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val currentMediaId = mediaItem?.mediaId
        val queueList = _queue.value
        val idx = queueList.indexOfFirst { it.uri == currentMediaId }

        if (idx >= 0) {
            _currentIndex.value = idx
            val track = queueList[idx]
            _currentTrack.value = track
            _durationMs.value = track.durationMs

            scope.launch {
                mediaRepository?.recordPlayback(track, 0L)
            }
        }
    }
}
