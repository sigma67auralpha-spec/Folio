package com.example.ui.screens

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.model.DoubleTapAction
import com.example.data.model.FormatCategory
import com.example.data.model.HighlightColor
import com.example.data.model.HighlightEntity
import com.example.data.model.HorizontalGestureMode
import com.example.data.model.ImageColorEffect
import com.example.data.model.OrientationLock
import com.example.data.model.PageAnimation
import com.example.data.model.ProgressDisplayMode
import com.example.data.model.ReaderFont
import com.example.data.model.ReaderPreferences
import com.example.data.model.ReaderReadingMode
import com.example.data.model.ReaderTextAlignment
import com.example.data.model.ReaderThemeMode
import com.example.data.model.TapZonePreset
import com.example.data.model.defaultReaderColorPresets
import com.example.engine.BookChapter
import com.example.engine.BlockStyle
import com.example.engine.ParagraphBlock
import com.example.engine.TextRun
import com.example.ui.components.HighlightNoteDialog
import com.example.ui.components.ReaderControlsSheet
import com.example.ui.components.ReaderIndexAndNotesSheet
import com.example.ui.theme.EbGaramondFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LexendFontFamily
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.MerriweatherFontFamily
import com.example.ui.theme.NunitoFontFamily
import com.example.ui.theme.OpenSansFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.ReaderAmoledBg
import com.example.ui.theme.ReaderAmoledInk
import com.example.ui.theme.ReaderPaperBg
import com.example.ui.theme.ReaderPaperInk
import com.example.ui.theme.ReaderSepiaBg
import com.example.ui.theme.ReaderSepiaInk
import com.example.ui.theme.ReaderSlateBg
import com.example.ui.theme.ReaderSlateInk
import com.example.ui.theme.RobotoSerifFontFamily
import com.example.ui.theme.RobotoSlabFontFamily
import com.example.ui.viewmodel.ReaderContentState
import com.example.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, bookId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> readerViewModel.setReaderForeground(true)
                Lifecycle.Event.ON_PAUSE -> readerViewModel.setReaderForeground(false)
                else -> Unit
            }
        }
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            readerViewModel.setReaderForeground(true)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            readerViewModel.setReaderForeground(false)
        }
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
    var presetToast by remember { mutableStateOf<String?>(null) }
    var restoredInitialScroll by remember(bookId) { mutableStateOf(false) }
    var previousUnit by remember(bookId) { mutableIntStateOf(-1) }
    var pendingTargetParagraph by remember(bookId) { mutableStateOf<Int?>(null) }
    var pendingJumpToLastPage by remember(bookId) { mutableStateOf(false) }
    var pagedScrollFraction by remember(bookId) { mutableFloatStateOf(0f) }
    var currentPagedPageNumber by remember(bookId) { mutableIntStateOf(1) }
    var currentPagedTotalPages by remember(bookId) { mutableIntStateOf(1) }
    var lastActiveParagraph by remember(bookId) { mutableIntStateOf(0) }
    var onStepForward by remember(bookId) { mutableStateOf<() -> Unit>({ readerViewModel.nextUnit() }) }
    var onStepBackward by remember(bookId) { mutableStateOf<() -> Unit>({ readerViewModel.previousUnit() }) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
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

    LaunchedEffect(presetToast) {
        if (presetToast != null) {
            delay(1300L)
            presetToast = null
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

    LaunchedEffect(listState.isScrollInProgress, prefs.hideBarsOnFastScroll) {
        if (prefs.hideBarsOnFastScroll && listState.isScrollInProgress && showOverlay && !showSearchBar) {
            showOverlay = false
        }
    }

    DisposableEffect(prefs.keepScreenAwake) {
        view.keepScreenOn = prefs.keepScreenAwake
        onDispose { view.keepScreenOn = false }
    }

    DisposableEffect(prefs.fullscreenMode) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (prefs.fullscreenMode) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (window != null) {
                WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(prefs.orientationLock) {
        val activity = context as? Activity
        if (activity != null) {
            activity.requestedOrientation = when (prefs.orientationLock) {
                OrientationLock.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                OrientationLock.FREE -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                OrientationLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                OrientationLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                OrientationLock.LOCKED_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                OrientationLock.LOCKED_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(prefs.customBrightnessEnabled, prefs.screenBrightness) {
        val window = (context as? Activity)?.window
        val original = window?.attributes?.screenBrightness ?: -1f
        if (window != null) {
            val lp = window.attributes
            lp.screenBrightness = if (prefs.customBrightnessEnabled) {
                prefs.screenBrightness.coerceIn(0.05f, 1f)
            } else {
                original
            }
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
    val activeScrollFraction = if (
        contentState is ReaderContentState.Reflowable &&
        prefs.readingMode == ReaderReadingMode.PAGED
    ) {
        pagedScrollFraction
    } else {
        scrollFraction
    }

    LaunchedEffect(contentState is ReaderContentState.Reflowable, prefs.readingMode) {
        if (contentState !is ReaderContentState.Reflowable || prefs.readingMode == ReaderReadingMode.SCROLL) {
            onStepForward = { readerViewModel.nextUnit() }
            onStepBackward = { readerViewModel.previousUnit() }
        }
    }

    LaunchedEffect(currentUnit, listState.firstVisibleItemIndex, prefs.readingMode) {
        if (prefs.readingMode == ReaderReadingMode.SCROLL) {
            lastActiveParagraph = listState.firstVisibleItemIndex
            readerViewModel.recordUserReadingActivity()
        }
    }

    BackHandler {
        readerViewModel.setReaderForeground(false)
        readerViewModel.saveScrollProgress(activeScrollFraction)
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
                            onStepForward()
                            true
                        }
                        Key.VolumeUp -> {
                            onStepBackward()
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
                LaunchedEffect(currentUnit, state.chapters.size, prefs.readingMode, pendingTargetParagraph) {
                    if (prefs.readingMode == ReaderReadingMode.SCROLL) {
                        val activeChapter = state.chapters.getOrNull(currentUnit)
                        val jumpPara = pendingTargetParagraph
                        if (jumpPara != null && activeChapter != null) {
                            listState.scrollToItem(jumpPara.coerceIn(0, (activeChapter.blocks.size - 1).coerceAtLeast(0)))
                            pendingTargetParagraph = null
                            restoredInitialScroll = true
                            previousUnit = currentUnit
                        } else if (!restoredInitialScroll && activeChapter != null && book != null) {
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
                }

                LaunchedEffect(ttsState.currentParagraphIndex, prefs.readingMode) {
                    val idx = ttsState.currentParagraphIndex
                    if (prefs.readingMode == ReaderReadingMode.SCROLL && ttsState.isSpeaking && idx >= 0) {
                        listState.animateScrollToItem(idx)
                    }
                }

                val transitionForward = currentUnit >= previousUnit
                val readerInsetsModifier = when {
                    prefs.cutoutMargin -> Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                    prefs.fullscreenMode -> Modifier
                    else -> Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                }
                val swipeThresholdPx = with(density) { 72.dp.toPx() }
                val chapterStartOffsets = remember(state.chapters) {
                    var running = 0
                    state.chapters.map { ch ->
                        val start = running
                        running += ch.blocks.size.coerceAtLeast(1)
                        start
                    }
                }
                val totalGlobalBlocks = remember(state.chapters) {
                    state.chapters.sumOf { it.blocks.size.coerceAtLeast(1) }.coerceAtLeast(1)
                }

                var containerRootPos by remember { mutableStateOf(Offset.Zero) }
                var containerWidthPx by remember { mutableFloatStateOf(1f) }
                var containerHeightPx by remember { mutableFloatStateOf(1f) }
                val handleReflowableTap: (Offset) -> Unit = { localOffset ->
                    readerViewModel.recordUserReadingActivity()
                    val w = containerWidthPx.coerceAtLeast(1f)
                    val h = containerHeightPx.coerceAtLeast(1f)
                    when (prefs.tapZonePreset) {
                        TapZonePreset.CLASSIC_LR -> when {
                            localOffset.x < w * 0.28f -> onStepBackward()
                            localOffset.x > w * 0.72f -> onStepForward()
                            else -> showOverlay = !showOverlay
                        }
                        TapZonePreset.ONE_HAND_RIGHT -> when {
                            localOffset.y < h * 0.30f -> onStepBackward()
                            localOffset.x > w * 0.65f || localOffset.y > h * 0.72f -> onStepForward()
                            else -> showOverlay = !showOverlay
                        }
                        TapZonePreset.SCROLL_ONLY -> showOverlay = !showOverlay
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(readerInsetsModifier)
                        .onGloballyPositioned { coords ->
                            containerRootPos = coords.positionInRoot()
                            containerWidthPx = coords.size.width.toFloat()
                            containerHeightPx = coords.size.height.toFloat()
                        }
                        .pointerInput(prefs.tapZonePreset, prefs.readingMode) {
                            detectTapGestures { offset -> handleReflowableTap(offset) }
                        }
                        .pointerInput(prefs.readingMode, prefs.horizontalGestureMode, swipeThresholdPx) {
                            if (prefs.readingMode == ReaderReadingMode.PAGED ||
                                prefs.horizontalGestureMode == HorizontalGestureMode.OFF
                            ) {
                                return@pointerInput
                            }
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                                onDragEnd = {
                                    if (abs(totalDrag) >= swipeThresholdPx) {
                                        readerViewModel.recordUserReadingActivity()
                                        val goNext = when (prefs.horizontalGestureMode) {
                                            HorizontalGestureMode.NORMAL -> totalDrag < 0f
                                            HorizontalGestureMode.INVERTED -> totalDrag > 0f
                                            HorizontalGestureMode.OFF -> false
                                        }
                                        if (goNext) readerViewModel.nextUnit() else readerViewModel.previousUnit()
                                    }
                                }
                            )
                        }
                ) {
                    val boxMaxWidthPx = constraints.maxWidth
                    val boxMaxHeightPx = constraints.maxHeight
                    val textMeasurer = rememberTextMeasurer(cacheSize = 64)

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
                        val chapterHighlightsByPara = remember(highlights, chapter.index) {
                            highlights.filter { it.chapterIndex == chapter.index }
                                .groupBy { it.paragraphIndex }
                        }
                        val imgColorFilter = remember(prefs.imageColorEffect, bgColor, inkColor) {
                            when (prefs.imageColorEffect) {
                                ImageColorEffect.OFF -> null
                                ImageColorEffect.GRAYSCALE -> ColorFilter.colorMatrix(
                                    ColorMatrix().apply { setToSaturation(0f) }
                                )
                                ImageColorEffect.TEXT_COLOR -> ColorFilter.tint(inkColor, BlendMode.Color)
                                ImageColorEffect.BG_COLOR -> {
                                    if (bgColor.luminance() < 0.2f) {
                                        ColorFilter.colorMatrix(
                                            ColorMatrix().apply {
                                                setToSaturation(0.25f)
                                                timesAssign(ColorMatrix().apply { setToScale(0.75f, 0.75f, 0.75f, 1f) })
                                            }
                                        )
                                    } else {
                                        ColorFilter.tint(bgColor, BlendMode.Multiply)
                                    }
                                }
                            }
                        }

                        val doubleTapActionFor: (Int, ParagraphBlock) -> (() -> Unit)? = { pIdx, block ->
                            if (prefs.doubleTapAction == DoubleTapAction.OFF) {
                                null
                            } else {
                                {
                                    readerViewModel.recordUserReadingActivity()
                                    when (prefs.doubleTapAction) {
                                        DoubleTapAction.SPEAK_PARAGRAPH -> {
                                            if (ttsState.isSpeaking && ttsState.currentParagraphIndex == pIdx) {
                                                readerViewModel.ttsManager.stop()
                                            } else {
                                                val blocks = chapter.blocks.map { it.text }
                                                readerViewModel.ttsManager.updateVoiceParams(prefs.ttsRate, prefs.ttsPitch)
                                                readerViewModel.ttsManager.startReading(blocks, pIdx)
                                            }
                                        }
                                        DoubleTapAction.QUICK_HIGHLIGHT -> {
                                            if (prefs.enableHighlights) {
                                                readerViewModel.addHighlight(
                                                    chapter.index,
                                                    pIdx,
                                                    block.text,
                                                    "",
                                                    HighlightColor.AMBER
                                                )
                                            }
                                        }
                                        DoubleTapAction.EXTERNAL_TRANSLATE -> {
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, block.text)
                                            }
                                            val chooser = Intent.createChooser(sendIntent, "Translate paragraph with")
                                            runCatching { context.startActivity(chooser) }.onFailure {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("No translator or text app available.")
                                                }
                                            }
                                        }
                                        DoubleTapAction.OFF -> Unit
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            if (prefs.readingMode == ReaderReadingMode.PAGED) {
                                val maxColPx = with(density) { prefs.maxColumnWidthDp.dp.roundToPx() }
                                val horizMarginPx = with(density) { prefs.horizontalMarginDp.dp.roundToPx() }
                                val topPadPx = with(density) { (prefs.verticalPaddingDp + 12f).dp.roundToPx() }
                                val footerReserveDp = (if (prefs.showReaderFooter || prefs.showProgressBars) 46f else 14f) +
                                    prefs.bottomBarMarginDp + prefs.verticalPaddingDp
                                val footerReservePx = with(density) { footerReserveDp.dp.roundToPx() }
                                val colWidthPx = minOf(boxMaxWidthPx, maxColPx)
                                val pageContentWidthPx = (colWidthPx - horizMarginPx * 2).coerceAtLeast(120)
                                val pageContentHeightPx = (boxMaxHeightPx - topPadPx - footerReservePx).coerceAtLeast(160)

                                val pages = remember(
                                    chapter.index,
                                    chapter.blocks,
                                    pageContentWidthPx,
                                    pageContentHeightPx,
                                    prefs.fontSizeSp,
                                    prefs.lineHeightMultiplier,
                                    prefs.letterSpacingEm,
                                    prefs.paragraphSpacingDp,
                                    prefs.firstLineIndentDp,
                                    prefs.readerFont,
                                    prefs.readerFontWeight,
                                    prefs.italicText,
                                    prefs.textAlignment,
                                    prefs.chapterTitleAlignment,
                                    prefs.autoHyphenation,
                                    prefs.bionicReading,
                                    prefs.showImages,
                                    prefs.showImageCaptions,
                                    prefs.imageWidthPercent,
                                    prefs.customFontPath
                                ) {
                                    paginateChapterBlocks(
                                        blocks = chapter.blocks,
                                        prefs = prefs,
                                        fontFamily = readerFontFamily,
                                        inkColor = inkColor,
                                        textMeasurer = textMeasurer,
                                        contentWidthPx = pageContentWidthPx,
                                        contentHeightPx = pageContentHeightPx,
                                        density = density
                                    )
                                }

                                val initialPage = remember(targetUnit, pages.size) {
                                    when {
                                        pendingTargetParagraph != null -> findPageForParagraph(pages, pendingTargetParagraph!!)
                                        pendingJumpToLastPage -> pages.lastIndex.coerceAtLeast(0)
                                        !restoredInitialScroll && book != null -> {
                                            val frac = book?.scrollFraction ?: 0f
                                            (frac * (pages.size - 1).coerceAtLeast(0)).roundToInt()
                                        }
                                        restoredInitialScroll && targetUnit == currentUnit && lastActiveParagraph > 0 ->
                                            findPageForParagraph(pages, lastActiveParagraph)
                                        else -> 0
                                    }.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                                }

                                val pagerState = rememberPagerState(initialPage = initialPage) {
                                    pages.size.coerceAtLeast(1)
                                }

                                if (targetUnit == currentUnit) {
                                    LaunchedEffect(pages, pendingTargetParagraph, pendingJumpToLastPage, restoredInitialScroll) {
                                        val targetPara = pendingTargetParagraph
                                        when {
                                            targetPara != null -> {
                                                pendingTargetParagraph = null
                                                restoredInitialScroll = true
                                                previousUnit = currentUnit
                                                pagerState.scrollToPage(findPageForParagraph(pages, targetPara))
                                            }
                                            pendingJumpToLastPage -> {
                                                pendingJumpToLastPage = false
                                                restoredInitialScroll = true
                                                previousUnit = currentUnit
                                                pagerState.scrollToPage(pages.lastIndex.coerceAtLeast(0))
                                            }
                                            !restoredInitialScroll && book != null -> {
                                                restoredInitialScroll = true
                                                previousUnit = currentUnit
                                                val frac = book?.scrollFraction ?: 0f
                                                val p = (frac * (pages.size - 1).coerceAtLeast(0)).roundToInt()
                                                    .coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                                                pagerState.scrollToPage(p)
                                            }
                                            previousUnit != -1 && previousUnit != currentUnit -> {
                                                previousUnit = currentUnit
                                                pagerState.scrollToPage(0)
                                            }
                                        }
                                    }

                                    LaunchedEffect(pagerState.currentPage, pages) {
                                        val maxIdx = (pages.size - 1).coerceAtLeast(1)
                                        pagedScrollFraction = if (pages.size <= 1) 0f else {
                                            (pagerState.currentPage.toFloat() / maxIdx).coerceIn(0f, 1f)
                                        }
                                        currentPagedPageNumber = pagerState.currentPage + 1
                                        currentPagedTotalPages = pages.size.coerceAtLeast(1)
                                        lastActiveParagraph = pages.getOrNull(pagerState.currentPage)?.firstParagraphIndex ?: 0
                                        readerViewModel.recordUserReadingActivity()
                                    }

                                    LaunchedEffect(pagerState.isScrollInProgress, prefs.hideBarsOnFastScroll) {
                                        if (prefs.hideBarsOnFastScroll && pagerState.isScrollInProgress && showOverlay && !showSearchBar) {
                                            showOverlay = false
                                        }
                                    }

                                    LaunchedEffect(ttsState.currentParagraphIndex, ttsState.isSpeaking, pages) {
                                        val idx = ttsState.currentParagraphIndex
                                        if (ttsState.isSpeaking && idx >= 0) {
                                            val targetPg = findPageForParagraph(pages, idx)
                                            if (targetPg != pagerState.currentPage) {
                                                pagerState.animateScrollToPage(targetPg)
                                            }
                                        }
                                    }

                                    LaunchedEffect(pagerState.currentPage, pages.size, currentUnit, state.chapters.size) {
                                        onStepForward = {
                                            readerViewModel.recordUserReadingActivity()
                                            if (pagerState.currentPage < pages.lastIndex) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                }
                                            } else {
                                                readerViewModel.nextUnit()
                                            }
                                        }
                                        onStepBackward = {
                                            readerViewModel.recordUserReadingActivity()
                                            if (pagerState.currentPage > 0) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                }
                                            } else if (currentUnit > 0) {
                                                pendingJumpToLastPage = true
                                                readerViewModel.previousUnit()
                                            }
                                        }
                                    }
                                }

                                val reverseLayout = prefs.horizontalGestureMode == HorizontalGestureMode.INVERTED
                                val edgeSwipeThresholdPx = with(density) { 48.dp.toPx() }
                                var edgeOverswipeAccum by remember(targetUnit) { mutableFloatStateOf(0f) }
                                val boundaryNestedScroll = remember(
                                    targetUnit,
                                    pages.size,
                                    reverseLayout,
                                    state.chapters.size
                                ) {
                                    object : NestedScrollConnection {
                                        override fun onPostScroll(
                                            consumed: Offset,
                                            available: Offset,
                                            source: NestedScrollSource
                                        ): Offset {
                                            if (source == NestedScrollSource.UserInput && available.x != 0f) {
                                                val forwardDelta = if (reverseLayout) available.x else -available.x
                                                val atLastPage = pagerState.currentPage >= pages.lastIndex
                                                val atFirstPage = pagerState.currentPage <= 0
                                                if ((atLastPage && forwardDelta > 0f && targetUnit < state.chapters.lastIndex) ||
                                                    (atFirstPage && forwardDelta < 0f && targetUnit > 0)
                                                ) {
                                                    edgeOverswipeAccum += forwardDelta
                                                }
                                            }
                                            return Offset.Zero
                                        }

                                        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                                            val accum = edgeOverswipeAccum
                                            edgeOverswipeAccum = 0f
                                            if (accum >= edgeSwipeThresholdPx &&
                                                pagerState.currentPage >= pages.lastIndex &&
                                                targetUnit < state.chapters.lastIndex
                                            ) {
                                                readerViewModel.recordUserReadingActivity()
                                                readerViewModel.nextUnit()
                                            } else if (accum <= -edgeSwipeThresholdPx &&
                                                pagerState.currentPage <= 0 &&
                                                targetUnit > 0
                                            ) {
                                                readerViewModel.recordUserReadingActivity()
                                                pendingJumpToLastPage = true
                                                readerViewModel.previousUnit()
                                            }
                                            return Velocity.Zero
                                        }
                                    }
                                }

                                HorizontalPager(
                                    state = pagerState,
                                    reverseLayout = reverseLayout,
                                    userScrollEnabled = prefs.horizontalGestureMode != HorizontalGestureMode.OFF,
                                    beyondViewportPageCount = 1,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nestedScroll(boundaryNestedScroll)
                                ) { pageIdx ->
                                    val chapterPage = pages.getOrNull(pageIdx)
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(prefs.paragraphSpacingDp.dp),
                                            modifier = Modifier
                                                .widthIn(max = prefs.maxColumnWidthDp.dp)
                                                .align(Alignment.TopCenter)
                                                .fillMaxHeight()
                                                .padding(
                                                    start = prefs.horizontalMarginDp.dp,
                                                    end = prefs.horizontalMarginDp.dp,
                                                    top = prefs.verticalPaddingDp.dp + 12.dp,
                                                    bottom = footerReserveDp.dp
                                                )
                                                .drawWithContent {
                                                    drawContent()
                                                    if (prefs.perceptionExpander) {
                                                        val xLeft = size.width * 0.14f
                                                        val xRight = size.width * 0.86f
                                                        val guideColor = inkColor.copy(alpha = 0.16f)
                                                        val strokePx = 1.2.dp.toPx()
                                                        drawLine(guideColor, Offset(xLeft, 0f), Offset(xLeft, size.height), strokeWidth = strokePx)
                                                        drawLine(guideColor, Offset(xRight, 0f), Offset(xRight, size.height), strokeWidth = strokePx)
                                                    }
                                                }
                                        ) {
                                            chapterPage?.slices?.forEach { slice ->
                                                when (slice) {
                                                    is PageSlice.DividerSlice -> {
                                                        HorizontalDivider(
                                                            color = inkColor.copy(alpha = 0.22f),
                                                            modifier = Modifier.padding(vertical = 8.dp)
                                                        )
                                                    }
                                                    is PageSlice.ImageSlice -> {
                                                        ReflowableImageBlock(
                                                            block = slice.block,
                                                            prefs = prefs,
                                                            inkColor = inkColor,
                                                            imgColorFilter = imgColorFilter,
                                                            maxHeightDp = 280
                                                        )
                                                    }
                                                    is PageSlice.TextSlice -> {
                                                        val paraHighlights = chapterHighlightsByPara[slice.paragraphIndex].orEmpty()
                                                        val isTtsActive = ttsState.isSpeaking &&
                                                            ttsState.currentParagraphIndex == slice.paragraphIndex
                                                        Column {
                                                            ReflowableBlockView(
                                                                text = slice.block.text,
                                                                style = slice.block.style,
                                                                runs = slice.block.runs,
                                                                highlights = if (prefs.enableHighlights) paraHighlights else emptyList(),
                                                                isTtsHighlighted = isTtsActive,
                                                                prefs = prefs,
                                                                fontFamily = readerFontFamily,
                                                                inkColor = inkColor,
                                                                startChar = slice.startChar,
                                                                endChar = slice.endChar,
                                                                omitFirstLineIndent = slice.isContinuation,
                                                                onTapRoot = { rootOffset ->
                                                                    handleReflowableTap(rootOffset - containerRootPos)
                                                                },
                                                                onLongPress = {
                                                                    if (prefs.enableHighlights) {
                                                                        pendingHighlightTarget = Triple(
                                                                            chapter.index,
                                                                            slice.paragraphIndex,
                                                                            slice.block.text
                                                                        )
                                                                    }
                                                                },
                                                                onDoubleTap = doubleTapActionFor(slice.paragraphIndex, slice.block)
                                                            )
                                                            if (prefs.enableHighlights && !slice.isContinuation) {
                                                                HighlightMarginNotes(
                                                                    paraHighlights = paraHighlights,
                                                                    inkColor = inkColor,
                                                                    onEdit = { editingHighlight = it }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(
                                        start = prefs.horizontalMarginDp.dp,
                                        end = prefs.horizontalMarginDp.dp,
                                        top = prefs.verticalPaddingDp.dp + 12.dp,
                                        bottom = (78f + prefs.bottomBarMarginDp).dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(prefs.paragraphSpacingDp.dp),
                                    modifier = Modifier
                                        .widthIn(max = prefs.maxColumnWidthDp.dp)
                                        .align(Alignment.TopCenter)
                                        .fillMaxHeight()
                                        .drawWithContent {
                                            drawContent()
                                            if (prefs.perceptionExpander) {
                                                val xLeft = size.width * 0.14f
                                                val xRight = size.width * 0.86f
                                                val guideColor = inkColor.copy(alpha = 0.16f)
                                                val strokePx = 1.2.dp.toPx()
                                                drawLine(guideColor, Offset(xLeft, 0f), Offset(xLeft, size.height), strokeWidth = strokePx)
                                                drawLine(guideColor, Offset(xRight, 0f), Offset(xRight, size.height), strokeWidth = strokePx)
                                            }
                                        }
                                ) {
                                    itemsIndexed(
                                        items = chapter.blocks,
                                        key = { pIdx, _ -> "${chapter.index}_$pIdx" }
                                    ) { pIdx, block ->
                                        val paraHighlights = chapterHighlightsByPara[pIdx].orEmpty()
                                        val isTtsActive = ttsState.isSpeaking && ttsState.currentParagraphIndex == pIdx
                                        if (block.style == BlockStyle.IMAGE) {
                                            ReflowableImageBlock(
                                                block = block,
                                                prefs = prefs,
                                                inkColor = inkColor,
                                                imgColorFilter = imgColorFilter,
                                                maxHeightDp = 360
                                            )
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
                                                    onTapRoot = { rootOffset ->
                                                        handleReflowableTap(rootOffset - containerRootPos)
                                                    },
                                                    onLongPress = {
                                                        if (prefs.enableHighlights) {
                                                            pendingHighlightTarget = Triple(chapter.index, pIdx, block.text)
                                                        }
                                                    },
                                                    onDoubleTap = doubleTapActionFor(pIdx, block)
                                                )
                                                if (prefs.enableHighlights) {
                                                    HighlightMarginNotes(
                                                        paraHighlights = paraHighlights,
                                                        inkColor = inkColor,
                                                        onEdit = { editingHighlight = it }
                                                    )
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
                            }

                            // Subtle modular footer + progress bar
                            val totalChapters = state.chapters.size.coerceAtLeast(1)
                            val unitProgress = if (prefs.readingMode == ReaderReadingMode.PAGED) pagedScrollFraction else scrollFraction
                            val overallPct = (((targetUnit + unitProgress) / totalChapters.toFloat()) * 100f).coerceIn(0f, 100f)
                            val activeBlockIndex = if (prefs.readingMode == ReaderReadingMode.PAGED) {
                                lastActiveParagraph
                            } else {
                                listState.firstVisibleItemIndex
                            }
                            val currentBlockPos = (chapterStartOffsets.getOrElse(targetUnit) { 0 } +
                                activeBlockIndex + 1).coerceIn(1, totalGlobalBlocks)
                            val chapterWords = remember(chapter.index, chapter.blocks.size) {
                                chapter.blocks.sumOf { b ->
                                    if (b.text.isBlank()) 0 else b.text.trim().split(Regex("\\s+")).size
                                }
                            }
                            val minsRemaining = ((chapterWords * (1f - unitProgress)) / 220f).roundToInt().coerceAtLeast(1)
                            if (prefs.showReaderFooter || prefs.showProgressBars) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = prefs.bottomBarMarginDp.dp)
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
                                                if (prefs.readingMode == ReaderReadingMode.PAGED) {
                                                    add("Pg $currentPagedPageNumber/$currentPagedTotalPages")
                                                }
                                                if (prefs.showEtaInReader && chapterWords > 40) add("~${minsRemaining}m")
                                                when (prefs.progressDisplayMode) {
                                                    ProgressDisplayMode.PERCENT -> {
                                                        add("${overallPct.roundToInt()}%")
                                                        add("${targetUnit + 1}/${state.chapters.size}")
                                                    }
                                                    ProgressDisplayMode.POSITION_COUNT -> {
                                                        add("Pos $currentBlockPos/$totalGlobalBlocks")
                                                        add("Ch ${targetUnit + 1}/${state.chapters.size}")
                                                    }
                                                }
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
                val fixedInsetsModifier = when {
                    prefs.cutoutMargin -> Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                    prefs.fullscreenMode -> Modifier
                    else -> Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                }
                FixedPageCanvas(
                    primaryBitmap = pageBitmap,
                    secondaryBitmap = spreadBitmap,
                    isComic = state.isComic,
                    comicRtl = prefs.comicRtl,
                    horizontalGestureMode = prefs.horizontalGestureMode,
                    onTapCenter = {
                        readerViewModel.recordUserReadingActivity()
                        showOverlay = !showOverlay
                    },
                    onNext = {
                        readerViewModel.recordUserReadingActivity()
                        readerViewModel.nextUnit()
                    },
                    onPrevious = {
                        readerViewModel.recordUserReadingActivity()
                        readerViewModel.previousUnit()
                    },
                    modifier = Modifier.fillMaxSize().then(fixedInsetsModifier)
                )

                val pct = if (state.pageCount <= 1) 100f else ((currentUnit + 1) * 100f / state.pageCount)
                if (prefs.showReaderFooter || prefs.showProgressBars) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = prefs.bottomBarMarginDp.dp)
                            .fillMaxWidth()
                    ) {
                        if (prefs.showReaderFooter) {
                            val pageMeta = buildList {
                                when (prefs.progressDisplayMode) {
                                    ProgressDisplayMode.PERCENT -> {
                                        add("Page ${currentUnit + 1} of ${state.pageCount}")
                                        add("${pct.roundToInt()}%")
                                    }
                                    ProgressDisplayMode.POSITION_COUNT -> {
                                        add("Pos ${currentUnit + 1}/${state.pageCount}")
                                    }
                                }
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
                        var currentB = prefs.screenBrightness.coerceIn(0.05f, 1f)
                        detectVerticalDragGestures(
                            onDragEnd = { brightnessToast = null },
                            onDragCancel = { brightnessToast = null }
                        ) { _, dragAmount ->
                            currentB = (currentB - dragAmount / 600f).coerceIn(0.05f, 1f)
                            brightnessToast = currentB
                            readerViewModel.updatePreferences {
                                it.copy(customBrightnessEnabled = true, screenBrightness = currentB)
                            }
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

        presetToast?.let { name ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 4.dp,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
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
            val topSwipeThresholdPx = with(density) { 48.dp.toPx() }
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(prefs.quickColorPresetSwipe, prefs.colorPresets, prefs.activeColorPresetId) {
                        if (!prefs.quickColorPresetSwipe) return@pointerInput
                        var dragAccum = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccum = 0f },
                            onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                            onDragEnd = {
                                if (abs(dragAccum) >= topSwipeThresholdPx) {
                                    val list = prefs.colorPresets.ifEmpty { defaultReaderColorPresets() }
                                    val curIdx = list.indexOfFirst { it.id == prefs.activeColorPresetId }.coerceAtLeast(0)
                                    val nextIdx = if (dragAccum < 0f) {
                                        (curIdx + 1) % list.size
                                    } else {
                                        (curIdx - 1 + list.size) % list.size
                                    }
                                    val nextPreset = list[nextIdx]
                                    readerViewModel.updatePreferences {
                                        it.copy(
                                            themeMode = ReaderThemeMode.CUSTOM,
                                            activeColorPresetId = nextPreset.id,
                                            customBgHex = nextPreset.bgHex,
                                            customInkHex = nextPreset.inkHex
                                        )
                                    }
                                    presetToast = nextPreset.name
                                }
                            }
                        )
                    }
            ) {
                Column(modifier = Modifier.statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                readerViewModel.setReaderForeground(false)
                                readerViewModel.saveScrollProgress(activeScrollFraction)
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
                                                pendingTargetParagraph = match.paragraphIndex
                                                readerViewModel.selectChapter(match.chapterIndex)
                                                if (prefs.readingMode == ReaderReadingMode.SCROLL) {
                                                    coroutineScope.launch {
                                                        listState.scrollToItem(match.paragraphIndex)
                                                    }
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

@Composable
private fun ReflowableImageBlock(
    block: ParagraphBlock,
    prefs: ReaderPreferences,
    inkColor: Color,
    imgColorFilter: ColorFilter?,
    maxHeightDp: Int
) {
    val imgFile = block.imagePath?.let { File(it) }
    if (!prefs.showImages || imgFile == null || !imgFile.exists()) return
    val imgAlign = when (prefs.imageAlignment) {
        ReaderTextAlignment.START -> Alignment.Start
        ReaderTextAlignment.END -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    val captionTextAlign = when (prefs.imageAlignment) {
        ReaderTextAlignment.START -> TextAlign.Start
        ReaderTextAlignment.END -> TextAlign.End
        else -> TextAlign.Center
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = imgAlign
    ) {
        AsyncImage(
            model = imgFile,
            contentDescription = block.text,
            contentScale = ContentScale.Fit,
            colorFilter = imgColorFilter,
            modifier = Modifier
                .fillMaxWidth(prefs.imageWidthPercent.coerceIn(0.35f, 1.0f))
                .heightIn(max = maxHeightDp.dp)
                .clip(RoundedCornerShape(prefs.imageCornerRadiusDp.dp))
        )
        if (prefs.showImageCaptions &&
            block.text.isNotBlank() &&
            !block.text.equals("image", ignoreCase = true) &&
            !block.text.equals("cover", ignoreCase = true)
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = block.text,
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
                color = inkColor.copy(alpha = 0.65f),
                textAlign = captionTextAlign,
                modifier = Modifier.fillMaxWidth(prefs.imageWidthPercent.coerceIn(0.35f, 1.0f))
            )
        }
    }
}

@Composable
private fun HighlightMarginNotes(
    paraHighlights: List<HighlightEntity>,
    inkColor: Color,
    onEdit: (HighlightEntity) -> Unit
) {
    paraHighlights.filter { it.noteText.isNotBlank() }.forEach { hl ->
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(hl.color.hex).copy(alpha = 0.28f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit(hl) }
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
    startChar: Int = 0,
    endChar: Int = text.length,
    omitFirstLineIndent: Boolean = false,
    onTapRoot: (Offset) -> Unit,
    onLongPress: () -> Unit,
    onDoubleTap: (() -> Unit)? = null
) {
    if (style == BlockStyle.DIVIDER) {
        HorizontalDivider(color = inkColor.copy(alpha = 0.22f), modifier = Modifier.padding(vertical = 8.dp))
        return
    }

    val annotated = remember(
        text,
        style,
        runs,
        highlights,
        isTtsHighlighted,
        prefs.bionicReading,
        prefs.readerFontWeight,
        inkColor,
        startChar,
        endChar
    ) {
        buildSliceAnnotatedString(
            fullText = text,
            style = style,
            runs = runs,
            highlights = highlights,
            isTtsHighlighted = isTtsHighlighted,
            prefs = prefs,
            inkColor = inkColor,
            startChar = startChar,
            endChar = endChar
        )
    }

    val textStyle = resolveBlockTextStyle(
        style = style,
        prefs = prefs,
        fontFamily = fontFamily,
        inkColor = inkColor,
        omitFirstLineIndent = omitFirstLineIndent
    )

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

    var blockRootPos by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = containerModifier
            .clip(RoundedCornerShape(6.dp))
            .onGloballyPositioned { blockRootPos = it.positionInRoot() }
            .pointerInput(onLongPress, onDoubleTap, onTapRoot) {
                detectTapGestures(
                    onTap = { local -> onTapRoot(blockRootPos + local) },
                    onLongPress = { onLongPress() },
                    onDoubleTap = onDoubleTap?.let { fn -> { fn() } }
                )
            }
    ) {
        Text(
            text = annotated,
            style = textStyle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FixedPageCanvas(
    primaryBitmap: Bitmap?,
    secondaryBitmap: Bitmap?,
    isComic: Boolean,
    comicRtl: Boolean,
    horizontalGestureMode: HorizontalGestureMode,
    onTapCenter: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember(primaryBitmap) { mutableFloatStateOf(1f) }
    var offsetX by remember(primaryBitmap) { mutableFloatStateOf(0f) }
    var offsetY by remember(primaryBitmap) { mutableFloatStateOf(0f) }
    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

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
            .pointerInput(horizontalGestureMode, isComic, comicRtl, scale) {
                if (horizontalGestureMode == HorizontalGestureMode.OFF || scale > 1.08f) return@pointerInput
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                    onDragEnd = {
                        if (abs(totalDrag) >= swipeThresholdPx) {
                            var goNext = when (horizontalGestureMode) {
                                HorizontalGestureMode.NORMAL -> totalDrag < 0f
                                HorizontalGestureMode.INVERTED -> totalDrag > 0f
                                HorizontalGestureMode.OFF -> false
                            }
                            if (isComic && comicRtl) goNext = !goNext
                            if (goNext) onNext() else onPrevious()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val multiTouch = event.changes.size >= 2
                        if (multiTouch || scale > 1.05f) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
                            scale = nextScale
                            if (nextScale > 1f) {
                                offsetX += panChange.x
                                offsetY += panChange.y
                            } else {
                                offsetX = 0f; offsetY = 0f
                            }
                            if (multiTouch || zoomChange != 1f || scale > 1.05f) {
                                event.changes.forEach { if (it.pressed) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
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
        ReaderFont.LORA -> LoraFontFamily
        ReaderFont.EB_GARAMOND -> EbGaramondFontFamily
        ReaderFont.ROBOTO_SERIF -> RobotoSerifFontFamily
        ReaderFont.ROBOTO_SLAB -> RobotoSlabFontFamily
        ReaderFont.OPEN_SANS -> OpenSansFontFamily
        ReaderFont.NUNITO -> NunitoFontFamily
        ReaderFont.LEXEND -> LexendFontFamily
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
