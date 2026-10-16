package com.sonora.ai.data.provider

import com.sonora.ai.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenAudioMusicProvider : MusicProvider {
    override val name: String = "Open License Audio Catalog"

    // High quality, open licensed / public domain full audio streams
    private val openCatalog = listOf(
        Track(
            id = "open_lofi_1",
            title = "Midnight Coffee Lo-Fi",
            artist = "Aesthetic Beats",
            album = "Chillhop Sessions Vol. 1",
            artworkUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3",
            durationMs = 147000L,
            sourceName = "Open Audio (Full Track)",
            isPreview = false,
            genre = "Lo-Fi"
        ),
        Track(
            id = "open_synth_2",
            title = "Cyberpunk Drive",
            artist = "Neon Dreamer",
            album = "Retro Wave Chronicles",
            artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=electronic-future-beats-117997.mp3",
            durationMs = 168000L,
            sourceName = "Open Audio (Full Track)",
            isPreview = false,
            genre = "Synthwave"
        ),
        Track(
            id = "open_acoustic_3",
            title = "Sunset Melodies",
            artist = "Acoustic Horizon",
            album = "Peaceful Acoustic",
            artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://cdn.pixabay.com/download/audio/2022/11/06/audio_c976d8b945.mp3?filename=acoustic-guitars-ambient-126216.mp3",
            durationMs = 135000L,
            sourceName = "Open Audio (Full Track)",
            isPreview = false,
            genre = "Acoustic"
        ),
        Track(
            id = "open_cinematic_4",
            title = "Epic Journey",
            artist = "Symphonic Echoes",
            album = "Cinematic Horizons",
            artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3?filename=cinematic-epic-112461.mp3",
            durationMs = 192000L,
            sourceName = "Open Audio (Full Track)",
            isPreview = false,
            genre = "Cinematic"
        )
    )

    override suspend fun search(query: String, limit: Int): Result<List<Track>> = withContext(Dispatchers.Default) {
        val q = query.trim().lowercase()
        val results = openCatalog.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.genre.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }.take(limit)
        Result.success(results)
    }

    override suspend fun getTrending(category: String): Result<List<Track>> = withContext(Dispatchers.Default) {
        Result.success(openCatalog)
    }
}
