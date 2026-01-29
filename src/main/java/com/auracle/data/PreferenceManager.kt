package com.auracle.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferenceManager(private val context: Context) {
    private val FOLDER_URI_KEY = stringPreferencesKey("folder_uri")

    val folderUri: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[FOLDER_URI_KEY]
        }

    suspend fun setFolderUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[FOLDER_URI_KEY] = uri
        }
    }
}
