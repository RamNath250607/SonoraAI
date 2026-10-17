package com.sonora.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonora.ai.theme.SonoraBackground
import com.sonora.ai.theme.SonoraCard
import com.sonora.ai.theme.SonoraCardBorder
import com.sonora.ai.theme.SonoraCyan
import com.sonora.ai.theme.SonoraPurple
import com.sonora.ai.theme.SonoraTextPrimary
import com.sonora.ai.theme.SonoraTextSecondary
import com.sonora.ai.theme.SonoraTextTertiary

@Composable
fun SettingsScreen(
    voiceLanguage: String,
    autoPlayNext: Boolean,
    onVoiceLanguageChange: (String) -> Unit,
    onAutoPlayNextChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SonoraBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 100.dp)
    ) {
        Text(
            text = "Settings",
            color = SonoraTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Preferences & App Information",
            color = SonoraTextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Voice Command Language Section
        SettingsCard(title = "Voice Input Language", icon = Icons.Default.Language) {
            val languages = listOf(
                "auto" to "Auto-Detect (System Default)",
                "ta-in" to "Tamil (தமிழ் / Tanglish)",
                "en-in" to "English (India / Global)"
            )

            languages.forEach { (code, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVoiceLanguageChange(code) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = voiceLanguage.equals(code, ignoreCase = true),
                        onClick = { onVoiceLanguageChange(code) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = SonoraCyan,
                            unselectedColor = SonoraTextTertiary
                        )
                    )
                    Text(
                        text = label,
                        color = SonoraTextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playback Preferences
        SettingsCard(title = "Audio Playback", icon = Icons.Default.GraphicEq) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-play Next Track",
                        color = SonoraTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Automatically continue playback with the next track in queue",
                        color = SonoraTextTertiary,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = autoPlayNext,
                    onCheckedChange = onAutoPlayNextChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SonoraCyan,
                        checkedTrackColor = SonoraPurple
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy & Transparency Notice
        SettingsCard(title = "Privacy & Permissions", icon = Icons.Default.Security) {
            Text(
                text = "• Microphone: Used strictly when you tap the voice button to recognize speech into search & playback commands. Audio is never stored or recorded.\n\n• Legal Playback: Ram Beats streams authorized full songs and open-catalog audio directly via Media3 ExoPlayer without bypassing DRM or scraping protected sources.",
                color = SonoraTextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Card
        SettingsCard(title = "About Ram Beats", icon = Icons.Default.Info) {
            Text(
                text = "Ram Beats v2.4\nBuilt with Kotlin, Jetpack Compose, Material 3, and Media3 ExoPlayer.",
                color = SonoraTextTertiary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val context = LocalContext.current
        SettingsCard(title = "App Updates & Live Sync", icon = Icons.Default.SystemUpdate) {
            Text(
                text = "• Version: v2.2 (Build 4)\n• Auto-Sync: Music, trending tracks, & radio update live from server\n• Direct Update: Tap below to update APK without searching files",
                color = SonoraTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://192.168.1.23:8080/api/download-apk"))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SonoraCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = SonoraBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Update Ram Beats on Phone", color = SonoraBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SonoraCardBorder, RoundedCornerShape(16.dp)),
        color = SonoraCard,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SonoraCyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    color = SonoraTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
