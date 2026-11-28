package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipFile

data class ComicImageEntry(val name: String, val bytes: ByteArray)
data class RarImageHeader(val name: String, val dataOffset: Long, val packedSize: Int)

object ComicArchiveHelper {

    private val imageExts = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    private val cbzIndexCache = object : LruCache<String, List<String>>(4) {}
    private val cbrIndexCache = object : LruCache<String, List<RarImageHeader>>(4) {}

    private val cbzZipLock = Any()
    private var cachedZipKey: String? = null
    private var cachedZipFile: ZipFile? = null

    fun closeCachedArchives() {
        synchronized(cbzZipLock) {
            runCatching { cachedZipFile?.close() }
            cachedZipFile = null
            cachedZipKey = null
        }
    }

    private inline fun <T> withCbzZip(file: File, block: (ZipFile) -> T): T {
        synchronized(cbzZipLock) {
            val key = "${file.absolutePath}:${file.lastModified()}:${file.length()}"
            if (cachedZipKey != key || cachedZipFile == null) {
                runCatching { cachedZipFile?.close() }
                cachedZipFile = ZipFile(file)
                cachedZipKey = key
            }
            return block(cachedZipFile!!)
        }
    }

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

    fun getCbzPageCount(file: File): Int {
        return runCatching {
            withCbzZip(file) { zip -> getCachedCbzNames(file, zip).size }
        }.getOrDefault(0)
    }

    private fun getCachedCbzNames(file: File, zip: ZipFile): List<String> {
        val key = "${file.absolutePath}:${file.lastModified()}"
        return cbzIndexCache.get(key) ?: listCbzImageNames(zip).also { cbzIndexCache.put(key, it) }
    }

    fun loadCbzPageBitmap(file: File, pageIndex: Int): Bitmap? {
        return runCatching {
            val bytes = withCbzZip(file) { zip ->
                val names = getCachedCbzNames(file, zip)
                val targetName = names.getOrNull(pageIndex) ?: return null
                val entry = zip.getEntry(targetName) ?: return null
                zip.getInputStream(entry).use { it.readBytes() }
            }
            decodeBoundedBitmap(bytes)
        }.getOrNull()
    }

    /**
     * Indexes RAR4 stored ("Store" method 0x30) image headers without copying every page's bytes into memory.
     */
    fun listRarStoredHeaders(file: File): List<RarImageHeader> {
        val key = "${file.absolutePath}:${file.lastModified()}"
        cbrIndexCache.get(key)?.let { return it }

        val result = mutableListOf<RarImageHeader>()
        runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val fileLen = raf.length()
                if (fileLen < 14L) return emptyList()
                val sig = ByteArray(7)
                raf.readFully(sig)
                val isRar4 = sig[0] == 0x52.toByte() && sig[1] == 0x61.toByte() &&
                    sig[2] == 0x72.toByte() && sig[3] == 0x21.toByte() &&
                    sig[4] == 0x1A.toByte() && sig[5] == 0x07.toByte() && sig[6] == 0x00.toByte()
                if (!isRar4) return emptyList()

                var pos = 7L
                val headerBuf = ByteArray(32)
                while (pos + 7L <= fileLen) {
                    raf.seek(pos)
                    val readBase = raf.read(headerBuf, 0, minOf(32L, fileLen - pos).toInt())
                    if (readBase < 7) break
                    val headType = headerBuf[2].toInt() and 0xFF
                    val headFlags = readLe16(headerBuf, 3)
                    val headSize = readLe16(headerBuf, 5)
                    if (headSize < 7 || pos + headSize > fileLen) break

                    val addSize = if ((headFlags and 0x8000) != 0 && readBase >= 11) {
                        readLe32(headerBuf, 7)
                    } else 0L

                    if (headType == 0x74 && readBase >= 32) {
                        val packedSize = readLe32(headerBuf, 7).toInt()
                        val method = headerBuf[25].toInt() and 0xFF
                        val nameSize = readLe16(headerBuf, 26)
                        val nameStart = pos + 32L
                        if (nameSize in 1..4096 && nameStart + nameSize <= pos + headSize) {
                            val nameBytes = ByteArray(nameSize)
                            raf.seek(nameStart)
                            raf.readFully(nameBytes)
                            val entryName = String(nameBytes, Charsets.UTF_8).substringBefore('\u0000')
                            val ext = entryName.substringAfterLast('.', "").lowercase()
                            val dataStart = pos + headSize
                            if (method == 0x30 && ext in imageExts && packedSize > 0 && dataStart + packedSize <= fileLen) {
                                result.add(RarImageHeader(entryName, dataStart, packedSize))
                            }
                        }
                    }
                    val step = headSize + addSize.coerceAtLeast(0L)
                    if (step <= 0L) break
                    pos += step
                }
            }
        }
        val sorted = result.sortedWith(compareBy(naturalComparator) { it.name })
        cbrIndexCache.put(key, sorted)
        return sorted
    }

    fun listRarStoredImages(file: File): List<ComicImageEntry> {
        val headers = listRarStoredHeaders(file)
        if (headers.isEmpty()) return emptyList()
        val result = ArrayList<ComicImageEntry>(headers.size)
        runCatching {
            RandomAccessFile(file, "r").use { raf ->
                for (h in headers) {
                    val buf = ByteArray(h.packedSize)
                    raf.seek(h.dataOffset)
                    raf.readFully(buf)
                    result.add(ComicImageEntry(h.name, buf))
                }
            }
        }
        return result
    }

    fun loadCbrPageBitmap(file: File, pageIndex: Int): Bitmap? {
        val headers = listRarStoredHeaders(file)
        val target = headers.getOrNull(pageIndex) ?: return null
        return runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val buf = ByteArray(target.packedSize)
                raf.seek(target.dataOffset)
                raf.readFully(buf)
                decodeBoundedBitmap(buf)
            }
        }.getOrNull()
    }

    private fun decodeBoundedBitmap(bytes: ByteArray, maxDim: Int = 2048): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun readLe16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun readLe32(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xFF) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFF) shl 24)

    val naturalComparator: Comparator<String> = Comparator { a, b ->
        var i = 0
        var j = 0
        val lenA = a.length
        val lenB = b.length
        while (i < lenA && j < lenB) {
            val chA = a[i]
            val chB = b[j]
            if (chA.isDigit() && chB.isDigit()) {
                val startA = i
                while (i < lenA && a[i].isDigit()) i++
                val startB = j
                while (j < lenB && b[j].isDigit()) j++

                var nzA = startA
                while (nzA < i && a[nzA] == '0') nzA++
                var nzB = startB
                while (nzB < j && b[nzB] == '0') nzB++

                val sigLenA = i - nzA
                val sigLenB = j - nzB
                if (sigLenA != sigLenB) return@Comparator sigLenA.compareTo(sigLenB)

                for (k in 0 until sigLenA) {
                    val dCmp = a[nzA + k].compareTo(b[nzB + k])
                    if (dCmp != 0) return@Comparator dCmp
                }
                val rawLenCmp = (i - startA).compareTo(j - startB)
                if (rawLenCmp != 0) return@Comparator rawLenCmp
            } else {
                val cmp = chA.lowercaseChar().compareTo(chB.lowercaseChar())
                if (cmp != 0) return@Comparator cmp
                i++
                j++
            }
        }
        lenA.compareTo(lenB)
    }
}
