package com.example.engine

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

object MobiParser {

    fun parse(file: File, fallbackTitle: String): ParsedBookContent {
        return runCatching {
            val bytes = file.readBytes()
            if (bytes.size < 84) return ParsedBookContent.Error("File too small to be a valid MOBI/AZW3 book.")

            val dbName = String(bytes, 0, 32, Charsets.ISO_8859_1).trim('\u0000', ' ')
            val numRecords = readUInt16(bytes, 76)
            if (numRecords <= 1 || 78 + numRecords * 8 > bytes.size) {
                return ParsedBookContent.Error("Unsupported PalmDB header.")
            }

            val recordOffsets = IntArray(numRecords)
            for (i in 0 until numRecords) {
                recordOffsets[i] = readUInt32(bytes, 78 + i * 8).toInt()
            }

            fun getRecord(idx: Int): ByteArray {
                val start = recordOffsets[idx]
                val end = if (idx + 1 < numRecords) recordOffsets[idx + 1] else bytes.size
                if (start < 0 || end < start || end > bytes.size) return ByteArray(0)
                return bytes.copyOfRange(start, end)
            }

            val rec0 = getRecord(0)
            if (rec0.size < 16) return ParsedBookContent.Error("Corrupt MOBI record 0.")

            val compression = readUInt16(rec0, 0)
            val textRecords = readUInt16(rec0, 8)
            val encryptionType = readUInt16(rec0, 12)
            if (encryptionType != 0) {
                return ParsedBookContent.Error("This MOBI/AZW3 file is DRM-protected and cannot be opened in a local DRM-free reader.")
            }
            if (compression == 17480) {
                return ParsedBookContent.Error("HUFF/CDIC compressed MOBI/AZW3 files require external decompression; convert to EPUB or standard PalmDOC MOBI.")
            }

            var title = dbName.ifBlank { fallbackTitle }
            var author = "Unknown Author"
            var description = ""
            var coverOffset = -1
            var firstImageRecord = -1
            var charset = Charsets.UTF_8

            if (rec0.size >= 132 && String(rec0, 16, 4, Charsets.ISO_8859_1) == "MOBI") {
                val mobiHeaderLen = readUInt32(rec0, 20).toInt()
                val textEncoding = readUInt32(rec0, 28).toInt()
                charset = if (textEncoding == 65001) Charsets.UTF_8 else Charset.forName("windows-1252")
                val fullNameOffset = readUInt32(rec0, 84).toInt()
                val fullNameLen = readUInt32(rec0, 88).toInt()
                if (fullNameOffset in 0 until rec0.size && fullNameLen > 0 && fullNameOffset + fullNameLen <= rec0.size) {
                    val parsedName = String(rec0, fullNameOffset, fullNameLen, charset).trim()
                    if (parsedName.isNotEmpty()) title = parsedName
                }
                if (rec0.size >= 112) {
                    firstImageRecord = readUInt32(rec0, 108).toInt()
                }
                val exthFlags = if (rec0.size >= 132) readUInt32(rec0, 128).toInt() else 0
                if ((exthFlags and 0x40) != 0) {
                    val exthStart = 16 + mobiHeaderLen
                    if (exthStart + 12 <= rec0.size && String(rec0, exthStart, 4, Charsets.ISO_8859_1) == "EXTH") {
                        val exthCount = readUInt32(rec0, exthStart + 8).toInt()
                        var pos = exthStart + 12
                        repeat(exthCount.coerceAtMost(64)) {
                            if (pos + 8 > rec0.size) return@repeat
                            val recType = readUInt32(rec0, pos).toInt()
                            val recLen = readUInt32(rec0, pos + 4).toInt()
                            if (recLen < 8 || pos + recLen > rec0.size) return@repeat
                            val payload = rec0.copyOfRange(pos + 8, pos + recLen)
                            when (recType) {
                                100 -> author = String(payload, charset).trim()
                                103 -> description = TextHtmlMdParser.decodeHtmlEntities(String(payload, charset)).replace(Regex("<[^>]+>"), "").trim()
                                201 -> if (payload.size >= 4) coverOffset = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN).int
                                503 -> {
                                    val updated = String(payload, charset).trim()
                                    if (updated.isNotEmpty()) title = updated
                                }
                            }
                            pos += recLen
                        }
                    }
                }
            }

            val coverBytes = if (firstImageRecord > 0 && coverOffset >= 0 && firstImageRecord + coverOffset < numRecords) {
                getRecord(firstImageRecord + coverOffset)
            } else null

            val outStream = ByteArrayOutputStream()
            val maxTextRec = textRecords.coerceAtMost(numRecords - 1)
            for (i in 1..maxTextRec) {
                val rawRec = getRecord(i)
                val decoded = when (compression) {
                    1 -> rawRec
                    2 -> decompressPalmDoc(rawRec)
                    else -> rawRec
                }
                outStream.write(decoded)
            }

            val html = String(outStream.toByteArray(), charset)
            val blocks = TextHtmlMdParser.extractHtmlBlocks(html)
            val chapters = TextHtmlMdParser.splitBlocksIntoChapters(blocks, title)
            ParsedBookContent.Reflowable(
                metadata = ParsedMetadata(
                    title = title,
                    author = author,
                    description = description,
                    coverBytes = coverBytes,
                    totalUnits = chapters.size
                ),
                chapters = chapters
            )
        }.getOrElse {
            ParsedBookContent.Error("Failed to read MOBI/AZW3: ${it.localizedMessage ?: "unsupported binary layout"}")
        }
    }

    private fun decompressPalmDoc(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(data.size * 2)
        var i = 0
        while (i < data.size) {
            val b = data[i].toInt() and 0xFF
            i++
            when {
                b == 0 -> out.write(0)
                b in 1..8 -> {
                    val count = b.coerceAtMost(data.size - i)
                    out.write(data, i, count)
                    i += count
                }
                b in 0x09..0x7F -> out.write(b)
                b in 0x80..0xBF -> {
                    if (i >= data.size) break
                    val next = data[i].toInt() and 0xFF
                    i++
                    val pair = (b shl 8) or next
                    val distance = (pair shr 3) and 0x07FF
                    val length = (pair and 0x07) + 3
                    val currentBytes = out.toByteArray()
                    val startPos = currentBytes.size - distance
                    if (distance > 0 && startPos >= 0) {
                        for (k in 0 until length) {
                            val idx = startPos + (k % distance)
                            if (idx in currentBytes.indices) out.write(currentBytes[idx].toInt())
                        }
                    }
                }
                else -> {
                    out.write(' '.code)
                    out.write(b xor 0x80)
                }
            }
        }
        return out.toByteArray()
    }

    private fun readUInt16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)

    private fun readUInt32(bytes: ByteArray, offset: Int): Long =
        ((bytes[offset].toLong() and 0xFF) shl 24) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 8) or
            (bytes[offset + 3].toLong() and 0xFF)
}
