package com.sonora.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sonora.ai.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StorageManager(context: Context) {
    private val prefs: SharedPreferences = run {
        val newPrefs = context.getSharedPreferences("ram_beats_prefs", Context.MODE_PRIVATE)
        val oldPrefs = context.getSharedPreferences("sonora_ai_prefs", Context.MODE_PRIVATE)
        if (oldPrefs.all.isNotEmpty() && newPrefs.all.isEmpty()) {
            val editor = newPrefs.edit()
            oldPrefs.all.forEach { (k, v) ->
                when (v) {
                    is String -> editor.putString(k, v)
                    is Boolean -> editor.putBoolean(k, v)
                    is Int -> editor.putInt(k, v)
                    is Long -> editor.putLong(k, v)
                    is Float -> editor.putFloat(k, v)
                }
            }
            editor.apply()
        }
        newPrefs
    }
    private val gson = Gson()

    private val _favoritesFlow = MutableStateFlow<List<Track>>(emptyList())
    val favoritesFlow: StateFlow<List<Track>> = _favoritesFlow.asStateFlow()

    private val _historyFlow = MutableStateFlow<List<String>>(emptyList())
    val historyFlow: StateFlow<List<String>> = _historyFlow.asStateFlow()

    init {
        loadFavorites()
        loadHistory()
    }

    private fun loadFavorites() {
        val json = prefs.getString(KEY_FAVORITES, null)
        if (!json.isNullOrBlank()) {
            val type = object : TypeToken<List<Track>>() {}.type
            try {
                val list: List<Track> = gson.fromJson(json, type) ?: emptyList()
                _favoritesFlow.value = list
            } catch (e: Exception) {
                _favoritesFlow.value = emptyList()
            }
        }
    }

    fun toggleFavorite(track: Track): Boolean {
        val current = _favoritesFlow.value.toMutableList()
        val exists = current.any { it.id == track.id }
        if (exists) {
            current.removeAll { it.id == track.id }
        } else {
            current.add(0, track)
        }
        _favoritesFlow.value = current
        prefs.edit().putString(KEY_FAVORITES, gson.toJson(current)).apply()
        return !exists
    }

    fun isFavorite(trackId: String): Boolean {
        return _favoritesFlow.value.any { it.id == trackId }
    }

    private fun loadHistory() {
        val json = prefs.getString(KEY_SEARCH_HISTORY, null)
        if (!json.isNullOrBlank()) {
            val type = object : TypeToken<List<String>>() {}.type
            try {
                val list: List<String> = gson.fromJson(json, type) ?: emptyList()
                _historyFlow.value = list
            } catch (e: Exception) {
                _historyFlow.value = emptyList()
            }
        }
    }

    fun addSearchQuery(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        val current = _historyFlow.value.toMutableList()
        current.removeAll { it.equals(clean, ignoreCase = true) }
        current.add(0, clean)
        val trimmed = if (current.size > 30) current.take(30) else current
        _historyFlow.value = trimmed
        prefs.edit().putString(KEY_SEARCH_HISTORY, gson.toJson(trimmed)).apply()
    }

    fun removeSearchQuery(query: String) {
        val current = _historyFlow.value.toMutableList()
        current.removeAll { it.equals(query, ignoreCase = true) }
        _historyFlow.value = current
        prefs.edit().putString(KEY_SEARCH_HISTORY, gson.toJson(current)).apply()
    }

    fun clearSearchHistory() {
        _historyFlow.value = emptyList()
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    // App Preferences
    var voiceLanguage: String
        get() = prefs.getString(KEY_VOICE_LANG, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_VOICE_LANG, value).apply()

    var autoPlayNext: Boolean
        get() = prefs.getBoolean(KEY_AUTOPLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTOPLAY, value).apply()

    companion object {
        private const val KEY_FAVORITES = "pref_favorites"
        private const val KEY_SEARCH_HISTORY = "pref_search_history"
        private const val KEY_VOICE_LANG = "pref_voice_language"
        private const val KEY_AUTOPLAY = "pref_auto_play"
    }
}
