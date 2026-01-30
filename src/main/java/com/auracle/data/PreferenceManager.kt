package com.auracle.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferenceManager(private val context: Context) {
    private val FOLDER_URI_KEY = stringPreferencesKey("folder_uri")
    private val LAST_AUDIOBOOK_ID = stringPreferencesKey("last_audiobook_id")

    val folderUri: Flow<String?> = context.dataStore.data.map { it[FOLDER_URI_KEY] }
    val lastAudiobookId: Flow<String?> = context.dataStore.data.map { it[LAST_AUDIOBOOK_ID] }

    fun getBookProgress(id: String): Flow<Pair<Int, Long>> = context.dataStore.data.map { prefs ->
        val chapter = prefs[intPreferencesKey("chapter_$id")] ?: 0
        val position = prefs[longPreferencesKey("position_$id")] ?: 0L
        Pair(chapter, position)
    }

    suspend fun setFolderUri(uri: String) {
        context.dataStore.edit { it[FOLDER_URI_KEY] = uri }
    }

    suspend fun savePlaybackState(id: String, chapterIndex: Int, position: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_AUDIOBOOK_ID] = id
            // Save global last state
            preferences[intPreferencesKey("last_chapter")] = chapterIndex
            preferences[longPreferencesKey("last_position")] = position
            
            // Save specific book state
            preferences[intPreferencesKey("chapter_$id")] = chapterIndex
            preferences[longPreferencesKey("position_$id")] = position
        }
    }
    
    fun getLastPlaybackState(): Flow<Pair<Int, Long>> = context.dataStore.data.map { prefs ->
        val chapter = prefs[intPreferencesKey("last_chapter")] ?: 0
        val position = prefs[longPreferencesKey("last_position")] ?: 0L
        Pair(chapter, position)
    }
}
