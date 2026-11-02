package com.wallora.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.libraryDataStore by preferencesDataStore("wallora_library")

/** Persists saved, downloaded and recently viewed wallpaper metadata on-device. */
class UserLibraryStore(context: Context) {
    private val store: DataStore<Preferences> = context.libraryDataStore
    private val gson = Gson()
    private val type = object : TypeToken<List<Wallpaper>>() {}.type

    private val favoritesKey = stringPreferencesKey("favorites")
    private val downloadsKey = stringPreferencesKey("downloads")
    private val recentKey = stringPreferencesKey("recent")

    val favorites: Flow<List<Wallpaper>> = collection(favoritesKey)
    val downloads: Flow<List<Wallpaper>> = collection(downloadsKey)
    val recentlyViewed: Flow<List<Wallpaper>> = collection(recentKey)

    suspend fun toggleFavorite(wallpaper: Wallpaper) = store.edit { prefs ->
        val current = decode(prefs[favoritesKey])
        val next = if (current.any { it.id == wallpaper.id }) current.filterNot { it.id == wallpaper.id }
        else listOf(wallpaper) + current
        prefs[favoritesKey] = gson.toJson(next, type)
    }

    suspend fun addDownload(wallpaper: Wallpaper) = putFirst(downloadsKey, wallpaper, limit = 500)
    suspend fun addRecent(wallpaper: Wallpaper) = putFirst(recentKey, wallpaper, limit = 40)

    private fun collection(key: Preferences.Key<String>): Flow<List<Wallpaper>> =
        store.data.map { decode(it[key]) }

    private fun decode(value: String?): List<Wallpaper> = value?.let {
        runCatching { gson.fromJson<List<Wallpaper>>(it, type).orEmpty() }.getOrDefault(emptyList())
    }.orEmpty()

    private suspend fun putFirst(key: Preferences.Key<String>, wallpaper: Wallpaper, limit: Int) = store.edit { prefs ->
        val next = (listOf(wallpaper) + decode(prefs[key]).filterNot { it.id == wallpaper.id }).take(limit)
        prefs[key] = gson.toJson(next, type)
    }
}
