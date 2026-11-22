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
import com.example.data.model.DoubleTapAction
import com.example.data.model.HorizontalGestureMode
import com.example.data.model.ImageColorEffect
import com.example.data.model.LibraryDensity
import com.example.data.model.LibrarySortOrder
import com.example.data.model.LibraryViewMode
import com.example.data.model.OrientationLock
import com.example.data.model.PageAnimation
import com.example.data.model.ProgressDisplayMode
import com.example.data.model.ReaderColorPreset
import com.example.data.model.ReaderFont
import com.example.data.model.ReaderFontWeight
import com.example.data.model.ReaderPreferences
import com.example.data.model.ReaderTextAlignment
import com.example.data.model.ReaderThemeMode
import com.example.data.model.TapZonePreset
import com.example.data.model.defaultReaderColorPresets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "folio_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CUSTOM_BG = longPreferencesKey("custom_bg")
        val CUSTOM_INK = longPreferencesKey("custom_ink")
        val ACTIVE_COLOR_PRESET_ID = stringPreferencesKey("active_color_preset_id")
        val COLOR_PRESETS_JSON = stringPreferencesKey("color_presets_json")
        val QUICK_COLOR_PRESET_SWIPE = booleanPreferencesKey("quick_color_preset_swipe")
        val READER_FONT = stringPreferencesKey("reader_font")
        val READER_FONT_WEIGHT = stringPreferencesKey("reader_font_weight")
        val ITALIC_TEXT = booleanPreferencesKey("italic_text")
        val CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
        val CUSTOM_FONT_NAME = stringPreferencesKey("custom_font_name")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val LINE_HEIGHT = floatPreferencesKey("line_height")
        val LETTER_SPACING = floatPreferencesKey("letter_spacing")
        val PARAGRAPH_SPACING = floatPreferencesKey("paragraph_spacing")
        val FIRST_LINE_INDENT = floatPreferencesKey("first_line_indent")
        val HORIZONTAL_MARGIN = floatPreferencesKey("horizontal_margin")
        val VERTICAL_PADDING = floatPreferencesKey("vertical_padding")
        val CUTOUT_MARGIN = booleanPreferencesKey("cutout_margin")
        val BOTTOM_BAR_MARGIN = floatPreferencesKey("bottom_bar_margin")
        val MAX_COLUMN_WIDTH = floatPreferencesKey("max_column_width")
        val TEXT_JUSTIFIED = booleanPreferencesKey("text_justified")
        val TEXT_ALIGNMENT = stringPreferencesKey("text_alignment")
        val CHAPTER_TITLE_ALIGNMENT = stringPreferencesKey("chapter_title_alignment")
        val AUTO_HYPHENATION = booleanPreferencesKey("auto_hyphenation")
        val BIONIC_READING = booleanPreferencesKey("bionic_reading")
        val PERCEPTION_EXPANDER = booleanPreferencesKey("perception_expander")
        val SHOW_IMAGES = booleanPreferencesKey("show_images")
        val SHOW_IMAGE_CAPTIONS = booleanPreferencesKey("show_image_captions")
        val IMAGE_COLOR_EFFECT = stringPreferencesKey("image_color_effect")
        val IMAGE_CORNER_RADIUS = floatPreferencesKey("image_corner_radius")
        val IMAGE_ALIGNMENT = stringPreferencesKey("image_alignment")
        val IMAGE_WIDTH_PERCENT = floatPreferencesKey("image_width_percent")
        val WARMTH_TINT = floatPreferencesKey("warmth_tint")
        val TAP_ZONE = stringPreferencesKey("tap_zone")
        val HORIZONTAL_GESTURE_MODE = stringPreferencesKey("horizontal_gesture_mode")
        val VOLUME_PAGING = booleanPreferencesKey("volume_paging")
        val SWIPE_BRIGHTNESS = booleanPreferencesKey("swipe_brightness")
        val CUSTOM_BRIGHTNESS_ENABLED = booleanPreferencesKey("custom_brightness_enabled")
        val SCREEN_BRIGHTNESS = floatPreferencesKey("screen_brightness")
        val FULLSCREEN_MODE = booleanPreferencesKey("fullscreen_mode")
        val HIDE_BARS_ON_FAST_SCROLL = booleanPreferencesKey("hide_bars_on_fast_scroll")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val PAGE_ANIMATION = stringPreferencesKey("page_animation")
        val ORIENTATION_LOCK = stringPreferencesKey("orientation_lock")
        val PROGRESS_DISPLAY_MODE = stringPreferencesKey("progress_display_mode")
        val DOUBLE_TAP_ACTION = stringPreferencesKey("double_tap_action")
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
        val legacyJustified = prefs[Keys.TEXT_JUSTIFIED] ?: false
        val textAlignment = prefs[Keys.TEXT_ALIGNMENT]?.let {
            runCatching { ReaderTextAlignment.valueOf(it) }.getOrNull()
        } ?: if (legacyJustified) ReaderTextAlignment.JUSTIFY else ReaderTextAlignment.START

        val legacyBrightness = prefs[Keys.SCREEN_BRIGHTNESS]
        val customBrightnessEnabled = prefs[Keys.CUSTOM_BRIGHTNESS_ENABLED]
            ?: (legacyBrightness != null && legacyBrightness >= 0f)

        return ReaderPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ReaderThemeMode.valueOf(it) }.getOrNull() }
                ?: ReaderThemeMode.PAPER,
            customBgHex = prefs[Keys.CUSTOM_BG] ?: 0xFFF9F4E8,
            customInkHex = prefs[Keys.CUSTOM_INK] ?: 0xFF231F1B,
            activeColorPresetId = prefs[Keys.ACTIVE_COLOR_PRESET_ID] ?: "paper",
            colorPresets = decodeColorPresets(prefs[Keys.COLOR_PRESETS_JSON]),
            quickColorPresetSwipe = prefs[Keys.QUICK_COLOR_PRESET_SWIPE] ?: true,
            readerFont = prefs[Keys.READER_FONT]?.let { runCatching { ReaderFont.valueOf(it) }.getOrNull() }
                ?: ReaderFont.LITERATA,
            readerFontWeight = prefs[Keys.READER_FONT_WEIGHT]?.let { runCatching { ReaderFontWeight.valueOf(it) }.getOrNull() }
                ?: ReaderFontWeight.REGULAR,
            italicText = prefs[Keys.ITALIC_TEXT] ?: false,
            customFontPath = prefs[Keys.CUSTOM_FONT_PATH],
            customFontName = prefs[Keys.CUSTOM_FONT_NAME],
            fontSizeSp = prefs[Keys.FONT_SIZE] ?: 18f,
            lineHeightMultiplier = prefs[Keys.LINE_HEIGHT] ?: 1.55f,
            letterSpacingEm = prefs[Keys.LETTER_SPACING] ?: 0f,
            paragraphSpacingDp = prefs[Keys.PARAGRAPH_SPACING] ?: 12f,
            firstLineIndentDp = prefs[Keys.FIRST_LINE_INDENT] ?: 0f,
            horizontalMarginDp = prefs[Keys.HORIZONTAL_MARGIN] ?: 22f,
            verticalPaddingDp = prefs[Keys.VERTICAL_PADDING] ?: 16f,
            cutoutMargin = prefs[Keys.CUTOUT_MARGIN] ?: true,
            bottomBarMarginDp = prefs[Keys.BOTTOM_BAR_MARGIN] ?: 0f,
            maxColumnWidthDp = prefs[Keys.MAX_COLUMN_WIDTH] ?: 720f,
            textAlignment = textAlignment,
            chapterTitleAlignment = prefs[Keys.CHAPTER_TITLE_ALIGNMENT]?.let {
                runCatching { ReaderTextAlignment.valueOf(it) }.getOrNull()
            } ?: ReaderTextAlignment.START,
            autoHyphenation = prefs[Keys.AUTO_HYPHENATION] ?: true,
            bionicReading = prefs[Keys.BIONIC_READING] ?: false,
            perceptionExpander = prefs[Keys.PERCEPTION_EXPANDER] ?: false,
            showImages = prefs[Keys.SHOW_IMAGES] ?: true,
            showImageCaptions = prefs[Keys.SHOW_IMAGE_CAPTIONS] ?: true,
            imageColorEffect = prefs[Keys.IMAGE_COLOR_EFFECT]?.let {
                runCatching { ImageColorEffect.valueOf(it) }.getOrNull()
            } ?: ImageColorEffect.OFF,
            imageCornerRadiusDp = prefs[Keys.IMAGE_CORNER_RADIUS] ?: 8f,
            imageAlignment = prefs[Keys.IMAGE_ALIGNMENT]?.let {
                runCatching { ReaderTextAlignment.valueOf(it) }.getOrNull()
            } ?: ReaderTextAlignment.CENTER,
            imageWidthPercent = prefs[Keys.IMAGE_WIDTH_PERCENT] ?: 1.0f,
            warmthTint = prefs[Keys.WARMTH_TINT] ?: 0f,
            tapZonePreset = prefs[Keys.TAP_ZONE]?.let { runCatching { TapZonePreset.valueOf(it) }.getOrNull() }
                ?: TapZonePreset.CLASSIC_LR,
            horizontalGestureMode = prefs[Keys.HORIZONTAL_GESTURE_MODE]?.let {
                runCatching { HorizontalGestureMode.valueOf(it) }.getOrNull()
            } ?: HorizontalGestureMode.NORMAL,
            volumeKeyPaging = prefs[Keys.VOLUME_PAGING] ?: true,
            swipeBrightness = prefs[Keys.SWIPE_BRIGHTNESS] ?: true,
            customBrightnessEnabled = customBrightnessEnabled,
            screenBrightness = (legacyBrightness ?: 0.5f).let { if (it < 0f) 0.5f else it.coerceIn(0.05f, 1f) },
            fullscreenMode = prefs[Keys.FULLSCREEN_MODE] ?: false,
            hideBarsOnFastScroll = prefs[Keys.HIDE_BARS_ON_FAST_SCROLL] ?: true,
            keepScreenAwake = prefs[Keys.KEEP_SCREEN_AWAKE] ?: true,
            pageAnimation = prefs[Keys.PAGE_ANIMATION]?.let { runCatching { PageAnimation.valueOf(it) }.getOrNull() }
                ?: PageAnimation.SLIDE,
            orientationLock = prefs[Keys.ORIENTATION_LOCK]?.let { runCatching { OrientationLock.valueOf(it) }.getOrNull() }
                ?: OrientationLock.SYSTEM,
            progressDisplayMode = prefs[Keys.PROGRESS_DISPLAY_MODE]?.let {
                runCatching { ProgressDisplayMode.valueOf(it) }.getOrNull()
            } ?: ProgressDisplayMode.PERCENT,
            doubleTapAction = prefs[Keys.DOUBLE_TAP_ACTION]?.let {
                runCatching { DoubleTapAction.valueOf(it) }.getOrNull()
            } ?: DoubleTapAction.OFF,
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
            prefs[Keys.ACTIVE_COLOR_PRESET_ID] = next.activeColorPresetId
            prefs[Keys.COLOR_PRESETS_JSON] = encodeColorPresets(next.colorPresets)
            prefs[Keys.QUICK_COLOR_PRESET_SWIPE] = next.quickColorPresetSwipe
            prefs[Keys.READER_FONT] = next.readerFont.name
            prefs[Keys.READER_FONT_WEIGHT] = next.readerFontWeight.name
            prefs[Keys.ITALIC_TEXT] = next.italicText
            if (next.customFontPath != null) prefs[Keys.CUSTOM_FONT_PATH] = next.customFontPath else prefs.remove(Keys.CUSTOM_FONT_PATH)
            if (next.customFontName != null) prefs[Keys.CUSTOM_FONT_NAME] = next.customFontName else prefs.remove(Keys.CUSTOM_FONT_NAME)
            prefs[Keys.FONT_SIZE] = next.fontSizeSp
            prefs[Keys.LINE_HEIGHT] = next.lineHeightMultiplier
            prefs[Keys.LETTER_SPACING] = next.letterSpacingEm
            prefs[Keys.PARAGRAPH_SPACING] = next.paragraphSpacingDp
            prefs[Keys.FIRST_LINE_INDENT] = next.firstLineIndentDp
            prefs[Keys.HORIZONTAL_MARGIN] = next.horizontalMarginDp
            prefs[Keys.VERTICAL_PADDING] = next.verticalPaddingDp
            prefs[Keys.CUTOUT_MARGIN] = next.cutoutMargin
            prefs[Keys.BOTTOM_BAR_MARGIN] = next.bottomBarMarginDp
            prefs[Keys.MAX_COLUMN_WIDTH] = next.maxColumnWidthDp
            prefs[Keys.TEXT_JUSTIFIED] = next.textAlignment == ReaderTextAlignment.JUSTIFY
            prefs[Keys.TEXT_ALIGNMENT] = next.textAlignment.name
            prefs[Keys.CHAPTER_TITLE_ALIGNMENT] = next.chapterTitleAlignment.name
            prefs[Keys.AUTO_HYPHENATION] = next.autoHyphenation
            prefs[Keys.BIONIC_READING] = next.bionicReading
            prefs[Keys.PERCEPTION_EXPANDER] = next.perceptionExpander
            prefs[Keys.SHOW_IMAGES] = next.showImages
            prefs[Keys.SHOW_IMAGE_CAPTIONS] = next.showImageCaptions
            prefs[Keys.IMAGE_COLOR_EFFECT] = next.imageColorEffect.name
            prefs[Keys.IMAGE_CORNER_RADIUS] = next.imageCornerRadiusDp
            prefs[Keys.IMAGE_ALIGNMENT] = next.imageAlignment.name
            prefs[Keys.IMAGE_WIDTH_PERCENT] = next.imageWidthPercent
            prefs[Keys.WARMTH_TINT] = next.warmthTint
            prefs[Keys.TAP_ZONE] = next.tapZonePreset.name
            prefs[Keys.HORIZONTAL_GESTURE_MODE] = next.horizontalGestureMode.name
            prefs[Keys.VOLUME_PAGING] = next.volumeKeyPaging
            prefs[Keys.SWIPE_BRIGHTNESS] = next.swipeBrightness
            prefs[Keys.CUSTOM_BRIGHTNESS_ENABLED] = next.customBrightnessEnabled
            prefs[Keys.SCREEN_BRIGHTNESS] = next.screenBrightness
            prefs[Keys.FULLSCREEN_MODE] = next.fullscreenMode
            prefs[Keys.HIDE_BARS_ON_FAST_SCROLL] = next.hideBarsOnFastScroll
            prefs[Keys.KEEP_SCREEN_AWAKE] = next.keepScreenAwake
            prefs[Keys.PAGE_ANIMATION] = next.pageAnimation.name
            prefs[Keys.ORIENTATION_LOCK] = next.orientationLock.name
            prefs[Keys.PROGRESS_DISPLAY_MODE] = next.progressDisplayMode.name
            prefs[Keys.DOUBLE_TAP_ACTION] = next.doubleTapAction.name
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

    private fun encodeColorPresets(presets: List<ReaderColorPreset>): String {
        val arr = JSONArray()
        presets.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("bg", p.bgHex)
                    put("ink", p.inkHex)
                }
            )
        }
        return arr.toString()
    }

    private fun decodeColorPresets(raw: String?): List<ReaderColorPreset> {
        if (raw.isNullOrBlank()) return defaultReaderColorPresets()
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<ReaderColorPreset>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ReaderColorPreset(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        bgHex = obj.getLong("bg"),
                        inkHex = obj.getLong("ink")
                    )
                )
            }
            list.ifEmpty { defaultReaderColorPresets() }
        }.getOrElse { defaultReaderColorPresets() }
    }
}
