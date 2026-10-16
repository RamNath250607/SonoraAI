package com.sonora.ai.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sonora.ai.theme.SonoraBackground
import com.sonora.ai.theme.SonoraCard
import com.sonora.ai.theme.SonoraCyan
import com.sonora.ai.theme.SonoraError
import com.sonora.ai.theme.SonoraPurple
import com.sonora.ai.theme.SonoraPurpleDark
import com.sonora.ai.theme.SonoraTextPrimary
import com.sonora.ai.theme.SonoraTextSecondary
import com.sonora.ai.theme.SonoraTextTertiary
import com.sonora.ai.voice.SonoraCommand
import com.sonora.ai.voice.VoiceState

@Composable
fun VoiceListeningModal(
    voiceState: VoiceState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onCommandDetected: (SonoraCommand) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, SonoraPurple.copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
            color = SonoraBackground,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SonoraTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Animated Mic Rings
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (voiceState is VoiceState.Listening) {
                        // Outer Pulsing Ring
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(SonoraCyan.copy(alpha = 0.35f), Color.Transparent)
                                    )
                                )
                        )
                        // Middle Ring
                        Box(
                            modifier = Modifier
                                .size(105.dp)
                                .clip(CircleShape)
                                .border(2.dp, SonoraPurple.copy(alpha = 0.6f), CircleShape)
                        )
                    }

                    // Center Mic Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(SonoraPurple, SonoraCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (voiceState is VoiceState.Processing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice State Feedback
                when (voiceState) {
                    is VoiceState.Listening -> {
                        Text(
                            text = "Listening...",
                            color = SonoraCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Say: \"Play Jana Nayagan\"\n\"Play Anirudh songs\" or \"Pause\"",
                            color = SonoraTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    is VoiceState.Processing -> {
                        Text(
                            text = "Processing command...",
                            color = SonoraPurple,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is VoiceState.Success -> {
                        Text(
                            text = "\"${voiceState.spokenText}\"",
                            color = SonoraTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        // Parsed Action preview
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SonoraCard)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Action: ${formatCommand(voiceState.command)}",
                                color = SonoraCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Auto execute command or provide tap button
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onCommandDetected(voiceState.command) },
                            colors = ButtonDefaults.buttonColors(containerColor = SonoraPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Execute Command", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    is VoiceState.Error -> {
                        Text(
                            text = "Couldn't catch that",
                            color = SonoraError,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = voiceState.message,
                            color = SonoraTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = SonoraPurpleDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Try Again", color = Color.White)
                        }
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Supports English, Tamil, and Tanglish",
                    color = SonoraTextTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun formatCommand(command: SonoraCommand): String {
    return when (command) {
        is SonoraCommand.Play -> "Play \"${command.query}\""
        is SonoraCommand.Pause -> "Pause playback"
        is SonoraCommand.Resume -> "Resume playback"
        is SonoraCommand.Next -> "Next song"
        is SonoraCommand.Previous -> "Previous song"
        is SonoraCommand.ToggleShuffle -> "Toggle shuffle"
        is SonoraCommand.ToggleRepeat -> "Toggle repeat"
        is SonoraCommand.FavoriteCurrent -> "Add to favorites"
        is SonoraCommand.SearchOnly -> "Search \"${command.query}\""
        is SonoraCommand.ShowHistory -> "Show search history"
        is SonoraCommand.ShowFavorites -> "Show favorites"
        is SonoraCommand.Unknown -> "Search \"${command.rawText}\""
    }
}
