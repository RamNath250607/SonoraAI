package com.sonora.ai.voice

sealed class SonoraCommand {
    data class Play(val query: String) : SonoraCommand()
    data object Pause : SonoraCommand()
    data object Resume : SonoraCommand()
    data object Next : SonoraCommand()
    data object Previous : SonoraCommand()
    data object ToggleShuffle : SonoraCommand()
    data object ToggleRepeat : SonoraCommand()
    data object FavoriteCurrent : SonoraCommand()
    data class SearchOnly(val query: String) : SonoraCommand()
    data object ShowHistory : SonoraCommand()
    data object ShowFavorites : SonoraCommand()
    data class Unknown(val rawText: String) : SonoraCommand()
}

object VoiceCommandParser {

    fun parse(rawInput: String): SonoraCommand {
        val text = rawInput.trim().lowercase()
        if (text.isEmpty()) return SonoraCommand.Unknown(rawInput)

        // 1. Direct Control Commands (English, Tamil, Tanglish)
        // Pause / Stop
        if (text in listOf("pause", "stop", "pause song", "stop playback", "niruthu", "pause pannu", "stop pannu", "paatta niruthu")) {
            return SonoraCommand.Pause
        }

        // Resume / Continue
        if (text in listOf("resume", "continue", "play again", "unpause", "marupadiyum play pannu", "thodaru")) {
            return SonoraCommand.Resume
        }

        // Next / Skip
        if (text in listOf("next", "next song", "skip", "skip song", "adutha paatu", "aduthathu", "skip pannu")) {
            return SonoraCommand.Next
        }

        // Previous / Back
        if (text in listOf("previous", "previous song", "prev", "back", "munthaiya paatu", "pinnadi po")) {
            return SonoraCommand.Previous
        }

        // Shuffle
        if (text.contains("shuffle") || text.contains("kalandhu podu")) {
            return SonoraCommand.ToggleShuffle
        }

        // Repeat
        if (text.contains("repeat") || text.contains("loop") || text.contains("marubadi")) {
            return SonoraCommand.ToggleRepeat
        }

        // Favorite / Like
        if (text.contains("favorite") || text.contains("favourite") || text.contains("add to favorite") ||
            text.contains("like this song") || text.contains("save this") || text.contains("pidichirukku")) {
            return SonoraCommand.FavoriteCurrent
        }

        // History
        if (text.contains("history") || text.contains("recent searches") || text.contains("searched songs")) {
            return SonoraCommand.ShowHistory
        }

        // Favorites screen
        if (text.contains("show favorites") || text.contains("my favorites") || text.contains("liked songs")) {
            return SonoraCommand.ShowFavorites
        }

        // 2. Play Commands
        // "Play <Song/Artist/Movie>"
        val playPrefixes = listOf(
            "play song", "play songs", "play track", "play the song", "play me", "play",
            "paatu podu", "song podu", "play pannu"
        )

        for (prefix in playPrefixes) {
            if (text.startsWith(prefix)) {
                var query = text.removePrefix(prefix).trim()
                query = cleanQuery(query)
                if (query.isNotEmpty()) {
                    return SonoraCommand.Play(query)
                }
            }
        }

        // Suffix patterns: "<Song name> play pannu" or "<Song name> paatu podu"
        val playSuffixes = listOf(
            "play pannu", "paatu podu", "paata podu", "poda sollu", "play"
        )
        for (suffix in playSuffixes) {
            if (text.endsWith(suffix) && text.length > suffix.length) {
                var query = text.removeSuffix(suffix).trim()
                query = cleanQuery(query)
                if (query.isNotEmpty()) {
                    return SonoraCommand.Play(query)
                }
            }
        }

        // Search Commands ("find...", "search for...")
        val searchPrefixes = listOf(
            "search for", "search", "find songs by", "find songs of", "find song", "find", "thedu", "kandupidi"
        )
        for (prefix in searchPrefixes) {
            if (text.startsWith(prefix)) {
                var query = text.removePrefix(prefix).trim()
                query = cleanQuery(query)
                if (query.isNotEmpty()) {
                    return SonoraCommand.SearchOnly(query)
                }
            }
        }

        // Fallback: If user spoke or typed a title/artist without prefix, treat as Play/Search
        val fallbackQuery = cleanQuery(text)
        return if (fallbackQuery.length > 1) {
            SonoraCommand.Play(fallbackQuery)
        } else {
            SonoraCommand.Unknown(rawInput)
        }
    }

    private fun cleanQuery(query: String): String {
        return query
            .replace(Regex("^(the|a|an)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\b(songs|song|tracks|track|paatu|paadal)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
