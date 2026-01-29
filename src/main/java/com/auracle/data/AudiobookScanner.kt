package com.auracle.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

data class Audiobook(
    val id: String,
    val title: String,
    val author: String?,
    val coverArt: ByteArray?,
    val folderUri: Uri
)

class AudiobookScanner(private val context: Context) {

    fun scanFolder(folderUri: Uri): List<Audiobook> {
        val rootDoc = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()
        val audiobooks = mutableListOf<Audiobook>()

        rootDoc.listFiles().forEach { subFolder ->
            if (subFolder.isDirectory) {
                val firstAudioFile = findFirstAudioFile(subFolder)
                if (firstAudioFile != null) {
                    val metadata = getMetadata(firstAudioFile.uri)
                    audiobooks.add(
                        Audiobook(
                            id = subFolder.name ?: "",
                            title = metadata.title ?: subFolder.name ?: "Unknown Title",
                            author = metadata.author,
                            coverArt = metadata.coverArt,
                            folderUri = subFolder.uri
                        )
                    )
                }
            }
        }
        return audiobooks
    }

    private fun findFirstAudioFile(folder: DocumentFile): DocumentFile? {
        return folder.listFiles().find { file ->
            val type = file.type ?: ""
            type.startsWith("audio/") || file.name?.endsWith(".mp3") == true || 
            file.name?.endsWith(".m4a") == true || file.name?.endsWith(".m4b") == true
        }
    }

    private data class BasicMetadata(val title: String?, val author: String?, val coverArt: ByteArray?)

    private fun getMetadata(fileUri: Uri): BasicMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            context.contentResolver.openFileDescriptor(fileUri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val author = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?:
                             retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
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
