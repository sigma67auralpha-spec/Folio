package com.example.engine

import android.content.Context
import android.net.Uri
import com.example.data.local.FolioDao
import com.example.data.model.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.BookShelfCrossRef
import com.example.data.model.HighlightColor
import com.example.data.model.HighlightEntity
import com.example.data.model.ReadStatus
import com.example.data.model.ReadingSessionEntity
import com.example.data.model.ShelfEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    private const val MAX_JSON_BYTES = 16 * 1024 * 1024
    private const val MAX_COVER_BYTES = 16 * 1024 * 1024
    private const val MAX_TOTAL_RESTORE_BYTES = 128L * 1024 * 1024
    private const val MAX_ZIP_ENTRIES = 5_000

    internal fun sanitizeBackupCoverName(rawEntryName: String): String? {
        val normalized = rawEntryName.replace('\\', '/').substringAfterLast('/')
        if (normalized.isBlank() || normalized == "." || normalized == "..") return null
        val safe = normalized.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        if (safe.isBlank() || safe.startsWith(".")) return null
        return safe
    }

    internal fun isPathInsideDir(target: File, parentDir: File): Boolean {
        val parentCanonical = parentDir.canonicalFile.path
        val targetCanonical = target.canonicalFile.path
        return targetCanonical.startsWith(parentCanonical + File.separator)
    }

    private fun readBoundedBytes(zis: ZipInputStream, maxBytes: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val read = zis.read(buf)
            if (read <= 0) break
            total += read
            if (total > maxBytes) {
                error("Backup entry exceeds maximum allowed size ($maxBytes bytes).")
            }
            out.write(buf, 0, read)
        }
        return out.toByteArray()
    }

    suspend fun exportBackupZip(context: Context, dao: FolioDao, destUri: Uri): Result<String> {
        return runCatching {
            val books = dao.getAllBooksSnapshot()
            val shelves = dao.getAllShelvesSnapshot()
            val shelfRefs = dao.getAllShelfRefsSnapshot()
            val highlights = dao.getAllHighlightsSnapshot()
            val sessions = dao.getAllSessionsSnapshot()

            val root = JSONObject().apply {
                put("version", 1)
                put("exportedAt", System.currentTimeMillis())
                put("books", JSONArray().apply {
                    books.forEach { b ->
                        put(JSONObject().apply {
                            put("id", b.id)
                            put("title", b.title)
                            put("author", b.author)
                            put("series", b.series)
                            put("seriesIndex", b.seriesIndex)
                            put("description", b.description)
                            put("format", b.format.name)
                            put("fileUri", b.fileUri)
                            put("localCopyPath", b.localCopyPath ?: "")
                            put("coverPath", b.coverPath ?: "")
                            put("fileHash", b.fileHash)
                            put("fileSizeBytes", b.fileSizeBytes)
                            put("totalUnits", b.totalUnits)
                            put("currentUnit", b.currentUnit)
                            put("scrollFraction", b.scrollFraction)
                            put("progressPercent", b.progressPercent)
                            put("readStatus", b.readStatus.name)
                            put("totalReadingSeconds", b.totalReadingSeconds)
                            put("addedAt", b.addedAt)
                            put("lastOpenedAt", b.lastOpenedAt ?: 0L)
                        })
                    }
                })
                put("shelves", JSONArray().apply {
                    shelves.forEach { s ->
                        put(JSONObject().apply {
                            put("id", s.id)
                            put("name", s.name)
                            put("createdAt", s.createdAt)
                        })
                    }
                })
                put("shelfRefs", JSONArray().apply {
                    shelfRefs.forEach { r ->
                        put(JSONObject().apply {
                            put("bookId", r.bookId)
                            put("shelfId", r.shelfId)
                        })
                    }
                })
                put("highlights", JSONArray().apply {
                    highlights.forEach { h ->
                        put(JSONObject().apply {
                            put("bookId", h.bookId)
                            put("chapterIndex", h.chapterIndex)
                            put("paragraphIndex", h.paragraphIndex)
                            put("startOffset", h.startOffset)
                            put("endOffset", h.endOffset)
                            put("quoteText", h.quoteText)
                            put("noteText", h.noteText)
                            put("color", h.color.name)
                            put("createdAt", h.createdAt)
                        })
                    }
                })
                put("sessions", JSONArray().apply {
                    sessions.forEach { s ->
                        put(JSONObject().apply {
                            put("bookId", s.bookId)
                            put("epochDay", s.epochDay)
                            put("durationSeconds", s.durationSeconds)
                        })
                    }
                })
            }

            context.contentResolver.openOutputStream(destUri)?.use { os ->
                ZipOutputStream(os).use { zos ->
                    zos.putNextEntry(ZipEntry("folio_backup.json"))
                    zos.write(root.toString(2).toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    books.mapNotNull { it.coverPath }.distinct().forEach { path ->
                        val coverFile = File(path)
                        if (coverFile.exists()) {
                            zos.putNextEntry(ZipEntry("covers/${coverFile.name}"))
                            coverFile.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            } ?: error("Cannot write backup output stream.")

            "Backed up ${books.size} books and ${highlights.size} notes."
        }
    }

    suspend fun restoreBackupZip(context: Context, dao: FolioDao, srcUri: Uri): Result<String> {
        return runCatching {
            var jsonString: String? = null
            val coverDir = File(context.filesDir, "covers").apply { mkdirs() }
            val booksDir = File(context.filesDir, "books").apply { mkdirs() }
            var totalBytesRead = 0L
            var entryCount = 0

            context.contentResolver.openInputStream(srcUri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        entryCount++
                        if (entryCount > MAX_ZIP_ENTRIES) {
                            error("Backup archive contains too many entries.")
                        }
                        val normalizedName = entry.name.replace('\\', '/')
                        if (normalizedName == "folio_backup.json" && !entry.isDirectory) {
                            val bytes = readBoundedBytes(zis, MAX_JSON_BYTES)
                            totalBytesRead += bytes.size
                            if (totalBytesRead > MAX_TOTAL_RESTORE_BYTES) error("Backup archive exceeds size limit.")
                            jsonString = String(bytes, Charsets.UTF_8)
                        } else if (normalizedName.startsWith("covers/") && !entry.isDirectory) {
                            val safeName = sanitizeBackupCoverName(normalizedName)
                            if (safeName != null) {
                                val targetFile = File(coverDir, safeName)
                                if (isPathInsideDir(targetFile, coverDir)) {
                                    val bytes = readBoundedBytes(zis, MAX_COVER_BYTES)
                                    totalBytesRead += bytes.size
                                    if (totalBytesRead > MAX_TOTAL_RESTORE_BYTES) error("Backup archive exceeds size limit.")
                                    targetFile.writeBytes(bytes)
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: error("Cannot open backup archive.")

            val root = JSONObject(jsonString ?: error("Invalid backup: missing folio_backup.json"))
            val bookIdMap = mutableMapOf<Long, Long>()
            val shelfIdMap = mutableMapOf<Long, Long>()

            val shelvesArr = root.optJSONArray("shelves") ?: JSONArray()
            for (i in 0 until shelvesArr.length()) {
                val obj = shelvesArr.getJSONObject(i)
                val oldId = obj.getLong("id")
                val newId = dao.insertShelf(
                    ShelfEntity(name = obj.getString("name"), createdAt = obj.optLong("createdAt"))
                )
                shelfIdMap[oldId] = newId
            }

            val booksArr = root.optJSONArray("books") ?: JSONArray()
            var importedCount = 0
            for (i in 0 until booksArr.length()) {
                val obj = booksArr.getJSONObject(i)
                val oldId = obj.getLong("id")
                val format = runCatching { BookFormat.valueOf(obj.getString("format")) }.getOrNull() ?: continue
                val hash = obj.getString("fileHash")
                val existing = dao.findBookByHash(hash)
                if (existing != null) {
                    bookIdMap[oldId] = existing.id
                    continue
                }
                val safeCoverName = sanitizeBackupCoverName(obj.optString("coverPath"))
                val restoredCover = if (safeCoverName != null) {
                    File(coverDir, safeCoverName)
                        .takeIf { isPathInsideDir(it, coverDir) && it.exists() }
                        ?.absolutePath
                } else null
                val rawLocalPath = obj.optString("localCopyPath")
                val localPath = rawLocalPath.takeIf { it.isNotBlank() }?.let { path ->
                    val candidate = File(path)
                    candidate.takeIf { isPathInsideDir(it, booksDir) && it.exists() }?.absolutePath
                }
                val newId = dao.insertBook(
                    BookEntity(
                        title = obj.getString("title"),
                        author = obj.optString("author", "Unknown Author"),
                        series = obj.optString("series", ""),
                        seriesIndex = obj.optDouble("seriesIndex", 0.0).toFloat(),
                        description = obj.optString("description", ""),
                        format = format,
                        fileUri = obj.getString("fileUri"),
                        localCopyPath = localPath,
                        coverPath = restoredCover,
                        fileHash = hash,
                        fileSizeBytes = obj.optLong("fileSizeBytes"),
                        totalUnits = obj.optInt("totalUnits", 1),
                        currentUnit = obj.optInt("currentUnit", 0),
                        scrollFraction = obj.optDouble("scrollFraction", 0.0).toFloat(),
                        progressPercent = obj.optDouble("progressPercent", 0.0).toFloat(),
                        readStatus = runCatching { ReadStatus.valueOf(obj.optString("readStatus")) }
                            .getOrDefault(ReadStatus.UNREAD),
                        totalReadingSeconds = obj.optLong("totalReadingSeconds", 0L),
                        addedAt = obj.optLong("addedAt", System.currentTimeMillis()),
                        lastOpenedAt = obj.optLong("lastOpenedAt", 0L).takeIf { it > 0L },
                        isMissingFile = localPath == null
                    )
                )
                bookIdMap[oldId] = newId
                importedCount++
            }

            val refsArr = root.optJSONArray("shelfRefs") ?: JSONArray()
            for (i in 0 until refsArr.length()) {
                val obj = refsArr.getJSONObject(i)
                val bId = bookIdMap[obj.getLong("bookId")]
                val sId = shelfIdMap[obj.getLong("shelfId")]
                if (bId != null && sId != null) {
                    dao.insertBookShelfRef(BookShelfCrossRef(bId, sId))
                }
            }

            val hlArr = root.optJSONArray("highlights") ?: JSONArray()
            for (i in 0 until hlArr.length()) {
                val obj = hlArr.getJSONObject(i)
                val bId = bookIdMap[obj.getLong("bookId")] ?: continue
                dao.insertHighlight(
                    HighlightEntity(
                        bookId = bId,
                        chapterIndex = obj.optInt("chapterIndex"),
                        paragraphIndex = obj.optInt("paragraphIndex"),
                        startOffset = obj.optInt("startOffset"),
                        endOffset = obj.optInt("endOffset"),
                        quoteText = obj.getString("quoteText"),
                        noteText = obj.optString("noteText", ""),
                        color = runCatching { HighlightColor.valueOf(obj.optString("color")) }
                            .getOrDefault(HighlightColor.AMBER),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val sessArr = root.optJSONArray("sessions") ?: JSONArray()
            for (i in 0 until sessArr.length()) {
                val obj = sessArr.getJSONObject(i)
                val bId = bookIdMap[obj.getLong("bookId")] ?: continue
                dao.insertSession(
                    ReadingSessionEntity(
                        bookId = bId,
                        epochDay = obj.getLong("epochDay"),
                        durationSeconds = obj.getLong("durationSeconds")
                    )
                )
            }

            "Restored $importedCount books from backup."
        }
    }

    fun formatNotesAsMarkdown(book: BookEntity, highlights: List<HighlightEntity>): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return buildString {
            appendLine("# ${book.title}")
            appendLine("*Author:* ${book.author}  ")
            if (book.series.isNotBlank()) {
                appendLine("*Series:* ${book.series} #${book.seriesIndex}  ")
            }
            appendLine("*Exported:* ${sdf.format(Date())}")
            appendLine()
            appendLine("---")
            appendLine()
            if (highlights.isEmpty()) {
                appendLine("_No highlights or notes recorded yet._")
            } else {
                highlights.forEach { h ->
                    appendLine("> ${h.quoteText}")
                    if (h.noteText.isNotBlank()) {
                        appendLine()
                        appendLine("**Note (${h.color.label}):** ${h.noteText}")
                    }
                    appendLine()
                    appendLine("*Chapter ${h.chapterIndex + 1} • ${sdf.format(Date(h.createdAt))}*")
                    appendLine()
                }
            }
        }
    }
}
