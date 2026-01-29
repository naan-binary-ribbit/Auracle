package com.auracle.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.*

class AudiobookRepository(private val context: Context) {
    private val scanner = AudiobookScanner(context)
    private val cacheFile = File(context.filesDir, "audiobooks_cache.dat")

    fun getAudiobooks(folderUri: Uri, forceRefresh: Boolean = false): Flow<List<Audiobook>> = flow {
        // 1. Emit cached results if they exist
        val cached = if (!forceRefresh) loadFromCache() else null
        if (cached != null) {
            emit(cached)
        }

        // 2. Scan for new files
        try {
            val fresh = scanner.scanFolder(folderUri)
            saveToCache(fresh)
            emit(fresh)
        } catch (e: Exception) {
            // If scanning fails, we already emitted cached. Just stop.
        }
    }

    private fun saveToCache(audiobooks: List<Audiobook>) {
        try {
            ObjectOutputStream(FileOutputStream(cacheFile)).use { it.writeObject(audiobooks) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadFromCache(): List<Audiobook>? {
        if (!cacheFile.exists()) return null
        return try {
            ObjectInputStream(FileInputStream(cacheFile)).use { it.readObject() as List<Audiobook> }
        } catch (e: Exception) {
            cacheFile.delete()
            null
        }
    }
}
