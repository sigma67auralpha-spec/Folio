package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.util.zip.ZipFile

data class ComicImageEntry(val name: String, val bytes: ByteArray)

object ComicArchiveHelper {

    private val imageExts = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    fun listCbzImageNames(zip: ZipFile): List<String> {
        return zip.entries().asSequence()
            .filter { !it.isDirectory }
            .map { it.name }
            .filter { name ->
                val ext = name.substringAfterLast('.', "").lowercase()
                ext in imageExts && !name.contains("__MACOSX")
            }
            .sortedWith(naturalComparator)
            .toList()
    }

    fun loadCbzPageBitmap(file: File, pageIndex: Int): Bitmap? {
        return runCatching {
            ZipFile(file).use { zip ->
                val names = listCbzImageNames(zip)
                val targetName = names.getOrNull(pageIndex) ?: return null
                val entry = zip.getEntry(targetName) ?: return null
                val bytes = zip.getInputStream(entry).use { it.readBytes() }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }.getOrNull()
    }

    /**
     * Parses RAR4 archives for uncompressed ("Store" method 0x30) comic image blocks.
     * Compressed solid RAR archives cannot be decoded without native unrar; those return empty with a clear UI hint.
     */
    fun listRarStoredImages(file: File): List<ComicImageEntry> {
        val result = mutableListOf<ComicImageEntry>()
        runCatching {
            val bytes = file.readBytes()
            if (bytes.size < 14) return emptyList()
            // RAR4 signature: 52 61 72 21 1A 07 00
            val isRar4 = bytes[0] == 0x52.toByte() && bytes[1] == 0x61.toByte() &&
                bytes[2] == 0x72.toByte() && bytes[3] == 0x21.toByte() &&
                bytes[4] == 0x1A.toByte() && bytes[5] == 0x07.toByte() && bytes[6] == 0x00.toByte()
            if (!isRar4) return emptyList()

            var pos = 7
            while (pos + 7 <= bytes.size) {
                val headType = bytes[pos + 2].toInt() and 0xFF
                val headFlags = readLe16(bytes, pos + 3)
                val headSize = readLe16(bytes, pos + 5)
                if (headSize < 7 || pos + headSize > bytes.size) break

                val addSize = if ((headFlags and 0x8000) != 0 && pos + 11 <= bytes.size) {
                    readLe32(bytes, pos + 7)
                } else 0L

                if (headType == 0x74 && pos + 32 <= bytes.size) { // File header
                    val packedSize = readLe32(bytes, pos + 7).toInt()
                    val method = bytes[pos + 25].toInt() and 0xFF
                    val nameSize = readLe16(bytes, pos + 26)
                    val nameStart = pos + 32
                    if (nameStart + nameSize <= pos + headSize) {
                        val entryName = String(bytes, nameStart, nameSize, Charsets.UTF_8)
                            .substringBefore('\u0000')
                        val ext = entryName.substringAfterLast('.', "").lowercase()
                        val dataStart = pos + headSize
                        if (method == 0x30 && ext in imageExts && packedSize > 0 && dataStart + packedSize <= bytes.size) {
                            result.add(ComicImageEntry(entryName, bytes.copyOfRange(dataStart, dataStart + packedSize)))
                        }
                    }
                }
                val step = headSize + addSize.toInt().coerceAtLeast(0)
                if (step <= 0) break
                pos += step
            }
        }
        return result.sortedWith(compareBy(naturalComparator) { it.name })
    }

    fun loadCbrPageBitmap(file: File, pageIndex: Int): Bitmap? {
        val entries = listRarStoredImages(file)
        val bytes = entries.getOrNull(pageIndex)?.bytes ?: return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun readLe16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun readLe32(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xFF) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFF) shl 24)

    val naturalComparator: Comparator<String> = Comparator { a, b ->
        val partsA = Regex("(\\d+|\\D+)").findAll(a).map { it.value }.toList()
        val partsB = Regex("(\\d+|\\D+)").findAll(b).map { it.value }.toList()
        val count = minOf(partsA.size, partsB.size)
        for (i in 0 until count) {
            val sa = partsA[i]
            val sb = partsB[i]
            val na = sa.toLongOrNull()
            val nb = sb.toLongOrNull()
            val cmp = if (na != null && nb != null) na.compareTo(nb) else sa.compareTo(sb, ignoreCase = true)
            if (cmp != 0) return@Comparator cmp
        }
        partsA.size.compareTo(partsB.size)
    }
}
