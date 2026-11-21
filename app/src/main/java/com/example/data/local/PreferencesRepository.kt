package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppAccent
import com.example.data.model.LibraryDensity
import com.example.data.model.LibrarySortOrder
import com.example.data.model.LibraryViewMode
import com.example.data.model.OrientationLock
import com.example.data.model.PageAnimation
import com.example.data.model.ReaderFont
import com.example.data.model.ReaderPreferences
import com.example.data.model.ReaderThemeMode
import com.example.data.model.TapZonePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "folio_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CUSTOM_BG = longPreferencesKey("custom_bg")
        val CUSTOM_INK = longPreferencesKey("custom_ink")
        val READER_FONT = stringPreferencesKey("reader_font")
        val CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
        val CUSTOM_FONT_NAME = stringPreferencesKey("custom_font_name")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val LINE_HEIGHT = floatPreferencesKey("line_height")
        val LETTER_SPACING = floatPreferencesKey("letter_spacing")
        val PARAGRAPH_SPACING = floatPreferencesKey("paragraph_spacing")
        val FIRST_LINE_INDENT = floatPreferencesKey("first_line_indent")
        val HORIZONTAL_MARGIN = floatPreferencesKey("horizontal_margin")
        val VERTICAL_PADDING = floatPreferencesKey("vertical_padding")
        val MAX_COLUMN_WIDTH = floatPreferencesKey("max_column_width")
        val TEXT_JUSTIFIED = booleanPreferencesKey("text_justified")
        val AUTO_HYPHENATION = booleanPreferencesKey("auto_hyphenation")
        val WARMTH_TINT = floatPreferencesKey("warmth_tint")
        val TAP_ZONE = stringPreferencesKey("tap_zone")
        val VOLUME_PAGING = booleanPreferencesKey("volume_paging")
        val SWIPE_BRIGHTNESS = booleanPreferencesKey("swipe_brightness")
        val SCREEN_BRIGHTNESS = floatPreferencesKey("screen_brightness")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val PAGE_ANIMATION = stringPreferencesKey("page_animation")
        val ORIENTATION_LOCK = stringPreferencesKey("orientation_lock")
        val SHOW_STATS = booleanPreferencesKey("show_stats")
        val SHOW_CONTINUE_READING = booleanPreferencesKey("show_continue_reading")
        val SHOW_PROGRESS_BARS = booleanPreferencesKey("show_progress_bars")
        val SHOW_FORMAT_BADGES = booleanPreferencesKey("show_format_badges")
        val SHOW_READER_FOOTER = booleanPreferencesKey("show_reader_footer")
        val SHOW_CLOCK_IN_READER = booleanPreferencesKey("show_clock_in_reader")
        val SHOW_ETA_IN_READER = booleanPreferencesKey("show_eta_in_reader")
        val ENABLE_HIGHLIGHTS = booleanPreferencesKey("enable_highlights")
        val ENABLE_TTS = booleanPreferencesKey("enable_tts")
        val APP_DARK_THEME = stringPreferencesKey("app_dark_theme")
        val APP_ACCENT = stringPreferencesKey("app_accent")
        val LIBRARY_DENSITY = stringPreferencesKey("library_density")
        val COMIC_RTL = booleanPreferencesKey("comic_rtl")
        val COMIC_DOUBLE_SPREAD = booleanPreferencesKey("comic_double_spread")
        val PDF_MARGIN_CROP = booleanPreferencesKey("pdf_margin_crop")
        val PDF_DARK_INVERT = booleanPreferencesKey("pdf_dark_invert")
        val TTS_RATE = floatPreferencesKey("tts_rate")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val LIBRARY_VIEW_MODE = stringPreferencesKey("library_view_mode")
        val LIBRARY_SORT_ORDER = stringPreferencesKey("library_sort_order")
    }

    private fun readPrefs(prefs: Preferences): ReaderPreferences {
        return ReaderPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ReaderThemeMode.valueOf(it) }.getOrNull() }
                ?: ReaderThemeMode.PAPER,
            customBgHex = prefs[Keys.CUSTOM_BG] ?: 0xFFF9F4E8,
            customInkHex = prefs[Keys.CUSTOM_INK] ?: 0xFF231F1B,
            readerFont = prefs[Keys.READER_FONT]?.let { runCatching { ReaderFont.valueOf(it) }.getOrNull() }
                ?: ReaderFont.LITERATA,
            customFontPath = prefs[Keys.CUSTOM_FONT_PATH],
            customFontName = prefs[Keys.CUSTOM_FONT_NAME],
            fontSizeSp = prefs[Keys.FONT_SIZE] ?: 18f,
            lineHeightMultiplier = prefs[Keys.LINE_HEIGHT] ?: 1.55f,
            letterSpacingEm = prefs[Keys.LETTER_SPACING] ?: 0f,
            paragraphSpacingDp = prefs[Keys.PARAGRAPH_SPACING] ?: 12f,
            firstLineIndentDp = prefs[Keys.FIRST_LINE_INDENT] ?: 0f,
            horizontalMarginDp = prefs[Keys.HORIZONTAL_MARGIN] ?: 22f,
            verticalPaddingDp = prefs[Keys.VERTICAL_PADDING] ?: 16f,
            maxColumnWidthDp = prefs[Keys.MAX_COLUMN_WIDTH] ?: 720f,
            textJustified = prefs[Keys.TEXT_JUSTIFIED] ?: false,
            autoHyphenation = prefs[Keys.AUTO_HYPHENATION] ?: true,
            warmthTint = prefs[Keys.WARMTH_TINT] ?: 0f,
            tapZonePreset = prefs[Keys.TAP_ZONE]?.let { runCatching { TapZonePreset.valueOf(it) }.getOrNull() }
                ?: TapZonePreset.CLASSIC_LR,
            volumeKeyPaging = prefs[Keys.VOLUME_PAGING] ?: true,
            swipeBrightness = prefs[Keys.SWIPE_BRIGHTNESS] ?: true,
            screenBrightness = prefs[Keys.SCREEN_BRIGHTNESS] ?: -1f,
            keepScreenAwake = prefs[Keys.KEEP_SCREEN_AWAKE] ?: true,
            pageAnimation = prefs[Keys.PAGE_ANIMATION]?.let { runCatching { PageAnimation.valueOf(it) }.getOrNull() }
                ?: PageAnimation.SLIDE,
            orientationLock = prefs[Keys.ORIENTATION_LOCK]?.let { runCatching { OrientationLock.valueOf(it) }.getOrNull() }
                ?: OrientationLock.SYSTEM,
            showStats = prefs[Keys.SHOW_STATS] ?: true,
            showContinueReading = prefs[Keys.SHOW_CONTINUE_READING] ?: true,
            showProgressBars = prefs[Keys.SHOW_PROGRESS_BARS] ?: true,
            showFormatBadges = prefs[Keys.SHOW_FORMAT_BADGES] ?: true,
            showReaderFooter = prefs[Keys.SHOW_READER_FOOTER] ?: true,
            showClockInReader = prefs[Keys.SHOW_CLOCK_IN_READER] ?: false,
            showEtaInReader = prefs[Keys.SHOW_ETA_IN_READER] ?: true,
            enableHighlights = prefs[Keys.ENABLE_HIGHLIGHTS] ?: true,
            enableTts = prefs[Keys.ENABLE_TTS] ?: true,
            appDarkTheme = when (prefs[Keys.APP_DARK_THEME]) {
                "DARK" -> true
                "LIGHT" -> false
                else -> null
            },
            appAccent = prefs[Keys.APP_ACCENT]?.let { runCatching { AppAccent.valueOf(it) }.getOrNull() }
                ?: AppAccent.TERRACOTTA,
            libraryDensity = prefs[Keys.LIBRARY_DENSITY]?.let { runCatching { LibraryDensity.valueOf(it) }.getOrNull() }
                ?: LibraryDensity.COMFORTABLE,
            comicRtl = prefs[Keys.COMIC_RTL] ?: false,
            comicDoubleSpread = prefs[Keys.COMIC_DOUBLE_SPREAD] ?: false,
            pdfMarginCrop = prefs[Keys.PDF_MARGIN_CROP] ?: false,
            pdfDarkInvert = prefs[Keys.PDF_DARK_INVERT] ?: false,
            ttsRate = prefs[Keys.TTS_RATE] ?: 1.0f,
            ttsPitch = prefs[Keys.TTS_PITCH] ?: 1.0f,
            libraryViewMode = prefs[Keys.LIBRARY_VIEW_MODE]?.let { runCatching { LibraryViewMode.valueOf(it) }.getOrNull() }
                ?: LibraryViewMode.GRID,
            librarySortOrder = prefs[Keys.LIBRARY_SORT_ORDER]?.let { runCatching { LibrarySortOrder.valueOf(it) }.getOrNull() }
                ?: LibrarySortOrder.LAST_OPENED
        )
    }

    val preferencesFlow: Flow<ReaderPreferences> = context.dataStore.data.map { readPrefs(it) }

    suspend fun update(transform: (ReaderPreferences) -> ReaderPreferences) {
        context.dataStore.edit { prefs ->
            val current = readPrefs(prefs)
            val next = transform(current)
            prefs[Keys.THEME_MODE] = next.themeMode.name
            prefs[Keys.CUSTOM_BG] = next.customBgHex
            prefs[Keys.CUSTOM_INK] = next.customInkHex
            prefs[Keys.READER_FONT] = next.readerFont.name
            if (next.customFontPath != null) prefs[Keys.CUSTOM_FONT_PATH] = next.customFontPath else prefs.remove(Keys.CUSTOM_FONT_PATH)
            if (next.customFontName != null) prefs[Keys.CUSTOM_FONT_NAME] = next.customFontName else prefs.remove(Keys.CUSTOM_FONT_NAME)
            prefs[Keys.FONT_SIZE] = next.fontSizeSp
            prefs[Keys.LINE_HEIGHT] = next.lineHeightMultiplier
            prefs[Keys.LETTER_SPACING] = next.letterSpacingEm
            prefs[Keys.PARAGRAPH_SPACING] = next.paragraphSpacingDp
            prefs[Keys.FIRST_LINE_INDENT] = next.firstLineIndentDp
            prefs[Keys.HORIZONTAL_MARGIN] = next.horizontalMarginDp
            prefs[Keys.VERTICAL_PADDING] = next.verticalPaddingDp
            prefs[Keys.MAX_COLUMN_WIDTH] = next.maxColumnWidthDp
            prefs[Keys.TEXT_JUSTIFIED] = next.textJustified
            prefs[Keys.AUTO_HYPHENATION] = next.autoHyphenation
            prefs[Keys.WARMTH_TINT] = next.warmthTint
            prefs[Keys.TAP_ZONE] = next.tapZonePreset.name
            prefs[Keys.VOLUME_PAGING] = next.volumeKeyPaging
            prefs[Keys.SWIPE_BRIGHTNESS] = next.swipeBrightness
            prefs[Keys.SCREEN_BRIGHTNESS] = next.screenBrightness
            prefs[Keys.KEEP_SCREEN_AWAKE] = next.keepScreenAwake
            prefs[Keys.PAGE_ANIMATION] = next.pageAnimation.name
            prefs[Keys.ORIENTATION_LOCK] = next.orientationLock.name
            prefs[Keys.SHOW_STATS] = next.showStats
            prefs[Keys.SHOW_CONTINUE_READING] = next.showContinueReading
            prefs[Keys.SHOW_PROGRESS_BARS] = next.showProgressBars
            prefs[Keys.SHOW_FORMAT_BADGES] = next.showFormatBadges
            prefs[Keys.SHOW_READER_FOOTER] = next.showReaderFooter
            prefs[Keys.SHOW_CLOCK_IN_READER] = next.showClockInReader
            prefs[Keys.SHOW_ETA_IN_READER] = next.showEtaInReader
            prefs[Keys.ENABLE_HIGHLIGHTS] = next.enableHighlights
            prefs[Keys.ENABLE_TTS] = next.enableTts
            prefs[Keys.APP_DARK_THEME] = when (next.appDarkTheme) {
                true -> "DARK"
                false -> "LIGHT"
                null -> "SYSTEM"
            }
            prefs[Keys.APP_ACCENT] = next.appAccent.name
            prefs[Keys.LIBRARY_DENSITY] = next.libraryDensity.name
            prefs[Keys.COMIC_RTL] = next.comicRtl
            prefs[Keys.COMIC_DOUBLE_SPREAD] = next.comicDoubleSpread
            prefs[Keys.PDF_MARGIN_CROP] = next.pdfMarginCrop
            prefs[Keys.PDF_DARK_INVERT] = next.pdfDarkInvert
            prefs[Keys.TTS_RATE] = next.ttsRate
            prefs[Keys.TTS_PITCH] = next.ttsPitch
            prefs[Keys.LIBRARY_VIEW_MODE] = next.libraryViewMode.name
            prefs[Keys.LIBRARY_SORT_ORDER] = next.librarySortOrder.name
        }
    }
}
