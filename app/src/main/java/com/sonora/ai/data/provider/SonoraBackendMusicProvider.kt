package com.sonora.ai.data.provider

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.sonora.ai.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class BackendTrackDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("album") val album: String?,
    @SerializedName("artworkUrl") val artworkUrl: String?,
    @SerializedName("streamUrl") val streamUrl: String,
    @SerializedName("durationMs") val durationMs: Long,
    @SerializedName("sourceName") val sourceName: String?,
    @SerializedName("isPreview") val isPreview: Boolean = false,
    @SerializedName("genre") val genre: String?,
    @SerializedName("releaseDate") val releaseDate: String?
)

class SonoraBackendMusicProvider(
    private val hostCandidates: List<String> = listOf(
        "http://192.168.1.23:8080",
        "http://10.0.2.2:8080"
    ),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) : MusicProvider {

    override val name: String = "SONORA AI Full-Song Engine"

    override suspend fun search(query: String, limit: Int): Result<List<Track>> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        var lastError: Exception? = null

        for (host in hostCandidates) {
            try {
                val url = "$host/api/search?q=$encodedQuery&limit=$limit"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "SonoraAI-Android/1.4")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val dtoArray = gson.fromJson(body, Array<BackendTrackDto>::class.java)
                    val tracks = dtoArray.map { dto ->
                        Track(
                            id = dto.id,
                            title = dto.title,
                            artist = dto.artist,
                            album = dto.album ?: "Full Song Release",
                            artworkUrl = dto.artworkUrl ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600",
                            streamUrl = dto.streamUrl,
                            durationMs = dto.durationMs,
                            sourceName = dto.sourceName ?: "Full Song Stream",
                            isPreview = dto.isPreview,
                            genre = dto.genre ?: "Soundtrack",
                            releaseDate = dto.releaseDate ?: ""
                        )
                    }
                    if (tracks.isNotEmpty()) {
                        return@withContext Result.success(tracks)
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        Result.failure(lastError ?: Exception("Unable to reach SONORA backend"))
    }

    override suspend fun getTrending(category: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        val encodedCat = URLEncoder.encode(category.trim(), "UTF-8")
        var lastError: Exception? = null

        for (host in hostCandidates) {
            try {
                val url = "$host/api/trending?category=$encodedCat"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "SonoraAI-Android/1.4")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val dtoArray = gson.fromJson(body, Array<BackendTrackDto>::class.java)
                    val tracks = dtoArray.map { dto ->
                        Track(
                            id = dto.id,
                            title = dto.title,
                            artist = dto.artist,
                            album = dto.album ?: "Full Song Release",
                            artworkUrl = dto.artworkUrl ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600",
                            streamUrl = dto.streamUrl,
                            durationMs = dto.durationMs,
                            sourceName = dto.sourceName ?: "Full Song Stream",
                            isPreview = dto.isPreview,
                            genre = dto.genre ?: "Soundtrack",
                            releaseDate = dto.releaseDate ?: ""
                        )
                    }
                    if (tracks.isNotEmpty()) {
                        return@withContext Result.success(tracks)
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        Result.failure(lastError ?: Exception("Unable to reach SONORA backend"))
    }
}
