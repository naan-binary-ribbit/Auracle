package com.auracle.util

import android.content.Context
import android.net.Uri
import com.auracle.data.AudioFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class M4BChapterParser(private val context: Context) {

    companion object {
        private const val MAX_BOX_DATA_SIZE = 512 * 1024 // 512KB cap to avoid OOM on corrupt/large boxes
        // chpl box timestamps are in 100-nanosecond units (1/10_000_000 second), NOT mvhd timescale.
        // See: https://stackoverflow.com/questions/60377633/what-unit-is-the-mp4-chpl-boxs-timestamp-in
        private const val CHPL_UNITS_PER_MS = 10_000L // 1 ms = 10_000 chpl units (100ns each)
    }

    fun parseChapters(uri: Uri): List<AudioFile> {
        val chapters = mutableListOf<AudioFile>()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                java.io.FileInputStream(pfd.fileDescriptor).use { fis ->
                    val channel = fis.channel
                    val movieData = parseChannel(channel)
                    
                    val chplEntries = movieData.chplEntries
                    // chpl timestamps are in 100ns units; convert to ms: divide by 10_000
                    for (entry in chplEntries) {
                        val durationMs = (entry.duration / CHPL_UNITS_PER_MS).coerceAtLeast(0L)
                        chapters.add(
                            AudioFile(
                                name = entry.title.ifBlank { "Chapter ${chapters.size + 1}" },
                                uri = uri.toString(),
                                duration = durationMs
                            )
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            // Catch everything (OOM, IllegalArgumentException, etc.) so m4b playback never crashes
            e.printStackTrace()
        }
        return chapters
    }

    private fun parseChannel(channel: java.nio.channels.FileChannel): MovieData {
        var timescale = 0L
        var totalDuration = 0L
        val chplEntries = mutableListOf<ChapterEntry>()
        val header = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)

        var position = 0L
        val size_file = channel.size()

        while (position < size_file) {
            header.clear()
            if (channel.read(header, position) < 8) break
            header.flip()
            
            val size = header.int.toLong() and 0xFFFFFFFFL
            val type = ByteArray(4)
            header.get(type)
            val typeStr = String(type)

            if (size < 0) break
            val boxSize = if (size == 0L) size_file - position else size
            if (boxSize <= 0) break

            when (typeStr) {
                "moov", "udta", "meta", "ilst" -> {
                    var offset = 8L
                    if (typeStr == "meta") offset += 4
                    position += if (size == 0L) (size_file - position) else offset
                }
                "mvhd" -> {
                    val actualSize = when {
                        size == 1L -> {
                             val extHeader = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
                             channel.read(extHeader, position + 8)
                             extHeader.flip()
                             extHeader.long
                        }
                        size == 0L -> boxSize
                        else -> size
                    }
                    if (actualSize > 0 && actualSize <= Int.MAX_VALUE) {
                        val headerOffset = if (size == 1L) 16 else 8
                        val dataSize = (actualSize - headerOffset).toInt().coerceIn(0, MAX_BOX_DATA_SIZE)
                        if (dataSize > 0) {
                            val data = ByteBuffer.allocate(dataSize.coerceAtMost(1024)).order(ByteOrder.BIG_ENDIAN)
                            channel.read(data, position + headerOffset)
                            data.flip()
                            
                            if (data.remaining() >= 12) {
                                val version = data.get().toInt()
                                data.get(); data.get(); data.get() // flags
                                if (version == 1) {
                                    if (data.remaining() >= 28) {
                                        data.long // creation
                                        data.long // modification
                                        timescale = data.int.toLong() and 0xFFFFFFFFL
                                        totalDuration = data.long
                                    }
                                } else {
                                    if (data.remaining() >= 16) {
                                        data.int // creation
                                        data.int // modification
                                        timescale = data.int.toLong() and 0xFFFFFFFFL
                                        totalDuration = data.int.toLong() and 0xFFFFFFFFL
                                    }
                                }
                            }
                        }
                    }
                    position += if (actualSize > 0 && actualSize <= size_file - position) actualSize else boxSize
                }
                "chpl" -> {
                    val actualSize = when {
                        size == 1L -> {
                             val extHeader = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
                             channel.read(extHeader, position + 8)
                             extHeader.flip()
                             extHeader.long
                        }
                        size == 0L -> boxSize
                        else -> size
                    }
                    if (actualSize > 0 && actualSize <= Int.MAX_VALUE) {
                        val headerOffset = if (size == 1L) 16 else 8
                        val dataSize = (actualSize - headerOffset).toInt().coerceIn(0, MAX_BOX_DATA_SIZE)
                        if (dataSize > 0) {
                            val data = ByteBuffer.allocate(dataSize).order(ByteOrder.BIG_ENDIAN)
                            channel.read(data, position + headerOffset)
                            data.flip()

                            if (data.remaining() >= 4) {
                                val version = data.get().toInt()
                                data.get(); data.get(); data.get() // flags
                                
                                val count = if (version == 1) {
                                    if (data.remaining() >= 5) {
                                        data.get() // reserved
                                        data.int
                                    } else 0
                                } else {
                                    if (data.remaining() >= 1) {
                                        data.get().toInt() and 0xFF
                                    } else 0
                                }.coerceIn(0, 2000) // cap to avoid OOM on corrupt files
                                
                                for (i in 0 until count) {
                                    if (data.remaining() < 9) break
                                    val startTime = data.long
                                    val titleLen = data.get().toInt() and 0xFF
                                    if (data.remaining() < titleLen) break
                                    val titleBytes = ByteArray(titleLen)
                                    data.get(titleBytes)
                                    val title = String(titleBytes, Charsets.UTF_8)
                                    chplEntries.add(ChapterEntry(title, startTime))
                                }
                                
                                for (i in 0 until chplEntries.size - 1) {
                                    val delta = chplEntries[i + 1].startTime - chplEntries[i].startTime
                                    chplEntries[i].duration = maxOf(0L, delta)
                                }
                                if (chplEntries.isNotEmpty() && totalDuration > 0 && timescale > 0) {
                                    try {
                                        // totalDuration is in mvhd timescale; convert to 100ns (chpl) units for consistency
                                        val totalDuration100ns = totalDuration * 10_000_000L / timescale
                                        val lastStart = chplEntries.last().startTime
                                        chplEntries.last().duration = maxOf(0L, totalDuration100ns - lastStart)
                                    } catch (_: ArithmeticException) {
                                        // overflow or invalid; leave last duration at 0
                                    }
                                }
                            }
                        }
                    }
                    position += if (actualSize > 0 && actualSize <= size_file - position) actualSize else boxSize
                }
                else -> {
                    val actualSize = if (size == 1L) {
                        val extHeader = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
                        channel.read(extHeader, position + 8)
                        extHeader.flip()
                        extHeader.long
                    } else if (size == 0L) {
                        size_file - position
                    } else size
                    if (actualSize <= 0 || actualSize > size_file) break
                    position += actualSize
                }
            }
        }
        return MovieData(timescale, chplEntries)
    }

    private data class MovieData(val timescale: Long, val chplEntries: List<ChapterEntry>)
    private data class ChapterEntry(val title: String, val startTime: Long, var duration: Long = 0)
}
