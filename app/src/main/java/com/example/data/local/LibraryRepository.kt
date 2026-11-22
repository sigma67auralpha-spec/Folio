package com.example.data.local

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.data.model.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.BookShelfCrossRef
import com.example.data.model.BookTagCrossRef
import com.example.data.model.BookWithRelations
import com.example.data.model.BookmarkEntity
import com.example.data.model.HighlightColor
import com.example.data.model.HighlightEntity
import com.example.data.model.ReadStatus
import com.example.data.model.ReadingSessionEntity
import com.example.data.model.ShelfEntity
import com.example.data.model.TagEntity
import com.example.data.model.WatchedFolderEntity
import com.example.engine.BackupManager
import com.example.engine.BookParser
import com.example.widget.FolioWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class LibraryRepository(
    private val context: Context,
    private val dao: FolioDao
) {
    private val booksDir = File(context.filesDir, "books").apply { mkdirs() }
    private val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
    private val fontsDir = File(context.filesDir, "fonts").apply { mkdirs() }
    private val readingTimeMutex = Mutex()

    val booksWithRelations: Flow<List<BookWithRelations>> = dao.observeBooksWithRelations()
    val shelves: Flow<List<ShelfEntity>> = dao.observeShelves()
    val tags: Flow<List<TagEntity>> = dao.observeTags()
    val sessions: Flow<List<ReadingSessionEntity>> = dao.observeReadingSessions()
    val watchedFolders: Flow<List<WatchedFolderEntity>> = dao.observeWatchedFolders()

    fun observeBook(bookId: Long): Flow<BookWithRelations?> = dao.observeBookWithRelations(bookId)
    fun observeHighlights(bookId: Long): Flow<List<HighlightEntity>> = dao.observeHighlights(bookId)
    fun observeBookmarks(bookId: Long): Flow<List<BookmarkEntity>> = dao.observeBookmarks(bookId)

    suspend fun ensureStarterLibraryIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.getAllBooksSnapshot().isNotEmpty()) return@withContext
        seedAssetBook(
            assetPath = "samples/folio_welcome_guide.md",
            fileName = "folio_welcome_guide.md",
            overrideTitle = "Welcome to Folio — Quick Guide",
            overrideAuthor = "Personal Library Handbook",
            overrideSeries = "Folio Guides",
            seriesIdx = 1f
        )
        seedAssetBook(
            assetPath = "samples/the_gift_of_the_magi.txt",
            fileName = "the_gift_of_the_magi.txt",
            overrideTitle = "The Gift of the Magi",
            overrideAuthor = "O. Henry",
            overrideSeries = "Short Classics",
            seriesIdx = 1f
        )
        val guideShelfId = dao.insertShelf(ShelfEntity(name = "Quick Reads"))
        dao.getAllBooksSnapshot().forEach { book ->
            dao.insertBookShelfRef(BookShelfCrossRef(book.id, guideShelfId))
        }
    }

    private suspend fun seedAssetBook(
        assetPath: String,
        fileName: String,
        overrideTitle: String,
        overrideAuthor: String,
        overrideSeries: String,
        seriesIdx: Float
    ) {
        val bytes = runCatching { context.assets.open(assetPath).use { it.readBytes() } }.getOrNull() ?: return
        val hash = sha256(bytes)
        if (dao.findBookByHash(hash) != null) return
        val format = BookFormat.fromFileName(fileName) ?: return
        val destFile = File(booksDir, "${hash.take(16)}_$fileName")
        destFile.writeBytes(bytes)
        val parsed = BookParser.parseMetadataAndCover(context, destFile, format, overrideTitle)
        dao.insertBook(
            BookEntity(
                title = overrideTitle,
                author = overrideAuthor,
                series = overrideSeries,
                seriesIndex = seriesIdx,
                description = parsed.description,
                format = format,
                fileUri = Uri.fromFile(destFile).toString(),
                localCopyPath = destFile.absolutePath,
                fileHash = hash,
                fileSizeBytes = destFile.length(),
                totalUnits = parsed.totalUnits
            )
        )
    }

    suspend fun importDocumentUris(uris: List<Uri>): ImportResult = withContext(Dispatchers.IO) {
        var added = 0
        var skippedDuplicates = 0
        var unsupported = 0

        for (uri in uris) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val outcome = importSingleUri(uri)
            when (outcome) {
                SingleImportStatus.ADDED -> added++
                SingleImportStatus.DUPLICATE -> skippedDuplicates++
                SingleImportStatus.UNSUPPORTED -> unsupported++
            }
        }
        if (added > 0) FolioWidgetUpdater.requestUpdate(context)
        ImportResult(added, skippedDuplicates, unsupported)
    }

    suspend fun linkAndScanFolder(treeUri: Uri): ImportResult = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        val folderName = queryFileName(folderUri) ?: "Linked Folder"
        dao.insertWatchedFolder(WatchedFolderEntity(treeUri = treeUri.toString(), displayName = folderName))
        scanDocumentTree(treeUri, docId)
    }

    suspend fun rescanFolder(folder: WatchedFolderEntity): ImportResult = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(folder.treeUri)
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            ?: return@withContext ImportResult(0, 0, 0)
        val res = scanDocumentTree(treeUri, docId)
        dao.updateFolderScanTime(folder.id, System.currentTimeMillis())
        res
    }

    suspend fun removeWatchedFolder(folder: WatchedFolderEntity) = withContext(Dispatchers.IO) {
        dao.deleteWatchedFolder(folder)
    }

    private suspend fun scanDocumentTree(treeUri: Uri, rootDocId: String): ImportResult {
        var added = 0
        var duplicates = 0
        var unsupported = 0
        val stack = ArrayDeque<String>()
        stack.add(rootDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        while (stack.isNotEmpty()) {
            val parentId = stack.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val childId = cursor.getString(0) ?: continue
                    val name = cursor.getString(1) ?: continue
                    val mime = cursor.getString(2) ?: ""
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        stack.addLast(childId)
                    } else if (BookFormat.fromFileName(name) != null) {
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                        when (importSingleUri(fileUri, explicitName = name)) {
                            SingleImportStatus.ADDED -> added++
                            SingleImportStatus.DUPLICATE -> duplicates++
                            SingleImportStatus.UNSUPPORTED -> unsupported++
                        }
                    }
                }
            }
        }
        if (added > 0) FolioWidgetUpdater.requestUpdate(context)
        return ImportResult(added, duplicates, unsupported)
    }

    private enum class SingleImportStatus { ADDED, DUPLICATE, UNSUPPORTED }

    private suspend fun importSingleUri(uri: Uri, explicitName: String? = null): SingleImportStatus {
        val fileName = explicitName ?: queryFileName(uri) ?: "book_${System.currentTimeMillis()}"
        val format = BookFormat.fromFileName(fileName) ?: return SingleImportStatus.UNSUPPORTED

        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return SingleImportStatus.UNSUPPORTED

        if (bytes.isEmpty()) return SingleImportStatus.UNSUPPORTED
        val hash = sha256(bytes)
        val existing = dao.findBookByHash(hash)
        if (existing != null) {
            if (existing.isMissingFile) {
                val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val localFile = File(booksDir, "${hash.take(16)}_$safeName")
                localFile.writeBytes(bytes)
                dao.updateBook(existing.copy(fileUri = uri.toString(), localCopyPath = localFile.absolutePath, isMissingFile = false))
            }
            return SingleImportStatus.DUPLICATE
        }

        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val localFile = File(booksDir, "${hash.take(16)}_$safeName")
        localFile.writeBytes(bytes)

        val fallbackTitle = fileName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim()
            .ifBlank { "Untitled Book" }
        val parsed = BookParser.parseMetadataAndCover(context, localFile, format, fallbackTitle)

        var coverPath: String? = null
        parsed.coverBytes?.let { cBytes ->
            if (cBytes.isNotEmpty()) {
                val coverFile = File(coversDir, "${hash.take(16)}.jpg")
                coverFile.writeBytes(cBytes)
                coverPath = coverFile.absolutePath
            }
        }

        dao.insertBook(
            BookEntity(
                title = parsed.title,
                author = parsed.author,
                series = parsed.series,
                seriesIndex = parsed.seriesIndex,
                description = parsed.description,
                format = format,
                fileUri = uri.toString(),
                localCopyPath = localFile.absolutePath,
                coverPath = coverPath,
                fileHash = hash,
                fileSizeBytes = localFile.length(),
                totalUnits = parsed.totalUnits
            )
        )
        return SingleImportStatus.ADDED
    }

    fun resolveBookFile(book: BookEntity): File? {
        val local = book.localCopyPath?.let { File(it) }
        if (local != null && local.exists()) return local
        return null
    }

    suspend fun reparseBookMetadata(book: BookEntity): Boolean = withContext(Dispatchers.IO) {
        val file = resolveBookFile(book) ?: return@withContext false
        val parsed = BookParser.parseMetadataAndCover(context, file, book.format, book.title)
        var coverPath = book.coverPath
        parsed.coverBytes?.let { cBytes ->
            if (cBytes.isNotEmpty()) {
                val coverFile = File(coversDir, "${book.fileHash.take(16)}.jpg")
                coverFile.writeBytes(cBytes)
                coverPath = coverFile.absolutePath
            }
        }
        dao.updateBook(
            book.copy(
                title = parsed.title.ifBlank { book.title },
                author = if (book.author == "Unknown Author" && parsed.author.isNotBlank()) parsed.author else book.author,
                series = book.series.ifBlank { parsed.series },
                seriesIndex = if (book.seriesIndex == 0f) parsed.seriesIndex else book.seriesIndex,
                description = book.description.ifBlank { parsed.description },
                coverPath = coverPath,
                totalUnits = parsed.totalUnits
            )
        )
        true
    }

    suspend fun importCustomFont(uri: Uri): Pair<String, String>? = withContext(Dispatchers.IO) {
        val name = queryFileName(uri) ?: "custom_font.ttf"
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext != "ttf" && ext != "otf") return@withContext null
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var total = 0
            while (true) {
                val read = input.read(buf)
                if (read <= 0) break
                total += read
                if (total > 32 * 1024 * 1024) return@withContext null
                out.write(buf, 0, read)
            }
            out.toByteArray()
        } ?: return@withContext null
        if (!hasValidFontHeader(bytes)) return@withContext null
        val safeName = name.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "custom_font.$ext" }
        val dest = File(fontsDir, safeName)
        if (!dest.canonicalFile.path.startsWith(fontsDir.canonicalFile.path + File.separator)) return@withContext null
        dest.writeBytes(bytes)
        dest.absolutePath to name.substringBeforeLast('.')
    }

    private fun hasValidFontHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val tag = String(bytes, 0, 4, Charsets.ISO_8859_1)
        val isSfnt = bytes[0] == 0x00.toByte() && bytes[1] == 0x01.toByte() &&
            bytes[2] == 0x00.toByte() && bytes[3] == 0x00.toByte()
        return isSfnt || tag == "OTTO" || tag == "true" || tag == "typ1" || tag == "ttcf"
    }

    suspend fun updateMetadata(
        book: BookEntity,
        title: String,
        author: String,
        series: String,
        seriesIndex: Float,
        description: String,
        newCoverUri: Uri?,
        tagNames: List<String>
    ) = withContext(Dispatchers.IO) {
        var coverPath = book.coverPath
        if (newCoverUri != null) {
            val bytes = context.contentResolver.openInputStream(newCoverUri)?.use { it.readBytes() }
            if (bytes != null && bytes.isNotEmpty()) {
                val coverFile = File(coversDir, "${book.fileHash.take(16)}_custom_${System.currentTimeMillis()}.jpg")
                coverFile.writeBytes(bytes)
                coverPath = coverFile.absolutePath
            }
        }
        dao.updateBook(
            book.copy(
                title = title.trim().ifBlank { book.title },
                author = author.trim().ifBlank { "Unknown Author" },
                series = series.trim(),
                seriesIndex = seriesIndex,
                description = description.trim(),
                coverPath = coverPath
            )
        )
        dao.clearTagsForBook(book.id)
        for (rawTag in tagNames.map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }.distinct()) {
            val existing = dao.findTagByName(rawTag)
            val tagId = existing?.id ?: dao.insertTag(TagEntity(name = rawTag))
            dao.insertBookTagRef(BookTagCrossRef(book.id, tagId))
        }
        FolioWidgetUpdater.requestUpdate(context)
    }

    suspend fun setBookShelves(bookId: Long, shelfIds: Set<Long>) = withContext(Dispatchers.IO) {
        dao.clearShelvesForBook(bookId)
        shelfIds.forEach { sId -> dao.insertBookShelfRef(BookShelfCrossRef(bookId, sId)) }
    }

    suspend fun createShelf(name: String): Long = withContext(Dispatchers.IO) {
        dao.insertShelf(ShelfEntity(name = name.trim()))
    }

    suspend fun renameShelf(shelf: ShelfEntity, newName: String) = withContext(Dispatchers.IO) {
        dao.updateShelf(shelf.copy(name = newName.trim()))
    }

    suspend fun deleteShelf(shelf: ShelfEntity) = withContext(Dispatchers.IO) {
        dao.deleteShelf(shelf)
    }

    suspend fun deleteBook(book: BookEntity, deleteLocalCopy: Boolean) = withContext(Dispatchers.IO) {
        if (deleteLocalCopy) {
            book.localCopyPath?.let { runCatching { File(it).delete() } }
            book.coverPath?.let { runCatching { File(it).delete() } }
        }
        dao.deleteBook(book)
        FolioWidgetUpdater.requestUpdate(context)
    }

    suspend fun saveProgress(bookId: Long, unit: Int, totalUnits: Int, scrollFraction: Float) = withContext(Dispatchers.IO) {
        val safeTotal = totalUnits.coerceAtLeast(1)
        val pct = if (safeTotal == 1) {
            (scrollFraction * 100f).coerceIn(0f, 100f)
        } else {
            (((unit + scrollFraction.coerceIn(0f, 1f)) / safeTotal.toFloat()) * 100f).coerceIn(0f, 100f)
        }
        val status = when {
            pct >= 98f || (safeTotal > 1 && unit >= safeTotal - 1 && scrollFraction >= 0.9f) -> ReadStatus.FINISHED
            pct > 0.5f || unit > 0 -> ReadStatus.READING
            else -> ReadStatus.UNREAD
        }
        dao.updateProgress(bookId, unit, scrollFraction, pct, status, System.currentTimeMillis())
        FolioWidgetUpdater.requestUpdate(context, throttled = true)
    }

    suspend fun recordReadingTime(bookId: Long, deltaSeconds: Long, forceWidgetUpdate: Boolean = false) = withContext(Dispatchers.IO) {
        val safeDelta = deltaSeconds.coerceIn(0L, 120L)
        if (safeDelta <= 0L) {
            if (forceWidgetUpdate) FolioWidgetUpdater.requestUpdate(context, throttled = false)
            return@withContext
        }
        readingTimeMutex.withLock {
            dao.addReadingSeconds(bookId, safeDelta)
            val now = System.currentTimeMillis()
            val epochDay = (now + java.util.TimeZone.getDefault().getOffset(now)) / 86_400_000L
            val existing = dao.findSession(bookId, epochDay)
            if (existing != null) {
                dao.incrementSession(existing.id, safeDelta)
            } else {
                dao.insertSession(ReadingSessionEntity(bookId = bookId, epochDay = epochDay, durationSeconds = safeDelta))
            }
        }
        FolioWidgetUpdater.requestUpdate(context, throttled = !forceWidgetUpdate)
    }

    suspend fun resetReadingStats() = withContext(Dispatchers.IO) {
        readingTimeMutex.withLock {
            dao.clearAllReadingSessions()
            dao.resetAllBookReadingSeconds()
        }
        FolioWidgetUpdater.requestUpdate(context)
    }

    suspend fun addHighlight(
        bookId: Long,
        chapterIndex: Int,
        paragraphIndex: Int,
        startOffset: Int,
        endOffset: Int,
        quoteText: String,
        noteText: String,
        color: HighlightColor
    ) = withContext(Dispatchers.IO) {
        dao.insertHighlight(
            HighlightEntity(
                bookId = bookId,
                chapterIndex = chapterIndex,
                paragraphIndex = paragraphIndex,
                startOffset = startOffset,
                endOffset = endOffset,
                quoteText = quoteText,
                noteText = noteText.trim(),
                color = color
            )
        )
    }

    suspend fun updateHighlight(highlight: HighlightEntity) = withContext(Dispatchers.IO) {
        dao.updateHighlight(highlight)
    }

    suspend fun deleteHighlight(highlight: HighlightEntity) = withContext(Dispatchers.IO) {
        dao.deleteHighlight(highlight)
    }

    suspend fun toggleBookmark(bookId: Long, unitIndex: Int, label: String, existing: BookmarkEntity?) =
        withContext(Dispatchers.IO) {
            if (existing != null) {
                dao.deleteBookmark(existing)
            } else {
                dao.insertBookmark(BookmarkEntity(bookId = bookId, unitIndex = unitIndex, label = label))
            }
        }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) = withContext(Dispatchers.IO) {
        dao.deleteBookmark(bookmark)
    }

    suspend fun exportBackup(destUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        BackupManager.exportBackupZip(context, dao, destUri)
    }

    suspend fun restoreBackup(srcUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        val res = BackupManager.restoreBackupZip(context, dao, srcUri)
        if (res.isSuccess) FolioWidgetUpdater.requestUpdate(context)
        res
    }

    suspend fun exportBookNotesMarkdown(book: BookEntity, destUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val highlights = dao.getAllHighlightsSnapshot().filter { it.bookId == book.id }
            val md = BackupManager.formatNotesAsMarkdown(book, highlights)
            context.contentResolver.openOutputStream(destUri)?.use { os ->
                os.write(md.toByteArray(Charsets.UTF_8))
            } ?: error("Unable to open output stream")
            "Exported ${highlights.size} notes to Markdown."
        }
    }

    private fun queryFileName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

data class ImportResult(
    val added: Int,
    val skippedDuplicates: Int,
    val unsupported: Int
) {
    fun summaryMessage(): String = buildString {
        append("Imported $added book${if (added == 1) "" else "s"}")
        if (skippedDuplicates > 0) append(" • $skippedDuplicates duplicate${if (skippedDuplicates == 1) "" else "s"} skipped")
        if (unsupported > 0) append(" • $unsupported unsupported")
    }
}
