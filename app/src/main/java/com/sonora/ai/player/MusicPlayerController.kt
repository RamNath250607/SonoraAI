package com.sonora.ai.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sonora.ai.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = -1,
    val isShuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val errorMessage: String? = null
)

class MusicPlayerController private constructor(context: Context) {

    private val applicationContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val player: ExoPlayer = ExoPlayer.Builder(applicationContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var progressJob: Job? = null

    init {
        setupPlayerListener()
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _playerState.update { it.copy(isBuffering = true) }
                    }
                    Player.STATE_READY -> {
                        _playerState.update {
                            it.copy(
                                isBuffering = false,
                                durationMs = if (player.duration > 0) player.duration else (it.currentTrack?.durationMs ?: 0L)
                            )
                        }
                    }
                    Player.STATE_ENDED -> {
                        _playerState.update { it.copy(isBuffering = false, isPlaying = false, currentPositionMs = it.durationMs) }
                        handleTrackEnded()
                    }
                    Player.STATE_IDLE -> {
                        _playerState.update { it.copy(isBuffering = false) }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val errorMsg = "Playback error: ${error.localizedMessage ?: "Cannot stream track"}"
                _playerState.update { it.copy(isPlaying = false, isBuffering = false, errorMessage = errorMsg) }
            }
        })
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = if (player.duration > 0) player.duration else (_playerState.value.currentTrack?.durationMs ?: 0L)
                    _playerState.update { it.copy(currentPositionMs = pos, durationMs = dur) }
                }
                delay(400)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        val index = queue.indexOfFirst { it.id == track.id }.let { if (it >= 0) it else 0 }
        _playerState.update {
            it.copy(
                currentTrack = track,
                queue = queue,
                queueIndex = index,
                errorMessage = null,
                currentPositionMs = 0L,
                durationMs = track.durationMs
            )
        }

        try {
            val metadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setArtworkUri(if (track.artworkUrl.isNotBlank()) Uri.parse(track.artworkUrl) else null)
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.streamUrl)
                .setMediaMetadata(metadata)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } catch (e: Exception) {
            _playerState.update { it.copy(errorMessage = "Could not initialize track: ${e.message}") }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE && _playerState.value.currentTrack != null) {
                _playerState.value.currentTrack?.let { playTrack(it, _playerState.value.queue) }
            } else {
                player.play()
            }
        }
    }

    fun pause() {
        if (player.isPlaying) {
            player.pause()
        }
    }

    fun resume() {
        if (!player.isPlaying) {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _playerState.value.durationMs)
        player.seekTo(clamped)
        _playerState.update { it.copy(currentPositionMs = clamped) }
    }

    fun skipNext() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return
        val nextIndex = if (state.isShuffle) {
            state.queue.indices.random()
        } else {
            (state.queueIndex + 1) % state.queue.size
        }
        playTrack(state.queue[nextIndex], state.queue)
    }

    fun skipPrevious() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return
        // If passed 3 seconds into the song, restart song; else go to prev track
        if (player.currentPosition > 3000L) {
            seekTo(0L)
            return
        }
        val prevIndex = if (state.queueIndex - 1 < 0) state.queue.size - 1 else state.queueIndex - 1
        playTrack(state.queue[prevIndex], state.queue)
    }

    fun toggleShuffle() {
        val newShuffle = !_playerState.value.isShuffle
        _playerState.update { it.copy(isShuffle = newShuffle) }
    }

    fun toggleRepeat() {
        val nextMode = when (_playerState.value.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = nextMode
        _playerState.update { it.copy(repeatMode = nextMode) }
    }

    private fun handleTrackEnded() {
        val state = _playerState.value
        when (state.repeatMode) {
            Player.REPEAT_MODE_ONE -> {
                seekTo(0L)
                player.play()
            }
            Player.REPEAT_MODE_ALL -> {
                skipNext()
            }
            Player.REPEAT_MODE_OFF -> {
                if (state.queueIndex < state.queue.size - 1) {
                    skipNext()
                }
            }
        }
    }

    fun release() {
        stopProgressTracker()
        player.release()
    }

    companion object {
        @Volatile
        private var instance: MusicPlayerController? = null

        fun getInstance(context: Context): MusicPlayerController {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayerController(context).also { instance = it }
            }
        }
    }
}
