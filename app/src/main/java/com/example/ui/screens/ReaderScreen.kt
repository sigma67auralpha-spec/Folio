package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.model.FormatCategory
import com.example.data.model.HighlightEntity
import com.example.data.model.OrientationLock
import com.example.data.model.PageAnimation
import com.example.data.model.ReaderFont
import com.example.data.model.ReaderPreferences
import com.example.data.model.ReaderThemeMode
import com.example.data.model.TapZonePreset
import com.example.engine.BlockStyle
import com.example.engine.TextRun
import com.example.ui.components.HighlightNoteDialog
import com.example.ui.components.ReaderControlsSheet
import com.example.ui.components.ReaderIndexAndNotesSheet
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.MerriweatherFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.ReaderAmoledBg
import com.example.ui.theme.ReaderAmoledInk
import com.example.ui.theme.ReaderPaperBg
import com.example.ui.theme.ReaderPaperInk
import com.example.ui.theme.ReaderSepiaBg
import com.example.ui.theme.ReaderSepiaInk
import com.example.ui.theme.ReaderSlateBg
import com.example.ui.theme.ReaderSlateInk
import com.example.ui.viewmodel.ReaderContentState
import com.example.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    bookId: Long,
    onBack: () -> Unit,
    readerViewModel: ReaderViewModel = viewModel()
) {
    LaunchedEffect(bookId) {
        readerViewModel.loadBook(bookId)
    }

    val book by readerViewModel.book.collectAsStateWithLifecycle()
    val contentState by readerViewModel.contentState.collectAsStateWithLifecycle()
    val currentUnit by readerViewModel.currentUnit.collectAsStateWithLifecycle()
    val prefs by readerViewModel.preferences.collectAsStateWithLifecycle()
    val highlights by readerViewModel.highlights.collectAsStateWithLifecycle()
    val bookmarks by readerViewModel.bookmarks.collectAsStateWithLifecycle()
    val ttsState by readerViewModel.ttsState.collectAsStateWithLifecycle()
    val pageBitmap by readerViewModel.currentPageBitmap.collectAsStateWithLifecycle()
    val spreadBitmap by readerViewModel.secondarySpreadBitmap.collectAsStateWithLifecycle()
    val searchResults by readerViewModel.searchResults.collectAsStateWithLifecycle()
    val toastMsg by readerViewModel.toastMessage.collectAsStateWithLifecycle()

    var showOverlay by remember { mutableStateOf(false) }
    var showControlsSheet by remember { mutableStateOf(false) }
    var showIndexSheet by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var pendingHighlightTarget by remember { mutableStateOf<Triple<Int, Int, String>?>(null) }
    var editingHighlight by remember { mutableStateOf<HighlightEntity?>(null) }
    var brightnessToast by remember { mutableStateOf<Float?>(null) }
    var restoredInitialScroll by remember(bookId) { mutableStateOf(false) }
    var previousUnit by remember(bookId) { mutableIntStateOf(-1) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val view = LocalView.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var clockText by remember { mutableStateOf("") }

    val exportNotesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) readerViewModel.exportNotes(uri)
    }

    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            snackbarHostState.showSnackbar(it)
            readerViewModel.clearToast()
        }
    }

    LaunchedEffect(prefs.showClockInReader) {
        if (!prefs.showClockInReader) {
            clockText = ""
        } else {
            val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            while (true) {
                clockText = fmt.format(Date())
                delay(30_000L)
            }
        }
    }

    LaunchedEffect(prefs.enableTts) {
        if (!prefs.enableTts && ttsState.isSpeaking) {
            readerViewModel.ttsManager.stop()
        }
    }

    DisposableEffect(prefs.keepScreenAwake) {
        view.keepScreenOn = prefs.keepScreenAwake
        onDispose { view.keepScreenOn = false }
    }

    DisposableEffect(prefs.orientationLock) {
        val activity = context as? Activity
        if (activity != null) {
            activity.requestedOrientation = when (prefs.orientationLock) {
                OrientationLock.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                OrientationLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                OrientationLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(prefs.screenBrightness) {
        val window = (context as? Activity)?.window
        val original = window?.attributes?.screenBrightness ?: -1f
        if (window != null && prefs.screenBrightness >= 0f) {
            val lp = window.attributes
            lp.screenBrightness = prefs.screenBrightness.coerceIn(0.05f, 1f)
            window.attributes = lp
        }
        onDispose {
            if (window != null) {
                val lp = window.attributes
                lp.screenBrightness = original
                window.attributes = lp
            }
        }
    }

    val scrollFraction by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total <= 1) 0f else (listState.firstVisibleItemIndex.toFloat() / (total - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
        }
    }

    BackHandler {
        readerViewModel.saveScrollProgress(scrollFraction)
        readerViewModel.ttsManager.stop()
        onBack()
    }

    val (bgColor, inkColor) = resolveReaderTheme(prefs, book?.format?.category)
    val readerFontFamily = remember(prefs.readerFont, prefs.customFontPath) {
        resolveReaderFont(prefs)
    }
    val isBookmarked = bookmarks.any { it.unitIndex == currentUnit }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .focusRequester(focusRequester)
            .onKeyEvent { ev ->
                if (prefs.volumeKeyPaging && ev.type == KeyEventType.KeyDown) {
                    when (ev.key) {
                        Key.VolumeDown -> {
                            readerViewModel.nextUnit()
                            true
                        }
                        Key.VolumeUp -> {
                            readerViewModel.previousUnit()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        when (val state = contentState) {
            is ReaderContentState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = inkColor)
                }
            }
            is ReaderContentState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message, style = MaterialTheme.typography.bodyLarge, color = inkColor, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Back to Library") }
                }
            }
            is ReaderContentState.Reflowable -> {
                LaunchedEffect(currentUnit, state.chapters.size) {
                    val activeChapter = state.chapters.getOrNull(currentUnit)
                    if (!restoredInitialScroll && activeChapter != null && book != null) {
                        restoredInitialScroll = true
                        val savedFrac = book?.scrollFraction ?: 0f
                        val targetItem = (savedFrac * (activeChapter.blocks.size - 1).coerceAtLeast(0)).roundToInt()
                        if (targetItem > 0) listState.scrollToItem(targetItem)
                        previousUnit = currentUnit
                    } else if (previousUnit != -1 && previousUnit != currentUnit) {
                        listState.scrollToItem(0)
                        previousUnit = currentUnit
                    }
                }

                LaunchedEffect(ttsState.currentParagraphIndex) {
                    val idx = ttsState.currentParagraphIndex
                    if (ttsState.isSpeaking && idx >= 0) {
                        listState.animateScrollToItem(idx)
                    }
                }

                val transitionForward = currentUnit >= previousUnit
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .pointerInput(prefs.tapZonePreset) {
                            detectTapGestures { offset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                when (prefs.tapZonePreset) {
                                    TapZonePreset.CLASSIC_LR -> when {
                                        offset.x < w * 0.28f -> readerViewModel.previousUnit()
                                        offset.x > w * 0.72f -> readerViewModel.nextUnit()
                                        else -> showOverlay = !showOverlay
                                    }
                                    TapZonePreset.ONE_HAND_RIGHT -> when {
                                        offset.y < h * 0.30f -> readerViewModel.previousUnit()
                                        offset.x > w * 0.65f || offset.y > h * 0.72f -> readerViewModel.nextUnit()
                                        else -> showOverlay = !showOverlay
                                    }
                                    TapZonePreset.SCROLL_ONLY -> showOverlay = !showOverlay
                                }
                            }
                        }
                ) {
                    AnimatedContent(
                        targetState = currentUnit,
                        transitionSpec = {
                            when (prefs.pageAnimation) {
                                PageAnimation.NONE -> fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                                PageAnimation.FADE -> fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                                PageAnimation.SLIDE -> {
                                    if (targetState >= initialState) {
                                        (slideInHorizontally(tween(240)) { it / 4 } + fadeIn(tween(200))) togetherWith
                                            (slideOutHorizontally(tween(200)) { -it / 4 } + fadeOut(tween(160)))
                                    } else {
                                        (slideInHorizontally(tween(240)) { -it / 4 } + fadeIn(tween(200))) togetherWith
                                            (slideOutHorizontally(tween(200)) { it / 4 } + fadeOut(tween(160)))
                                    }
                                }
                            }
                        },
                        label = "chapter_transition"
                    ) { targetUnit ->
                        val chapter = state.chapters.getOrNull(targetUnit) ?: state.chapters.first()
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(
                                    start = prefs.horizontalMarginDp.dp,
                                    end = prefs.horizontalMarginDp.dp,
                                    top = prefs.verticalPaddingDp.dp + 12.dp,
                                    bottom = 78.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(prefs.paragraphSpacingDp.dp),
                                modifier = Modifier
                                    .widthIn(max = prefs.maxColumnWidthDp.dp)
                                    .align(Alignment.TopCenter)
                                    .fillMaxHeight()
                            ) {
                                itemsIndexed(chapter.blocks) { pIdx, block ->
                                    val paraHighlights = highlights.filter {
                                        it.chapterIndex == chapter.index && it.paragraphIndex == pIdx
                                    }
                                    val isTtsActive = ttsState.isSpeaking && ttsState.currentParagraphIndex == pIdx
                                    if (block.style == BlockStyle.IMAGE && block.imagePath != null) {
                                        val imgFile = File(block.imagePath)
                                        if (imgFile.exists()) {
                                            AsyncImage(
                                                model = imgFile,
                                                contentDescription = block.text,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 360.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        }
                                    } else {
                                        Column {
                                            ReflowableBlockView(
                                                text = block.text,
                                                style = block.style,
                                                runs = block.runs,
                                                highlights = if (prefs.enableHighlights) paraHighlights else emptyList(),
                                                isTtsHighlighted = isTtsActive,
                                                prefs = prefs,
                                                fontFamily = readerFontFamily,
                                                inkColor = inkColor,
                                                onLongPress = {
                                                    if (prefs.enableHighlights) {
                                                        pendingHighlightTarget = Triple(chapter.index, pIdx, block.text)
                                                    }
                                                }
                                            )
                                            if (prefs.enableHighlights) {
                                                paraHighlights.filter { it.noteText.isNotBlank() }.forEach { hl ->
                                                    Spacer(Modifier.height(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(hl.color.hex).copy(alpha = 0.28f),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { editingHighlight = hl }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                Icons.Outlined.EditNote,
                                                                contentDescription = "Margin note",
                                                                tint = inkColor.copy(alpha = 0.75f),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(
                                                                text = hl.noteText,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = inkColor.copy(alpha = 0.88f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        if (targetUnit > 0) {
                                            TextButton(
                                                onClick = { readerViewModel.previousUnit() }
                                            ) { Text("← Previous", color = inkColor) }
                                        } else Spacer(Modifier.width(1.dp))
                                        if (targetUnit < state.chapters.lastIndex) {
                                            TextButton(
                                                onClick = { readerViewModel.nextUnit() }
                                            ) { Text("Next Chapter →", color = inkColor) }
                                        }
                                    }
                                }
                            }

                            // Subtle modular footer + progress bar
                            val totalChapters = state.chapters.size.coerceAtLeast(1)
                            val overallPct = (((targetUnit + scrollFraction) / totalChapters.toFloat()) * 100f).coerceIn(0f, 100f)
                            val chapterWords = remember(chapter.index, chapter.blocks.size) {
                                chapter.blocks.sumOf { b ->
                                    if (b.text.isBlank()) 0 else b.text.trim().split(Regex("\\s+")).size
                                }
                            }
                            val minsRemaining = ((chapterWords * (1f - scrollFraction)) / 220f).roundToInt().coerceAtLeast(1)
                            if (prefs.showReaderFooter || prefs.showProgressBars) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                ) {
                                    if (prefs.showReaderFooter) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = chapter.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = inkColor.copy(alpha = 0.55f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            val rightMeta = buildList {
                                                if (prefs.showEtaInReader && chapterWords > 40) add("~${minsRemaining}m")
                                                add("${overallPct.roundToInt()}%")
                                                add("${targetUnit + 1}/${state.chapters.size}")
                                                if (prefs.showClockInReader && clockText.isNotBlank()) add(clockText)
                                            }.joinToString(" • ")
                                            Text(
                                                text = rightMeta,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = inkColor.copy(alpha = 0.55f)
                                            )
                                        }
                                    }
                                    if (prefs.showProgressBars) {
                                        LinearProgressIndicator(
                                            progress = { overallPct / 100f },
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                            trackColor = inkColor.copy(alpha = 0.08f),
                                            modifier = Modifier.fillMaxWidth().height(2.5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (!transitionForward && false) { Unit } // keep compiler clean
                }
            }
            is ReaderContentState.FixedPage -> {
                FixedPageCanvas(
                    primaryBitmap = pageBitmap,
                    secondaryBitmap = spreadBitmap,
                    isComic = state.isComic,
                    comicRtl = prefs.comicRtl,
                    onTapCenter = { showOverlay = !showOverlay },
                    onNext = { readerViewModel.nextUnit() },
                    onPrevious = { readerViewModel.previousUnit() },
                    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                )

                val pct = if (state.pageCount <= 1) 100f else ((currentUnit + 1) * 100f / state.pageCount)
                if (prefs.showReaderFooter || prefs.showProgressBars) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .fillMaxWidth()
                    ) {
                        if (prefs.showReaderFooter) {
                            val pageMeta = buildList {
                                add("Page ${currentUnit + 1} of ${state.pageCount}")
                                add("${pct.roundToInt()}%")
                                if (prefs.showClockInReader && clockText.isNotBlank()) add(clockText)
                            }.joinToString(" • ")
                            Text(
                                text = pageMeta,
                                style = MaterialTheme.typography.labelSmall,
                                color = inkColor.copy(alpha = 0.65f),
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp)
                            )
                        }
                        if (prefs.showProgressBars) {
                            LinearProgressIndicator(
                                progress = { (pct / 100f).coerceIn(0f, 1f) },
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                                trackColor = inkColor.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth().height(2.5.dp)
                            )
                        }
                    }
                }
            }
        }

        // Left-edge vertical swipe brightness zone
        if (prefs.swipeBrightness) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(26.dp)
                    .fillMaxHeight(0.7f)
                    .pointerInput(Unit) {
                        var currentB = if (prefs.screenBrightness < 0f) 0.5f else prefs.screenBrightness
                        detectVerticalDragGestures(
                            onDragEnd = { brightnessToast = null },
                            onDragCancel = { brightnessToast = null }
                        ) { _, dragAmount ->
                            currentB = (currentB - dragAmount / 600f).coerceIn(0.05f, 1f)
                            brightnessToast = currentB
                            readerViewModel.updatePreferences { it.copy(screenBrightness = currentB) }
                        }
                    }
            )
        }

        brightnessToast?.let { b ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 4.dp,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.BrightnessMedium, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("${(b * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        AnimatedVisibility(
            visible = ttsState.isSpeaking,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (showOverlay) 120.dp else 36.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { readerViewModel.ttsManager.prevParagraph() }) {
                        Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous paragraph")
                    }
                    Text(
                        text = if (ttsState.sleepMinutesRemaining > 0)
                            "Speaking • ${ttsState.sleepMinutesRemaining}m"
                        else "Speaking",
                        style = MaterialTheme.typography.labelMedium
                    )
                    IconButton(onClick = { readerViewModel.ttsManager.nextParagraph() }) {
                        Icon(Icons.Outlined.SkipNext, contentDescription = "Next paragraph")
                    }
                    IconButton(onClick = { readerViewModel.ttsManager.stop() }) {
                        Icon(Icons.Outlined.Stop, contentDescription = "Stop reading aloud")
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                readerViewModel.saveScrollProgress(scrollFraction)
                                readerViewModel.ttsManager.stop()
                                onBack()
                            },
                            modifier = Modifier.testTag("reader_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to library")
                        }
                        Text(
                            text = book?.title ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (contentState is ReaderContentState.Reflowable) {
                            IconButton(
                                onClick = { showSearchBar = !showSearchBar },
                                modifier = Modifier.testTag("reader_search_button")
                            ) {
                                Icon(Icons.Outlined.Search, contentDescription = "Search inside book")
                            }
                            if (prefs.enableTts) {
                                IconButton(
                                    onClick = { readerViewModel.toggleTts() },
                                    modifier = Modifier.testTag("reader_tts_button")
                                ) {
                                    Icon(Icons.Outlined.Headphones, contentDescription = "Toggle text-to-speech")
                                }
                            }
                        }
                        IconButton(
                            onClick = { readerViewModel.toggleBookmarkCurrent() },
                            modifier = Modifier.testTag("reader_bookmark_button")
                        ) {
                            Icon(
                                if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Toggle bookmark"
                            )
                        }
                    }

                    if (showSearchBar) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    readerViewModel.searchInBook(it)
                                },
                                placeholder = { Text("Find in book…") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        showSearchBar = false
                                        searchQuery = ""
                                        readerViewModel.clearSearch()
                                    }) { Icon(Icons.Outlined.Close, contentDescription = "Close search") }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("in_book_search_input")
                            )
                        }
                        if (searchResults.isNotEmpty()) {
                            LazyColumn(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                                items(searchResults) { match ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                readerViewModel.selectChapter(match.chapterIndex)
                                                coroutineScope.launch {
                                                    listState.scrollToItem(match.paragraphIndex)
                                                }
                                                showSearchBar = false
                                                showOverlay = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(match.chapterTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        Text(match.snippet, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val totalUnits = when (val st = contentState) {
                    is ReaderContentState.Reflowable -> st.chapters.size
                    is ReaderContentState.FixedPage -> st.pageCount
                    else -> 1
                }.coerceAtLeast(1)

                Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    if (totalUnits > 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { readerViewModel.previousUnit() }) {
                                Icon(Icons.AutoMirrored.Outlined.NavigateBefore, contentDescription = "Previous")
                            }
                            Slider(
                                value = currentUnit.toFloat(),
                                onValueChange = { readerViewModel.selectChapter(it.roundToInt()) },
                                valueRange = 0f..(totalUnits - 1).toFloat(),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { readerViewModel.nextUnit() }) {
                                Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = "Next")
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = { showIndexSheet = true },
                            modifier = Modifier.testTag("reader_toc_notes_button")
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.List, contentDescription = "Contents, Notes, and Bookmarks")
                        }
                        IconButton(
                            onClick = { showControlsSheet = true },
                            modifier = Modifier.testTag("reader_controls_aa_button")
                        ) {
                            Icon(Icons.Outlined.TextFields, contentDescription = "Reading appearance and controls")
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        )

        if (prefs.warmthTint > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFF9800).copy(alpha = (prefs.warmthTint * 0.42f).coerceIn(0f, 0.22f)))
            )
        }
    }

    if (showControlsSheet) {
        ReaderControlsSheet(
            category = book?.format?.category ?: FormatCategory.REFLOWABLE,
            preferences = prefs,
            ttsState = ttsState,
            onDismiss = { showControlsSheet = false },
            onUpdatePrefs = { readerViewModel.updatePreferences(it) },
            onToggleTts = { readerViewModel.toggleTts() },
            onSetSleepTimer = { readerViewModel.ttsManager.setSleepTimer(it) }
        )
    }

    if (showIndexSheet) {
        val chapters = (contentState as? ReaderContentState.Reflowable)?.chapters.orEmpty()
        val totalPages = (contentState as? ReaderContentState.FixedPage)?.pageCount ?: chapters.size
        ReaderIndexAndNotesSheet(
            chapters = chapters,
            totalPages = totalPages,
            currentUnit = currentUnit,
            bookmarks = bookmarks,
            highlights = highlights,
            onDismiss = { showIndexSheet = false },
            onJumpToUnit = { readerViewModel.selectChapter(it) },
            onDeleteBookmark = { readerViewModel.deleteBookmark(it) },
            onDeleteHighlight = { readerViewModel.deleteHighlight(it) },
            onExportNotesMarkdown = {
                val safeName = (book?.title ?: "notes").replace(Regex("[^a-zA-Z0-9._-]"), "_")
                exportNotesLauncher.launch("${safeName}_notes.md")
            }
        )
    }

    pendingHighlightTarget?.let { (chIdx, pIdx, quote) ->
        HighlightNoteDialog(
            quotePreview = quote,
            onDismiss = { pendingHighlightTarget = null },
            onSave = { note, color ->
                readerViewModel.addHighlight(chIdx, pIdx, quote, note, color)
                pendingHighlightTarget = null
            }
        )
    }

    editingHighlight?.let { existing ->
        HighlightNoteDialog(
            quotePreview = existing.quoteText,
            initialNote = existing.noteText,
            initialColor = existing.color,
            onDismiss = { editingHighlight = null },
            onSave = { note, color ->
                readerViewModel.updateHighlight(existing.copy(noteText = note.trim(), color = color))
                editingHighlight = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReflowableBlockView(
    text: String,
    style: BlockStyle,
    runs: List<TextRun>,
    highlights: List<HighlightEntity>,
    isTtsHighlighted: Boolean,
    prefs: ReaderPreferences,
    fontFamily: FontFamily,
    inkColor: Color,
    onLongPress: () -> Unit
) {
    if (style == BlockStyle.DIVIDER) {
        HorizontalDivider(color = inkColor.copy(alpha = 0.22f), modifier = Modifier.padding(vertical = 8.dp))
        return
    }

    val annotated = remember(text, runs, highlights, isTtsHighlighted) {
        buildAnnotatedString {
            append(text)
            runs.forEach { run ->
                val s = run.start.coerceIn(0, text.length)
                val e = run.end.coerceIn(s, text.length)
                if (s < e) {
                    addStyle(
                        SpanStyle(
                            fontWeight = if (run.bold) FontWeight.Bold else null,
                            fontStyle = if (run.italic) FontStyle.Italic else null,
                            fontFamily = if (run.code) JetBrainsMonoFontFamily else null,
                            background = if (run.code) inkColor.copy(alpha = 0.08f) else Color.Unspecified
                        ),
                        s,
                        e
                    )
                }
            }
            highlights.forEach { hl ->
                addStyle(SpanStyle(background = Color(hl.color.hex)), 0, text.length)
            }
            if (isTtsHighlighted) {
                addStyle(SpanStyle(background = Color(0x33C86D3B)), 0, text.length)
            }
        }
    }

    val baseSize = prefs.fontSizeSp
    val textStyle = when (style) {
        BlockStyle.HEADING_1 -> TextStyle(
            fontFamily = fontFamily,
            fontSize = (baseSize * 1.45f).sp,
            lineHeight = (baseSize * 1.45f * 1.25f).sp,
            fontWeight = FontWeight.Bold,
            color = inkColor
        )
        BlockStyle.HEADING_2 -> TextStyle(
            fontFamily = fontFamily,
            fontSize = (baseSize * 1.25f).sp,
            lineHeight = (baseSize * 1.25f * 1.3f).sp,
            fontWeight = FontWeight.SemiBold,
            color = inkColor
        )
        BlockStyle.HEADING_3 -> TextStyle(
            fontFamily = fontFamily,
            fontSize = (baseSize * 1.1f).sp,
            lineHeight = (baseSize * 1.1f * 1.35f).sp,
            fontWeight = FontWeight.SemiBold,
            color = inkColor
        )
        BlockStyle.CODE_BLOCK -> TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = (baseSize * 0.85f).sp,
            lineHeight = (baseSize * 0.85f * 1.45f).sp,
            color = inkColor
        )
        BlockStyle.BLOCKQUOTE -> TextStyle(
            fontFamily = fontFamily,
            fontSize = baseSize.sp,
            lineHeight = (baseSize * prefs.lineHeightMultiplier).sp,
            fontStyle = FontStyle.Italic,
            color = inkColor.copy(alpha = 0.85f)
        )
        else -> TextStyle(
            fontFamily = fontFamily,
            fontSize = baseSize.sp,
            lineHeight = (baseSize * prefs.lineHeightMultiplier).sp,
            letterSpacing = prefs.letterSpacingEm.em,
            textIndent = if (prefs.firstLineIndentDp > 0.5f) TextIndent(firstLine = prefs.firstLineIndentDp.sp) else TextIndent.None,
            hyphens = if (prefs.autoHyphenation) Hyphens.Auto else Hyphens.None,
            color = inkColor,
            textAlign = if (prefs.textJustified) TextAlign.Justify else TextAlign.Start
        )
    }

    val containerModifier = when (style) {
        BlockStyle.BLOCKQUOTE -> Modifier
            .fillMaxWidth()
            .background(inkColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(start = 14.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
        BlockStyle.CODE_BLOCK -> Modifier
            .fillMaxWidth()
            .background(inkColor.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(12.dp)
        else -> Modifier.fillMaxWidth()
    }

    Box(
        modifier = containerModifier
            .clip(RoundedCornerShape(6.dp))
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
    ) {
        Text(text = annotated, style = textStyle)
    }
}

@Composable
private fun FixedPageCanvas(
    primaryBitmap: Bitmap?,
    secondaryBitmap: Bitmap?,
    isComic: Boolean,
    comicRtl: Boolean,
    onTapCenter: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember(primaryBitmap) { mutableFloatStateOf(1f) }
    var offsetX by remember(primaryBitmap) { mutableFloatStateOf(0f) }
    var offsetY by remember(primaryBitmap) { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(comicRtl, scale) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.1f) {
                            scale = 1f; offsetX = 0f; offsetY = 0f
                        } else {
                            scale = 2.0f
                        }
                    },
                    onTap = { pos ->
                        val w = size.width.toFloat()
                        when {
                            pos.x < w * 0.25f -> if (isComic && comicRtl) onNext() else onPrevious()
                            pos.x > w * 0.75f -> if (isComic && comicRtl) onPrevious() else onNext()
                            else -> onTapCenter()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextScale = (scale * zoom).coerceIn(1f, 4f)
                    scale = nextScale
                    if (nextScale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f; offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (primaryBitmap == null) {
            CircularProgressIndicator()
        } else {
            val contentModifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }

            if (secondaryBitmap != null) {
                val leftBmp = if (comicRtl) secondaryBitmap else primaryBitmap
                val rightBmp = if (comicRtl) primaryBitmap else secondaryBitmap
                Row(modifier = contentModifier, horizontalArrangement = Arrangement.Center) {
                    Image(
                        bitmap = leftBmp.asImageBitmap(),
                        contentDescription = "Left comic page",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    Image(
                        bitmap = rightBmp.asImageBitmap(),
                        contentDescription = "Right comic page",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            } else {
                Image(
                    bitmap = primaryBitmap.asImageBitmap(),
                    contentDescription = "Book page",
                    contentScale = ContentScale.Fit,
                    modifier = contentModifier
                )
            }
        }
    }
}

private fun resolveReaderTheme(prefs: ReaderPreferences, category: FormatCategory?): Pair<Color, Color> {
    if (category == FormatCategory.PDF && prefs.pdfDarkInvert) {
        return ReaderSlateBg to ReaderSlateInk
    }
    if (category == FormatCategory.COMIC) {
        return ReaderAmoledBg to ReaderAmoledInk
    }
    return when (prefs.themeMode) {
        ReaderThemeMode.PAPER -> ReaderPaperBg to ReaderPaperInk
        ReaderThemeMode.SEPIA -> ReaderSepiaBg to ReaderSepiaInk
        ReaderThemeMode.SLATE -> ReaderSlateBg to ReaderSlateInk
        ReaderThemeMode.AMOLED -> ReaderAmoledBg to ReaderAmoledInk
        ReaderThemeMode.CUSTOM -> Color(prefs.customBgHex) to Color(prefs.customInkHex)
    }
}

private fun resolveReaderFont(prefs: ReaderPreferences): FontFamily {
    return when (prefs.readerFont) {
        ReaderFont.LITERATA -> LiterataFontFamily
        ReaderFont.MERRIWEATHER -> MerriweatherFontFamily
        ReaderFont.PLUS_JAKARTA_SANS -> PlusJakartaFontFamily
        ReaderFont.JETBRAINS_MONO -> JetBrainsMonoFontFamily
        ReaderFont.SYSTEM_SERIF -> FontFamily.Serif
        ReaderFont.CUSTOM_FILE -> {
            val file = prefs.customFontPath?.let { File(it) }?.takeIf { it.exists() }
            if (file != null) {
                runCatching { FontFamily(Typeface.createFromFile(file)) }.getOrDefault(LiterataFontFamily)
            } else LiterataFontFamily
        }
    }
}
