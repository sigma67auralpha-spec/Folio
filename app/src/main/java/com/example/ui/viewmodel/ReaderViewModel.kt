package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FolioDatabase
import com.example.data.local.LibraryRepository
import com.example.data.local.PreferencesRepository
import com.example.data.model.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.BookmarkEntity
import com.example.data.model.FormatCategory
import com.example.data.model.HighlightColor
import com.example.data.model.HighlightEntity
import com.example.data.model.ReaderPreferences
import com.example.engine.BookChapter
import com.example.engine.BookParser
import com.example.engine.ComicArchiveHelper
import com.example.engine.ParsedBookContent
import com.example.engine.PdfPageHelper
import com.example.engine.TtsManager
import com.example.engine.TtsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SearchMatch(
    val chapterIndex: Int,
    val chapterTitle: String,
    val paragraphIndex: Int,
    val snippet: String
)

sealed interface ReaderContentState {
    data object Loading : ReaderContentState
    data class Reflowable(val chapters: List<BookChapter>) : ReaderContentState
    data class FixedPage(val pageCount: Int, val isComic: Boolean) : ReaderContentState
    data class Error(val message: String) : ReaderContentState
}

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FolioDatabase.getInstance(application)
    private val repository = LibraryRepository(application, db.folioDao())
    private val prefsRepo = PreferencesRepository(application)

    private val _book = MutableStateFlow<BookEntity?>(null)
    val book: StateFlow<BookEntity?> = _book.asStateFlow()

    private val _contentState = MutableStateFlow<ReaderContentState>(ReaderContentState.Loading)
    val contentState: StateFlow<ReaderContentState> = _contentState.asStateFlow()

    private val _currentUnit = MutableStateFlow(0)
    val currentUnit: StateFlow<Int> = _currentUnit.asStateFlow()

    private val _currentPageBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPageBitmap: StateFlow<Bitmap?> = _currentPageBitmap.asStateFlow()

    private val _secondarySpreadBitmap = MutableStateFlow<Bitmap?>(null)
    val secondarySpreadBitmap: StateFlow<Bitmap?> = _secondarySpreadBitmap.asStateFlow()

    private val _highlights = MutableStateFlow<List<HighlightEntity>>(emptyList())
    val highlights: StateFlow<List<HighlightEntity>> = _highlights.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkEntity>> = _bookmarks.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchMatch>>(emptyList())
    val searchResults: StateFlow<List<SearchMatch>> = _searchResults.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val preferences: StateFlow<ReaderPreferences> = prefsRepo.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReaderPreferences()
    )

    val ttsManager = TtsManager(
        context = application,
        scope = viewModelScope,
        onRequestNextChapter = { nextUnit() }
    )
    val ttsState: StateFlow<TtsState> = ttsManager.state

    private var activeBookFile: File? = null
    private var sessionTimerJob: Job? = null
    private var collectorJobs = mutableListOf<Job>()
    private var lastLoadedPdfOptions: Triple<Int, Boolean, Boolean>? = null

    fun loadBook(bookId: Long) {
        collectorJobs.forEach { it.cancel() }
        collectorJobs.clear()
        sessionTimerJob?.cancel()

        viewModelScope.launch {
            _contentState.value = ReaderContentState.Loading
            val loaded = db.folioDao().getBookById(bookId)
            if (loaded == null) {
                _contentState.value = ReaderContentState.Error("Book not found in library.")
                return@launch
            }
            _book.value = loaded
            _currentUnit.value = loaded.currentUnit.coerceAtLeast(0)

            collectorJobs += launch {
                repository.observeHighlights(bookId).collect { _highlights.value = it }
            }
            collectorJobs += launch {
                repository.observeBookmarks(bookId).collect { _bookmarks.value = it }
            }

            val file = repository.resolveBookFile(loaded)
            if (file == null || !file.exists()) {
                _contentState.value = ReaderContentState.Error("Local file is missing. Re-import or relink the source file.")
                return@launch
            }
            activeBookFile = file

            withContext(Dispatchers.IO) {
                when (loaded.format.category) {
                    FormatCategory.REFLOWABLE -> {
                        val imgCache = File(getApplication<Application>().cacheDir, "epub_images/${loaded.fileHash.take(16)}")
                        when (val parsed = BookParser.parseReflowableContent(file, loaded.format, loaded.title, imgCache)) {
                            is ParsedBookContent.Reflowable -> {
                                val safeUnit = loaded.currentUnit.coerceIn(0, (parsed.chapters.size - 1).coerceAtLeast(0))
                                _currentUnit.value = safeUnit
                                _contentState.value = ReaderContentState.Reflowable(parsed.chapters)
                                repository.saveProgress(loaded.id, safeUnit, parsed.chapters.size, loaded.scrollFraction)
                            }
                            is ParsedBookContent.Error -> {
                                _contentState.value = ReaderContentState.Error(parsed.message)
                            }
                        }
                    }
                    FormatCategory.PDF -> {
                        val pageCount = PdfPageHelper.getPageCount(file)
                        val safeUnit = loaded.currentUnit.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                        _currentUnit.value = safeUnit
                        _contentState.value = ReaderContentState.FixedPage(pageCount, isComic = false)
                        renderFixedPage(safeUnit)
                    }
                    FormatCategory.COMIC -> {
                        val count = if (loaded.format == BookFormat.CBZ) {
                            runCatching {
                                java.util.zip.ZipFile(file).use { ComicArchiveHelper.listCbzImageNames(it).size }
                            }.getOrDefault(0)
                        } else {
                            ComicArchiveHelper.listRarStoredImages(file).size
                        }
                        if (count <= 0) {
                            _contentState.value = ReaderContentState.Error(
                                if (loaded.format == BookFormat.CBR) {
                                    "This CBR uses compressed RAR blocks. Uncompressed CBR and CBZ archives are supported natively."
                                } else {
                                    "No comic image pages found inside archive."
                                }
                            )
                        } else {
                            val safeUnit = loaded.currentUnit.coerceIn(0, count - 1)
                            _currentUnit.value = safeUnit
                            _contentState.value = ReaderContentState.FixedPage(count, isComic = true)
                            renderFixedPage(safeUnit)
                        }
                    }
                }
            }

            startReadingSessionTracker(loaded.id)
        }
    }

    private fun startReadingSessionTracker(bookId: Long) {
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch {
            delay(2_000L)
            repository.recordReadingTime(bookId, 2L)
            while (isActive) {
                delay(15_000L)
                repository.recordReadingTime(bookId, 15L)
            }
        }
    }

    fun renderFixedPage(pageIndex: Int, forceRefresh: Boolean = false) {
        val file = activeBookFile ?: return
        val currentBook = _book.value ?: return
        val prefs = preferences.value
        val state = _contentState.value as? ReaderContentState.FixedPage ?: return
        val safePage = pageIndex.coerceIn(0, (state.pageCount - 1).coerceAtLeast(0))
        _currentUnit.value = safePage

        if (!state.isComic) {
            val key = Triple(safePage, prefs.pdfMarginCrop, prefs.pdfDarkInvert)
            if (!forceRefresh && lastLoadedPdfOptions == key && _currentPageBitmap.value != null) return
            lastLoadedPdfOptions = key
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (!state.isComic) {
                val bmp = PdfPageHelper.renderPage(
                    file = file,
                    pageIndex = safePage,
                    targetWidth = 1280,
                    cropMargins = prefs.pdfMarginCrop,
                    darkInvert = prefs.pdfDarkInvert
                )
                _currentPageBitmap.value = bmp
                _secondarySpreadBitmap.value = null
            } else {
                val loader: (Int) -> Bitmap? = { idx ->
                    if (currentBook.format == BookFormat.CBZ) {
                        ComicArchiveHelper.loadCbzPageBitmap(file, idx)
                    } else {
                        ComicArchiveHelper.loadCbrPageBitmap(file, idx)
                    }
                }
                val primary = loader(safePage)
                val secondary = if (prefs.comicDoubleSpread && safePage + 1 < state.pageCount) {
                    loader(safePage + 1)
                } else null
                _currentPageBitmap.value = primary
                _secondarySpreadBitmap.value = secondary
            }
            repository.saveProgress(currentBook.id, safePage, state.pageCount, if (safePage == state.pageCount - 1) 1f else 0f)
        }
    }

    fun selectChapter(chapterIndex: Int, scrollFraction: Float = 0f) {
        val currentBook = _book.value ?: return
        when (val st = _contentState.value) {
            is ReaderContentState.Reflowable -> {
                val safeIdx = chapterIndex.coerceIn(0, (st.chapters.size - 1).coerceAtLeast(0))
                _currentUnit.value = safeIdx
                viewModelScope.launch {
                    repository.saveProgress(currentBook.id, safeIdx, st.chapters.size, scrollFraction)
                }
                if (ttsState.value.isSpeaking) {
                    val blocks = st.chapters.getOrNull(safeIdx)?.blocks?.map { it.text }.orEmpty()
                    ttsManager.startReading(blocks, 0)
                }
            }
            is ReaderContentState.FixedPage -> {
                val step = if (st.isComic && preferences.value.comicDoubleSpread) 2 else 1
                renderFixedPage(chapterIndex.coerceIn(0, (st.pageCount - 1).coerceAtLeast(0)))
            }
            else -> Unit
        }
    }

    fun saveScrollProgress(scrollFraction: Float) {
        val currentBook = _book.value ?: return
        val total = when (val st = _contentState.value) {
            is ReaderContentState.Reflowable -> st.chapters.size
            is ReaderContentState.FixedPage -> st.pageCount
            else -> 1
        }
        viewModelScope.launch {
            repository.saveProgress(currentBook.id, _currentUnit.value, total, scrollFraction)
        }
    }

    fun nextUnit() {
        val step = if ((_contentState.value as? ReaderContentState.FixedPage)?.isComic == true && preferences.value.comicDoubleSpread) 2 else 1
        selectChapter(_currentUnit.value + step)
    }

    fun previousUnit() {
        val step = if ((_contentState.value as? ReaderContentState.FixedPage)?.isComic == true && preferences.value.comicDoubleSpread) 2 else 1
        selectChapter((_currentUnit.value - step).coerceAtLeast(0))
    }

    fun toggleBookmarkCurrent() {
        val currentBook = _book.value ?: return
        val unit = _currentUnit.value
        val existing = _bookmarks.value.firstOrNull { it.unitIndex == unit }
        val label = when (val st = _contentState.value) {
            is ReaderContentState.Reflowable -> st.chapters.getOrNull(unit)?.title ?: "Chapter ${unit + 1}"
            is ReaderContentState.FixedPage -> "Page ${unit + 1}"
            else -> "Location ${unit + 1}"
        }
        viewModelScope.launch {
            repository.toggleBookmark(currentBook.id, unit, label, existing)
            _toastMessage.value = if (existing == null) "Bookmark saved" else "Bookmark removed"
        }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch { repository.deleteBookmark(bookmark) }
    }

    fun addHighlight(
        chapterIndex: Int,
        paragraphIndex: Int,
        quoteText: String,
        noteText: String,
        color: HighlightColor
    ) {
        val currentBook = _book.value ?: return
        viewModelScope.launch {
            repository.addHighlight(
                bookId = currentBook.id,
                chapterIndex = chapterIndex,
                paragraphIndex = paragraphIndex,
                startOffset = 0,
                endOffset = quoteText.length,
                quoteText = quoteText,
                noteText = noteText,
                color = color
            )
            _toastMessage.value = if (noteText.isBlank()) "Highlight saved" else "Highlight & note saved"
        }
    }

    fun updateHighlight(highlight: HighlightEntity) {
        viewModelScope.launch {
            repository.updateHighlight(highlight)
            _toastMessage.value = "Note updated"
        }
    }

    fun deleteHighlight(highlight: HighlightEntity) {
        viewModelScope.launch { repository.deleteHighlight(highlight) }
    }

    fun searchInBook(query: String) {
        val st = _contentState.value as? ReaderContentState.Reflowable ?: return
        val q = query.trim()
        if (q.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val matches = mutableListOf<SearchMatch>()
            for (chapter in st.chapters) {
                chapter.blocks.forEachIndexed { pIdx, block ->
                    val hitIdx = block.text.indexOf(q, ignoreCase = true)
                    if (hitIdx >= 0) {
                        val start = (hitIdx - 28).coerceAtLeast(0)
                        val end = (hitIdx + q.length + 52).coerceAtMost(block.text.length)
                        val prefix = if (start > 0) "…" else ""
                        val suffix = if (end < block.text.length) "…" else ""
                        matches.add(
                            SearchMatch(
                                chapterIndex = chapter.index,
                                chapterTitle = chapter.title,
                                paragraphIndex = pIdx,
                                snippet = "$prefix${block.text.substring(start, end)}$suffix"
                            )
                        )
                    }
                    if (matches.size >= 40) return@launch.also { _searchResults.value = matches }
                }
            }
            _searchResults.value = matches
        }
    }

    fun clearSearch() { _searchResults.value = emptyList() }
    fun clearToast() { _toastMessage.value = null }

    fun toggleTts(startParagraphIndex: Int = 0) {
        val st = _contentState.value as? ReaderContentState.Reflowable ?: return
        if (ttsState.value.isSpeaking) {
            ttsManager.stop()
        } else {
            val prefs = preferences.value
            ttsManager.updateVoiceParams(prefs.ttsRate, prefs.ttsPitch)
            val blocks = st.chapters.getOrNull(_currentUnit.value)?.blocks?.map { it.text }.orEmpty()
            if (blocks.isNotEmpty()) {
                ttsManager.startReading(blocks, startParagraphIndex)
            } else {
                _toastMessage.value = "No readable text in this chapter."
            }
        }
    }

    fun updatePreferences(transform: (ReaderPreferences) -> ReaderPreferences) {
        viewModelScope.launch {
            prefsRepo.update(transform)
            val updated = preferences.value
            ttsManager.updateVoiceParams(updated.ttsRate, updated.ttsPitch)
            if (_contentState.value is ReaderContentState.FixedPage) {
                renderFixedPage(_currentUnit.value, forceRefresh = true)
            }
        }
    }

    fun exportNotes(destUri: Uri) {
        val currentBook = _book.value ?: return
        viewModelScope.launch {
            val res = repository.exportBookNotesMarkdown(currentBook, destUri)
            _toastMessage.value = res.getOrElse { "Export failed: ${it.localizedMessage}" }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionTimerJob?.cancel()
        ttsManager.release()
    }
}
