package com.sonora.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.sonora.ai.data.model.Track
import com.sonora.ai.player.PlayerState
import com.sonora.ai.theme.SonoraBackground
import com.sonora.ai.theme.SonoraCard
import com.sonora.ai.theme.SonoraCardBorder
import com.sonora.ai.theme.SonoraCyan
import com.sonora.ai.theme.SonoraPurple
import com.sonora.ai.theme.SonoraTextPrimary
import com.sonora.ai.theme.SonoraTextSecondary
import com.sonora.ai.theme.SonoraTextTertiary
import com.sonora.ai.ui.components.TrackItemRow

@Composable
fun NowPlayingScreen(
    playerState: PlayerState,
    isFavorite: Boolean,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onTrackSelect: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return

    var isQueueVisible by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SonoraBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = SonoraTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = if (isQueueVisible) "UP NEXT QUEUE" else "NOW PLAYING",
                    color = SonoraTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                IconButton(onClick = { isQueueVisible = !isQueueVisible }) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (isQueueVisible) SonoraCyan else SonoraTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content: either Album Player view or Queue list
            if (isQueueVisible) {
                QueueView(
                    queue = playerState.queue,
                    currentTrack = playerState.currentTrack,
                    isPlaying = playerState.isPlaying,
                    onTrackSelect = onTrackSelect,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.weight(1f)
                )
            } else {
                PlayerView(
                    track = track,
                    playerState = playerState,
                    isFavorite = isFavorite,
                    isSeeking = isSeeking,
                    seekPosition = seekPosition,
                    onSeekingChange = { seeking, pos ->
                        isSeeking = seeking
                        seekPosition = pos
                    },
                    onSeek = onSeek,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat,
                    onToggleFavorite = { onToggleFavorite(track) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PlayerView(
    track: Track,
    playerState: PlayerState,
    isFavorite: Boolean,
    isSeeking: Boolean,
    seekPosition: Float,
    onSeekingChange: (Boolean, Float) -> Unit,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Big Album Artwork Card with Neon Glow
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, SonoraPurple.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .background(SonoraCard),
            contentAlignment = Alignment.Center
        ) {
            if (track.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = SonoraPurple,
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Track Information
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = SonoraTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${track.artist} • ${track.album}",
                    color = SonoraTextSecondary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (track.isPreview) "OFFICIAL AUDIO PREVIEW" else "FULL TRACK",
                        color = SonoraCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(SonoraCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "AAC • 256 kbps",
                        color = SonoraTextTertiary,
                        fontSize = 10.sp
                    )
                }
            }

            // Favorite Button
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else SonoraTextSecondary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Slider
        val currentMs = if (isSeeking) seekPosition.toLong() else playerState.currentPositionMs
        val totalMs = playerState.durationMs.coerceAtLeast(1L)
        val sliderValue = (currentMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = sliderValue,
                onValueChange = { frac ->
                    onSeekingChange(true, frac * totalMs)
                },
                onValueChangeFinished = {
                    onSeek(seekPosition.toLong())
                    onSeekingChange(false, 0f)
                },
                colors = SliderDefaults.colors(
                    thumbColor = SonoraCyan,
                    activeTrackColor = SonoraCyan,
                    inactiveTrackColor = SonoraCardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentMs),
                    color = SonoraTextTertiary,
                    fontSize = 12.sp
                )
                Text(
                    text = formatTime(totalMs),
                    color = SonoraTextTertiary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controls (Shuffle, Prev, Play/Pause, Next, Repeat)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (playerState.isShuffle) SonoraCyan else SonoraTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Skip Previous
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = SonoraTextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Central Play / Pause Button with Neon Glow Circle
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(SonoraPurple, SonoraCyan)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (playerState.isBuffering) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }

            // Skip Next
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = SonoraTextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Repeat Mode
            IconButton(onClick = onToggleRepeat) {
                val icon = if (playerState.repeatMode == Player.REPEAT_MODE_ONE) {
                    Icons.Default.RepeatOne
                } else {
                    Icons.Default.Repeat
                }
                val tint = if (playerState.repeatMode != Player.REPEAT_MODE_OFF) SonoraCyan else SonoraTextSecondary
                Icon(
                    imageVector = icon,
                    contentDescription = "Repeat",
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun QueueView(
    queue: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onTrackSelect: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    if (queue.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("Queue is empty", color = SonoraTextSecondary)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth()
        ) {
            itemsIndexed(queue, key = { _, item -> item.id }) { _, trackItem ->
                TrackItemRow(
                    track = trackItem,
                    isPlaying = isPlaying,
                    isCurrentTrack = trackItem.id == currentTrack?.id,
                    isFavorite = false,
                    onTrackClick = { onTrackSelect(trackItem) },
                    onFavoriteClick = { onToggleFavorite(trackItem) }
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
