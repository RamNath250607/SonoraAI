package com.sonora.ai.data.repository

import com.sonora.ai.data.model.Track
import com.sonora.ai.data.provider.MusicProvider
import com.sonora.ai.data.provider.OpenAudioMusicProvider
import com.sonora.ai.data.provider.SonoraBackendMusicProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(
    private val providers: List<MusicProvider> = listOf(
        SonoraBackendMusicProvider(),
        OpenAudioMusicProvider()
    )
) {
    // Memory cache for quick back/forth search results
    private val searchCache = mutableMapOf<String, List<Track>>()

    suspend fun search(query: String, limit: Int = 25): Result<List<Track>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext Result.success(emptyList())

        searchCache[cleanQuery.lowercase()]?.let {
            if (it.isNotEmpty()) return@withContext Result.success(it)
        }

        val allResults = mutableListOf<Track>()
        var primaryError: Throwable? = null

        for (provider in providers) {
            val result = provider.search(cleanQuery, limit)
            if (result.isSuccess) {
                val tracks = result.getOrNull().orEmpty()
                if (tracks.isNotEmpty()) {
                    allResults.addAll(tracks)
                    break // Stop immediately once full songs are found!
                }
            } else if (primaryError == null) {
                primaryError = result.exceptionOrNull()
            }
        }

        if (allResults.isNotEmpty()) {
            val distinctTracks = allResults.distinctBy { it.id }
            searchCache[cleanQuery.lowercase()] = distinctTracks
            Result.success(distinctTracks)
        } else if (primaryError != null) {
            Result.failure(primaryError)
        } else {
            Result.success(emptyList())
        }
    }

    suspend fun getCuratedSection(category: String): List<Track> = withContext(Dispatchers.IO) {
        for (provider in providers) {
            val result = provider.getTrending(category)
            if (result.isSuccess && !result.getOrNull().isNullOrEmpty()) {
                return@withContext result.getOrNull().orEmpty()
            }
        }
        emptyList()
    }
}
