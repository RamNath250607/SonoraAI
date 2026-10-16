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

class ItunesMusicProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) : MusicProvider {

    override val name: String = "Official Music Catalog"

    override suspend fun search(query: String, limit: Int): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=$limit"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SonoraAI/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Search failed with HTTP ${response.code}"))
            }

            val responseBody = response.body?.string() ?: ""
            val itunesResponse = gson.fromJson(responseBody, ItunesSearchResponse::class.java)

            val tracks = itunesResponse.results
                .filter { !it.previewUrl.isNullOrBlank() }
                .map { item ->
                    val highResArtwork = item.artworkUrl100
                        ?.replace("100x100bb.jpg", "600x600bb.jpg")
                        ?.replace("100x100bb.png", "600x600bb.png")
                        ?: ""

                    Track(
                        id = "itunes_${item.trackId}",
                        title = item.trackName ?: "Unknown Track",
                        artist = item.artistName ?: "Unknown Artist",
                        album = item.collectionName ?: item.trackName ?: "Single",
                        artworkUrl = highResArtwork,
                        streamUrl = item.previewUrl ?: "",
                        durationMs = 30000L, // iTunes preview duration is ~30s
                        sourceName = "Official Audio Preview",
                        isPreview = true,
                        genre = item.primaryGenreName ?: "",
                        releaseDate = item.releaseDate ?: ""
                    )
                }

            Result.success(tracks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTrending(category: String): Result<List<Track>> {
        val query = when (category.lowercase()) {
            "tamil", "tamil hits" -> "Anirudh Ravichander Tamil hits"
            "thalapathy", "vijay" -> "Thalapathy Vijay Tamil"
            "romantic", "melody" -> "AR Rahman romantic Tamil"
            "anirudh" -> "Anirudh Ravichander"
            "global", "trending" -> "Top Hits 2024"
            "lo-fi", "lofi" -> "Lofi chill beats"
            else -> category
        }
        return search(query, limit = 20)
    }

    private data class ItunesSearchResponse(
        @SerializedName("resultCount") val resultCount: Int = 0,
        @SerializedName("results") val results: List<ItunesTrackDto> = emptyList()
    )

    private data class ItunesTrackDto(
        @SerializedName("trackId") val trackId: Long = 0,
        @SerializedName("trackName") val trackName: String? = null,
        @SerializedName("artistName") val artistName: String? = null,
        @SerializedName("collectionName") val collectionName: String? = null,
        @SerializedName("artworkUrl100") val artworkUrl100: String? = null,
        @SerializedName("previewUrl") val previewUrl: String? = null,
        @SerializedName("trackTimeMillis") val trackTimeMillis: Long? = null,
        @SerializedName("primaryGenreName") val primaryGenreName: String? = null,
        @SerializedName("releaseDate") val releaseDate: String? = null
    )
}
