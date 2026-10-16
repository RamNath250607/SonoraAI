package com.sonora.ai.data.provider

import com.sonora.ai.data.model.Track

interface MusicProvider {
    val name: String
    suspend fun search(query: String, limit: Int = 25): Result<List<Track>>
    suspend fun getTrending(category: String): Result<List<Track>>
}
