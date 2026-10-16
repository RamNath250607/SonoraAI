package com.sonora.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun SearchScreen(
    searchQuery: String,
    searchResults: List<Track>,
    searchHistory: List<String>,
    isLoading: Boolean,
    errorMessage: String?,
    playerState: PlayerState,
    isFavorite: (String) -> Boolean,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onClearQuery: () -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onBack: () -> Unit,
    onMicClick: () -> Unit,
    onTrackPlay: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SonoraBackground)
    ) {
        // Search Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = SonoraTextPrimary
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                placeholder = {
                    Text(
                        "Search songs, artists, Tamil...",
                        color = SonoraTextTertiary,
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SonoraCyan,
                    unfocusedBorderColor = SonoraCardBorder,
                    focusedContainerColor = SonoraCard,
                    unfocusedContainerColor = SonoraCard,
                    focusedTextColor = SonoraTextPrimary,
                    unfocusedTextColor = SonoraTextPrimary,
                    cursorColor = SonoraCyan
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = SonoraCyan,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearQuery) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = SonoraTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(onClick = onMicClick) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice",
                                tint = SonoraPurple,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        onSearchSubmit(searchQuery)
                    }
                )
            )
        }

        // Recent searches history pills
        if (searchHistory.isNotEmpty() && searchResults.isEmpty() && !isLoading) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Searches",
                        color = SonoraTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchHistory) { historyItem ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(SonoraCard)
                                .border(1.dp, SonoraCardBorder, RoundedCornerShape(16.dp))
                                .clickable {
                                    onQueryChange(historyItem)
                                    onSearchSubmit(historyItem)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = SonoraTextTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = historyItem,
                                    color = SonoraTextPrimary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = SonoraTextTertiary,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onDeleteHistoryItem(historyItem) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Loading, Results, or Empty State
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SonoraCyan, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Searching music catalogs...", color = SonoraTextSecondary, fontSize = 13.sp)
                    }
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = SonoraTextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            searchResults.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Found ${searchResults.size} tracks",
                                color = SonoraTextTertiary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "All with in-app audio playback",
                                color = SonoraCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    items(searchResults, key = { it.id }) { track ->
                        TrackItemRow(
                            track = track,
                            isPlaying = playerState.isPlaying && playerState.currentTrack?.id == track.id,
                            isCurrentTrack = playerState.currentTrack?.id == track.id,
                            isFavorite = isFavorite(track.id),
                            onTrackClick = { onTrackPlay(track, searchResults) },
                            onFavoriteClick = { onToggleFavorite(track) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
            else -> {
                // Empty search guidance
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = SonoraPurple.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Search & Discover",
                            color = SonoraTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Type any song, movie, or singer name.\nOr tap the microphone and speak:\n\"Play Jana Nayagan song\"\n\"Play latest Anirudh songs\"",
                            color = SonoraTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
