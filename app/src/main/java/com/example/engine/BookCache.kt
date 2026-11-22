package com.example.engine

import android.util.LruCache
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Fast binary cache for parsed reflowable books (in-memory LRU + private disk cache)
 * so reopening a book loads nearly instantaneously without re-unzipping or re-parsing HTML.
 */
object BookCache {
    private const val MAGIC = 0x464F4C43 // "FOLC"
    private const val VERSION = 1

    private val memoryCache = object : LruCache<String, ParsedBookContent.Reflowable>(2) {}

    fun getFromMemory(key: String): ParsedBookContent.Reflowable? = memoryCache.get(key)

    fun putInMemory(key: String, content: ParsedBookContent.Reflowable) {
        if (key.isNotBlank()) memoryCache.put(key, content)
    }

    fun readFromDisk(cacheFile: File): ParsedBookContent.Reflowable? {
        if (!cacheFile.exists() || cacheFile.length() < 16L) return null
        return runCatching {
            DataInputStream(BufferedInputStream(FileInputStream(cacheFile), 32 * 1024)).use { dis ->
                if (dis.readInt() != MAGIC || dis.readInt() != VERSION) return null
                val title = dis.readUTF()
                val author = dis.readUTF()
                val series = dis.readUTF()
                val seriesIndex = dis.readFloat()
                val description = dis.readUTF()
                val totalUnits = dis.readInt()
                val chapterCount = dis.readInt().coerceIn(0, 10_000)
                val chapters = ArrayList<BookChapter>(chapterCount)
                val styles = BlockStyle.entries
                for (c in 0 until chapterCount) {
                    val idx = dis.readInt()
                    val cTitle = dis.readUTF()
                    val blockCount = dis.readInt().coerceIn(0, 200_000)
                    val blocks = ArrayList<ParagraphBlock>(blockCount)
                    for (b in 0 until blockCount) {
                        val text = readLongString(dis)
                        val styleOrd = dis.readByte().toInt().coerceIn(0, styles.lastIndex)
                        val hasImg = dis.readBoolean()
                        val imgPath = if (hasImg) dis.readUTF() else null
                        val runCount = dis.readInt().coerceIn(0, 5_000)
                        val runs = if (runCount == 0) emptyList() else ArrayList<TextRun>(runCount).apply {
                            for (r in 0 until runCount) {
                                add(
                                    TextRun(
                                        start = dis.readInt(),
                                        end = dis.readInt(),
                                        bold = dis.readBoolean(),
                                        italic = dis.readBoolean(),
                                        code = dis.readBoolean()
                                    )
                                )
                            }
                        }
                        blocks.add(ParagraphBlock(text, styles[styleOrd], runs, imgPath))
                    }
                    chapters.add(BookChapter(idx, cTitle, blocks))
                }
                ParsedBookContent.Reflowable(
                    metadata = ParsedMetadata(
                        title = title,
                        author = author,
                        series = series,
                        seriesIndex = seriesIndex,
                        description = description,
                        coverBytes = null,
                        totalUnits = totalUnits
                    ),
                    chapters = chapters
                )
            }
        }.getOrNull()
    }

    fun writeToDisk(cacheFile: File, content: ParsedBookContent.Reflowable) {
        runCatching {
            cacheFile.parentFile?.mkdirs()
            val tmp = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
            DataOutputStream(BufferedOutputStream(FileOutputStream(tmp), 32 * 1024)).use { dos ->
                dos.writeInt(MAGIC)
                dos.writeInt(VERSION)
                dos.writeUTF(content.metadata.title.take(2048))
                dos.writeUTF(content.metadata.author.take(1024))
                dos.writeUTF(content.metadata.series.take(1024))
                dos.writeFloat(content.metadata.seriesIndex)
                dos.writeUTF(content.metadata.description.take(8192))
                dos.writeInt(content.metadata.totalUnits)
                dos.writeInt(content.chapters.size)
                for (ch in content.chapters) {
                    dos.writeInt(ch.index)
                    dos.writeUTF(ch.title.take(1024))
                    dos.writeInt(ch.blocks.size)
                    for (block in ch.blocks) {
                        writeLongString(dos, block.text)
                        dos.writeByte(block.style.ordinal)
                        val img = block.imagePath
                        if (img != null) {
                            dos.writeBoolean(true)
                            dos.writeUTF(img)
                        } else {
                            dos.writeBoolean(false)
                        }
                        dos.writeInt(block.runs.size)
                        for (run in block.runs) {
                            dos.writeInt(run.start)
                            dos.writeInt(run.end)
                            dos.writeBoolean(run.bold)
                            dos.writeBoolean(run.italic)
                            dos.writeBoolean(run.code)
                        }
                    }
                }
            }
            if (cacheFile.exists()) cacheFile.delete()
            tmp.renameTo(cacheFile)
        }
    }

    private fun writeLongString(dos: DataOutputStream, s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        dos.writeInt(bytes.size)
        dos.write(bytes)
    }

    private fun readLongString(dis: DataInputStream): String {
        val len = dis.readInt().coerceIn(0, 8 * 1024 * 1024)
        if (len == 0) return ""
        val bytes = ByteArray(len)
        dis.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }
}
