package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FolioDatabase
import com.example.data.local.LibraryRepository
import com.example.data.local.PreferencesRepository
import com.example.data.model.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.BookWithRelations
import com.example.data.model.FormatCategory
import com.example.data.model.LibrarySortOrder
import com.example.data.model.LibraryViewMode
import com.example.data.model.ReadStatus
import com.example.data.model.ReaderPreferences
import com.example.data.model.ReadingSessionEntity
import com.example.data.model.ShelfEntity
import com.example.data.model.TagEntity
import com.example.data.model.WatchedFolderEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryFilterChip(val label: String) {
    ALL("All"),
    READING("Reading"),
    UNREAD("Unread"),
    FINISHED("Finished"),
    EPUB("EPUB"),
    PDF("PDF"),
    COMICS("Comics"),
    DOCS("Docs & Text")
}

data class LibraryUiState(
    val allBooks: List<BookWithRelations> = emptyList(),
    val filteredBooks: List<BookWithRelations> = emptyList(),
    val continueReadingBook: BookWithRelations? = null,
    val shelves: List<ShelfEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val sessions: List<ReadingSessionEntity> = emptyList(),
    val watchedFolders: List<WatchedFolderEntity> = emptyList(),
    val preferences: ReaderPreferences = ReaderPreferences(),
    val searchQuery: String = "",
    val activeFilter: LibraryFilterChip = LibraryFilterChip.ALL,
    val activeTagFilter: String? = null,
    val isImporting: Boolean = false,
    val statusMessage: String? = null
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FolioDatabase.getInstance(application)
    val repository = LibraryRepository(application, db.folioDao())
    val prefsRepository = PreferencesRepository(application)

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(LibraryFilterChip.ALL)
    private val _activeTagFilter = MutableStateFlow<String?>(null)
    private val _isImporting = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.ensureStarterLibraryIfEmpty()
        }
    }

    private data class CoreFlows(
        val books: List<BookWithRelations>,
        val shelves: List<ShelfEntity>,
        val tags: List<TagEntity>,
        val sessions: List<ReadingSessionEntity>,
        val folders: List<WatchedFolderEntity>
    )

    private val coreFlows = combine(
        repository.booksWithRelations,
        repository.shelves,
        repository.tags,
        repository.sessions,
        repository.watchedFolders
    ) { books, shelves, tags, sessions, folders ->
        CoreFlows(books, shelves, tags, sessions, folders)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        coreFlows,
        prefsRepository.preferencesFlow,
        _searchQuery,
        _activeFilter,
        _activeTagFilter,
        _isImporting,
        _statusMessage
    ) { arr ->
        val core = arr[0] as CoreFlows
        val prefs = arr[1] as ReaderPreferences
        val query = arr[2] as String
        val filter = arr[3] as LibraryFilterChip
        val tagFilter = arr[4] as String?
        val importing = arr[5] as Boolean
        val message = arr[6] as String?

        val filtered = applyFilterAndSort(core.books, query, filter, tagFilter, prefs.librarySortOrder)
        val continueBook = core.books
            .filter { it.book.lastOpenedAt != null && it.book.readStatus != ReadStatus.FINISHED }
            .maxByOrNull { it.book.lastOpenedAt ?: 0L }
            ?: core.books.firstOrNull { it.book.lastOpenedAt != null }

        LibraryUiState(
            allBooks = core.books,
            filteredBooks = filtered,
            continueReadingBook = continueBook,
            shelves = core.shelves,
            tags = core.tags,
            sessions = core.sessions,
            watchedFolders = core.folders,
            preferences = prefs,
            searchQuery = query,
            activeFilter = filter,
            activeTagFilter = tagFilter,
            isImporting = importing,
            statusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    private fun applyFilterAndSort(
        books: List<BookWithRelations>,
        query: String,
        filter: LibraryFilterChip,
        tagFilter: String?,
        sortOrder: LibrarySortOrder
    ): List<BookWithRelations> {
        val q = query.trim().lowercase()
        val matched = books.filter { item ->
            val b = item.book
            val matchesQuery = q.isEmpty() ||
                b.title.lowercase().contains(q) ||
                b.author.lowercase().contains(q) ||
                b.series.lowercase().contains(q) ||
                item.tags.any { it.name.lowercase().contains(q) }

            val matchesFilter = when (filter) {
                LibraryFilterChip.ALL -> true
                LibraryFilterChip.READING -> b.readStatus == ReadStatus.READING
                LibraryFilterChip.UNREAD -> b.readStatus == ReadStatus.UNREAD
                LibraryFilterChip.FINISHED -> b.readStatus == ReadStatus.FINISHED
                LibraryFilterChip.EPUB -> b.format == BookFormat.EPUB
                LibraryFilterChip.PDF -> b.format == BookFormat.PDF
                LibraryFilterChip.COMICS -> b.format.category == FormatCategory.COMIC
                LibraryFilterChip.DOCS -> b.format in listOf(
                    BookFormat.MOBI, BookFormat.AZW3, BookFormat.FB2,
                    BookFormat.DOCX, BookFormat.RTF, BookFormat.TXT,
                    BookFormat.HTML, BookFormat.MARKDOWN
                )
            }

            val matchesTag = tagFilter == null || item.tags.any { it.name.equals(tagFilter, ignoreCase = true) }
            matchesQuery && matchesFilter && matchesTag
        }

        return when (sortOrder) {
            LibrarySortOrder.LAST_OPENED -> matched.sortedByDescending { it.book.lastOpenedAt ?: it.book.addedAt }
            LibrarySortOrder.ADDED_DESC -> matched.sortedByDescending { it.book.addedAt }
            LibrarySortOrder.TITLE_ASC -> matched.sortedBy { it.book.title.lowercase() }
            LibrarySortOrder.AUTHOR_ASC -> matched.sortedBy { it.book.author.lowercase() }
            LibrarySortOrder.PROGRESS_DESC -> matched.sortedByDescending { it.book.progressPercent }
        }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setFilter(filter: LibraryFilterChip) { _activeFilter.value = filter }
    fun setTagFilter(tag: String?) { _activeTagFilter.value = if (_activeTagFilter.value == tag) null else tag }
    fun clearStatusMessage() { _statusMessage.value = null }

    fun importFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isImporting.value = true
            val res = repository.importDocumentUris(uris)
            _isImporting.value = false
            _statusMessage.value = res.summaryMessage()
        }
    }

    fun linkFolder(treeUri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            val res = repository.linkAndScanFolder(treeUri)
            _isImporting.value = false
            _statusMessage.value = res.summaryMessage()
        }
    }

    fun rescanFolder(folder: WatchedFolderEntity) {
        viewModelScope.launch {
            _isImporting.value = true
            val res = repository.rescanFolder(folder)
            _isImporting.value = false
            _statusMessage.value = "${folder.displayName}: ${res.summaryMessage()}"
        }
    }

    fun removeWatchedFolder(folder: WatchedFolderEntity) {
        viewModelScope.launch { repository.removeWatchedFolder(folder) }
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            prefsRepository.update {
                it.copy(
                    libraryViewMode = if (it.libraryViewMode == LibraryViewMode.GRID)
                        LibraryViewMode.LIST else LibraryViewMode.GRID
                )
            }
        }
    }

    fun setSortOrder(order: LibrarySortOrder) {
        viewModelScope.launch { prefsRepository.update { it.copy(librarySortOrder = order) } }
    }

    fun updatePreferences(transform: (ReaderPreferences) -> ReaderPreferences) {
        viewModelScope.launch { prefsRepository.update(transform) }
    }

    fun importCustomFont(uri: Uri) {
        viewModelScope.launch {
            val imported = repository.importCustomFont(uri)
            if (imported != null) {
                prefsRepository.update {
                    it.copy(
                        readerFont = com.example.data.model.ReaderFont.CUSTOM_FILE,
                        customFontPath = imported.first,
                        customFontName = imported.second
                    )
                }
                _statusMessage.value = "Loaded custom font: ${imported.second}"
            } else {
                _statusMessage.value = "Please pick a valid .ttf or .otf font file."
            }
        }
    }

    fun updateBookMetadata(
        book: BookEntity,
        title: String,
        author: String,
        series: String,
        seriesIndex: Float,
        description: String,
        coverUri: Uri?,
        tags: List<String>
    ) {
        viewModelScope.launch {
            repository.updateMetadata(book, title, author, series, seriesIndex, description, coverUri, tags)
            _statusMessage.value = "Updated metadata for \"$title\""
        }
    }

    fun setBookShelves(bookId: Long, shelfIds: Set<Long>) {
        viewModelScope.launch {
            repository.setBookShelves(bookId, shelfIds)
            _statusMessage.value = "Updated shelves"
        }
    }

    fun createShelf(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createShelf(name) }
    }

    fun renameShelf(shelf: ShelfEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repository.renameShelf(shelf, newName) }
    }

    fun deleteShelf(shelf: ShelfEntity) {
        viewModelScope.launch { repository.deleteShelf(shelf) }
    }

    fun deleteBook(book: BookEntity, deleteLocalFile: Boolean) {
        viewModelScope.launch {
            repository.deleteBook(book, deleteLocalFile)
            _statusMessage.value = "Removed \"${book.title}\""
        }
    }

    fun reparseBook(book: BookEntity) {
        viewModelScope.launch {
            val ok = repository.reparseBookMetadata(book)
            _statusMessage.value = if (ok) "Refreshed \"${book.title}\"" else "Local file is missing."
        }
    }

    fun exportBackup(destUri: Uri) {
        viewModelScope.launch {
            val res = repository.exportBackup(destUri)
            _statusMessage.value = res.getOrElse { "Backup failed: ${it.localizedMessage}" }
        }
    }

    fun restoreBackup(srcUri: Uri) {
        viewModelScope.launch {
            val res = repository.restoreBackup(srcUri)
            _statusMessage.value = res.getOrElse { "Restore failed: ${it.localizedMessage}" }
        }
    }

    fun exportNotesMarkdown(book: BookEntity, destUri: Uri) {
        viewModelScope.launch {
            val res = repository.exportBookNotesMarkdown(book, destUri)
            _statusMessage.value = res.getOrElse { "Export failed: ${it.localizedMessage}" }
        }
    }
}
