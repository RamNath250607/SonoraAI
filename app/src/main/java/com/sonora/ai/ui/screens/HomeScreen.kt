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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun HomeScreen(
    featuredTracks: List<Track>,
    recommendedTracks: List<Track>,
    selectedCategory: String,
    isLoading: Boolean,
    playerState: PlayerState,
    isFavorite: (String) -> Boolean,
    onCategorySelect: (String) -> Unit,
    onSearchClick: () -> Unit,
    onMicClick: () -> Unit,
    onTrackPlay: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        "Tamil Hits",
        "Thalapathy",
        "Anirudh",
        "Romantic",
        "Trending",
        "Lo-Fi"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SonoraBackground),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // App Header with Glowing Logo & Mic Trigger
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.sonora.ai.R.drawable.logo),
                        contentDescription = "Ram Beats Logo",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, SonoraCyan, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RAM",
                                color = SonoraTextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "BEATS",
                                color = SonoraCyan,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = "Smart Voice & Full Music Player",
                            color = SonoraTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Glowing Voice Trigger Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(SonoraPurple, SonoraCyan)
                            )
                        )
                        .clickable(onClick = onMicClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Search",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Search Bar Trigger
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SonoraCard)
                    .border(1.dp, SonoraCardBorder, RoundedCornerShape(16.dp))
                    .clickable(onClick = onSearchClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = SonoraCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Try \"Play Jana Nayagan\" or \"Anirudh\"",
                        color = SonoraTextTertiary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Category Pills
        item {
            Spacer(modifier = Modifier.height(18.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category.equals(selectedCategory, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) SonoraPurple else SonoraCard)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) SonoraCyan else SonoraCardBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onCategorySelect(category) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color.White else SonoraTextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Featured Horizontal Cards
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Featured & Curated",
                color = SonoraTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading && featuredTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SonoraCyan, strokeWidth = 2.dp)
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(featuredTracks) { track ->
                        FeaturedCard(
                            track = track,
                            isPlaying = playerState.isPlaying && playerState.currentTrack?.id == track.id,
                            onPlay = { onTrackPlay(track, featuredTracks) }
                        )
                    }
                }
            }
        }

        // Recommended / Category List
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$selectedCategory Tracks",
                    color = SonoraTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${recommendedTracks.size} available",
                    color = SonoraTextTertiary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(recommendedTracks, key = { it.id }) { track ->
            TrackItemRow(
                track = track,
                isPlaying = playerState.isPlaying && playerState.currentTrack?.id == track.id,
                isCurrentTrack = playerState.currentTrack?.id == track.id,
                isFavorite = isFavorite(track.id),
                onTrackClick = { onTrackPlay(track, recommendedTracks) },
                onFavoriteClick = { onToggleFavorite(track) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun FeaturedCard(
    track: Track,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay),
        color = SonoraCard,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SonoraCardBorder),
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
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Play Button overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SonoraCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = track.title,
                color = SonoraTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
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
    }
}
