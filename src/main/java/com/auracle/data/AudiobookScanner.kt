package com.auracle.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.*
import java.io.Serializable

data class Audiobook(
    val id: String,
    val title: String,
    val author: String?,
    val coverArt: ByteArray?,
    val folderUri: String,
    val audioFiles: List<AudioFile> = emptyList(),
    val duration: Long = 0L,
    val isNew: Boolean = false
) : Serializable

data class AudioFile(
    val name: String,
    val uri: String,
    val duration: Long
) : Serializable

data class Chapter(
    val title: String,
    val startPosition: Long,
    val endPosition: Long,
    val fileUri: String // The file this chapter belongs to
) : Serializable

class AudiobookScanner(private val context: Context) {

    suspend fun scanFolder(folderUri: Uri): List<Audiobook> = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
        val audiobookFolders = mutableListOf<DocumentFile>()
        
        // Step 1: Find all folders that contain audio files
        findAudiobookFolders(rootDoc, audiobookFolders)

        // Step 2: Extract metadata in parallel
        audiobookFolders.map { folder ->
            async {
                val files = folder.listFiles()
                    .filter { isAudioFile(it) }
                    .sortedBy { it.name }
                    .map { file ->
                        val duration = getFileDuration(file.uri)
                        AudioFile(file.name ?: "Unknown", file.uri.toString(), duration)
                    }

                val firstAudioFile = files.firstOrNull()
                val metadata = if (firstAudioFile != null) {
                    getMetadata(Uri.parse(firstAudioFile.uri))
                } else {
                    BasicMetadata(null, null, null)
                }
                
                Audiobook(
                    id = folder.uri.toString(),
                    title = metadata.title ?: folder.name ?: "Unknown Title",
                    author = metadata.author,
                    coverArt = metadata.coverArt,
                    folderUri = folder.uri.toString(),
                    audioFiles = files,
                    duration = files.sumOf { it.duration }
                )
            }
        }.awaitAll()
    }

    private fun getFileDuration(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    private fun findAudiobookFolders(folder: DocumentFile, result: MutableList<DocumentFile>) {
        if (containsAudioFiles(folder)) {
            result.add(folder)
        }
        
        // Even if the current folder has audio files, it might have subfolders with OTHER audiobooks
        // So we scan subfolders regardless.
        folder.listFiles().forEach { subFile ->
            if (subFile.isDirectory) {
                findAudiobookFolders(subFile, result)
            }
        }
    }

    private fun containsAudioFiles(folder: DocumentFile): Boolean {
        return folder.listFiles().any { isAudioFile(it) }
    }

    private fun isAudioFile(file: DocumentFile): Boolean {
        val name = file.name?.lowercase() ?: ""
        val type = file.type?.lowercase() ?: ""
        return type.startsWith("audio/") || 
               name.endsWith(".mp3") || 
               name.endsWith(".m4a") || 
               name.endsWith(".m4b") ||
               name.endsWith(".aac") ||
               name.endsWith(".wav") ||
               name.endsWith(".flac")
    }

    private fun findFirstAudioFile(folder: DocumentFile): DocumentFile? {
        return folder.listFiles().find { isAudioFile(it) }
    }

    private data class BasicMetadata(val title: String?, val author: String?, val coverArt: ByteArray?)

    private fun getMetadata(fileUri: Uri): BasicMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            context.contentResolver.openFileDescriptor(fileUri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val author = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?:
                             retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST) ?:
                             retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                val coverArt = retriever.embeddedPicture
                BasicMetadata(title, author, coverArt)
            } ?: BasicMetadata(null, null, null)
        } catch (e: Exception) {
            BasicMetadata(null, null, null)
        } finally {
            retriever.release()
        }
    }
}

