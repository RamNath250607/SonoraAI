package com.sonora.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonora.ai.player.PlayerState
import com.sonora.ai.theme.SonoraCard
import com.sonora.ai.theme.SonoraCardBorder
import com.sonora.ai.theme.SonoraCyan
import com.sonora.ai.theme.SonoraPurple
import com.sonora.ai.theme.SonoraTextPrimary
import com.sonora.ai.theme.SonoraTextSecondary

@Composable
fun MiniPlayerBar(
    playerState: PlayerState,
    onBarClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onBarClick),
        color = SonoraCard,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Tiny progress line across the top
            val progress = if (playerState.durationMs > 0) {
                (playerState.currentPositionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = SonoraCyan,
                trackColor = SonoraCardBorder
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Artwork
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SonoraCardBorder),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = if (track.artworkUrl.isNotBlank() && !track.artworkUrl.contains("unsplash") && track.artworkUrl != "/logo.png") track.artworkUrl else com.sonora.ai.R.drawable.logo,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Artist
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track.title,
                        color = SonoraTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = SonoraTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Play / Pause or Buffering Indicator
                if (playerState.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(24.dp),
                        strokeWidth = 2.dp,
                        color = SonoraCyan
                    )
                } else {
                    IconButton(onClick = onPlayPauseClick) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = SonoraCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Next Button
                IconButton(onClick = onNextClick) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = SonoraTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
