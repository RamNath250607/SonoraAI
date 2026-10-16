package com.sonora.ai.data.model

import java.io.Serializable

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val sourceName: String = "Official Audio Stream",
    val isPreview: Boolean = false,
    val genre: String = "",
    val releaseDate: String = ""
) : Serializable {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
