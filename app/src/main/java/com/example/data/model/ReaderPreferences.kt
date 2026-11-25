package com.example.data.model

enum class ReaderThemeMode(val label: String) {
    PAPER("Paper"),
    SEPIA("Sepia"),
    SLATE("Slate"),
    AMOLED("OLED"),
    CUSTOM("Custom")
}

enum class ReaderFont(val label: String) {
    LITERATA("Literata"),
    MERRIWEATHER("Merriweather"),
    LORA("Lora"),
    EB_GARAMOND("Garamond"),
    ROBOTO_SERIF("Roboto Serif"),
    ROBOTO_SLAB("Roboto Slab"),
    OPEN_SANS("Open Sans"),
    NUNITO("Nunito"),
    LEXEND("Lexend"),
    PLUS_JAKARTA_SANS("Sans"),
    JETBRAINS_MONO("Mono"),
    SYSTEM_SERIF("System Serif"),
    CUSTOM_FILE("Custom Font")
}

enum class ReaderFontWeight(val label: String, val weightValue: Int) {
    THIN("Thin", 200),
    LIGHT("Light", 300),
    REGULAR("Regular", 400),
    MEDIUM("Medium", 500),
    SEMIBOLD("SemiBold", 600)
}

enum class ReaderTextAlignment(val label: String) {
    START("Start"),
    JUSTIFY("Justify"),
    CENTER("Center"),
    END("End")
}

enum class HorizontalGestureMode(val label: String) {
    OFF("Off"),
    NORMAL("On"),
    INVERTED("On (Inverted)")
}

enum class ImageColorEffect(val label: String) {
    OFF("Off"),
    GRAYSCALE("Grayscale"),
    TEXT_COLOR("Text Color"),
    BG_COLOR("Bg Blend")
}

enum class ProgressDisplayMode(val label: String) {
    PERCENT("Percentage"),
    POSITION_COUNT("Position")
}

enum class DoubleTapAction(val label: String) {
    OFF("Off"),
    SPEAK_PARAGRAPH("Read Aloud"),
    QUICK_HIGHLIGHT("Highlight"),
    EXTERNAL_TRANSLATE("Translator")
}

enum class TapZonePreset(val label: String, val description: String) {
    CLASSIC_LR("Classic Left / Right", "Left 30% back, right 30% forward, center menu"),
    ONE_HAND_RIGHT("One-Handed Right", "Top 35% back, bottom/right 40% forward, center menu"),
    SCROLL_ONLY("Scroll Only", "Disable page-jump taps; center tap toggles menu")
}

enum class ReaderReadingMode(val label: String) {
    SCROLL("Vertical Scroll"),
    PAGED("Page-by-Page")
}

enum class PageAnimation(val label: String) {
    NONE("Instant"),
    SLIDE("Slide"),
    FADE("Fade")
}

enum class OrientationLock(val label: String) {
    SYSTEM("System"),
    FREE("Free"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    LOCKED_PORTRAIT("Locked Portrait"),
    LOCKED_LANDSCAPE("Locked Landscape")
}

enum class LibraryViewMode {
    GRID,
    LIST
}

enum class LibraryDensity(val label: String, val minCellDp: Int) {
    COMPACT("Compact", 116),
    COMFORTABLE("Balanced", 148),
    LARGE("Large", 184)
}

enum class AppAccent(val label: String, val lightHex: Long, val darkHex: Long) {
    TERRACOTTA("Terracotta", 0xFFC86D3B, 0xFFE89B71),
    FOREST("Forest", 0xFF2D6A4F, 0xFF74C69D),
    COBALT("Cobalt", 0xFF2B59C3, 0xFF82AAFF),
    PLUM("Plum", 0xFF7B3E65, 0xFFD99AC5),
    AMBER("Amber", 0xFFB4690E, 0xFFF2B866)
}

enum class LibrarySortOrder(val label: String) {
    LAST_OPENED("Recently Read"),
    ADDED_DESC("Recently Added"),
    TITLE_ASC("Title A–Z"),
    AUTHOR_ASC("Author A–Z"),
    PROGRESS_DESC("Progress")
}

data class ReaderColorPreset(
    val id: String,
    val name: String,
    val bgHex: Long,
    val inkHex: Long
)

fun defaultReaderColorPresets(): List<ReaderColorPreset> = listOf(
    ReaderColorPreset("paper", "Warm Paper", 0xFFF9F4E8, 0xFF231F1B),
    ReaderColorPreset("sepia", "Sepia", 0xFFF2E6D0, 0xFF423121),
    ReaderColorPreset("sage", "Sage Mist", 0xFFE6EFE8, 0xFF1A2920),
    ReaderColorPreset("slate", "Slate Dark", 0xFF1D2228, 0xFFDFE5EE),
    ReaderColorPreset("amoled", "AMOLED", 0xFF000000, 0xFFD4D0C8)
)

data class ReaderPreferences(
    val themeMode: ReaderThemeMode = ReaderThemeMode.PAPER,
    val customBgHex: Long = 0xFFF9F4E8,
    val customInkHex: Long = 0xFF231F1B,
    val activeColorPresetId: String = "paper",
    val colorPresets: List<ReaderColorPreset> = defaultReaderColorPresets(),
    val quickColorPresetSwipe: Boolean = true,
    val readerFont: ReaderFont = ReaderFont.LITERATA,
    val readerFontWeight: ReaderFontWeight = ReaderFontWeight.REGULAR,
    val italicText: Boolean = false,
    val customFontPath: String? = null,
    val customFontName: String? = null,
    val fontSizeSp: Float = 18f,
    val lineHeightMultiplier: Float = 1.55f,
    val letterSpacingEm: Float = 0f,
    val paragraphSpacingDp: Float = 12f,
    val firstLineIndentDp: Float = 0f,
    val horizontalMarginDp: Float = 22f,
    val verticalPaddingDp: Float = 16f,
    val cutoutMargin: Boolean = true,
    val bottomBarMarginDp: Float = 0f,
    val maxColumnWidthDp: Float = 720f,
    val textAlignment: ReaderTextAlignment = ReaderTextAlignment.START,
    val chapterTitleAlignment: ReaderTextAlignment = ReaderTextAlignment.START,
    val autoHyphenation: Boolean = true,
    // Speed reading & focus helpers
    val bionicReading: Boolean = false,
    val perceptionExpander: Boolean = false,
    // Image styling
    val showImages: Boolean = true,
    val showImageCaptions: Boolean = true,
    val imageColorEffect: ImageColorEffect = ImageColorEffect.OFF,
    val imageCornerRadiusDp: Float = 8f,
    val imageAlignment: ReaderTextAlignment = ReaderTextAlignment.CENTER,
    val imageWidthPercent: Float = 1.0f,
    // Eye comfort & screen
    val warmthTint: Float = 0f,
    val tapZonePreset: TapZonePreset = TapZonePreset.CLASSIC_LR,
    val readingMode: ReaderReadingMode = ReaderReadingMode.SCROLL,
    val horizontalGestureMode: HorizontalGestureMode = HorizontalGestureMode.NORMAL,
    val volumeKeyPaging: Boolean = true,
    val swipeBrightness: Boolean = true,
    val customBrightnessEnabled: Boolean = false,
    val screenBrightness: Float = 0.5f,
    val fullscreenMode: Boolean = false,
    val hideBarsOnFastScroll: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val pageAnimation: PageAnimation = PageAnimation.SLIDE,
    val orientationLock: OrientationLock = OrientationLock.SYSTEM,
    val progressDisplayMode: ProgressDisplayMode = ProgressDisplayMode.PERCENT,
    val doubleTapAction: DoubleTapAction = DoubleTapAction.OFF,
    // Toggleable modular features
    val showStats: Boolean = true,
    val showContinueReading: Boolean = true,
    val showProgressBars: Boolean = true,
    val showFormatBadges: Boolean = true,
    val showReaderFooter: Boolean = true,
    val showClockInReader: Boolean = false,
    val showEtaInReader: Boolean = true,
    val enableHighlights: Boolean = true,
    val enableTts: Boolean = true,
    // Library & app appearance
    val appDarkTheme: Boolean? = null,
    val appAccent: AppAccent = AppAccent.TERRACOTTA,
    val libraryDensity: LibraryDensity = LibraryDensity.COMFORTABLE,
    val groupSeriesInLibrary: Boolean = true,
    val comicRtl: Boolean = false,
    val comicDoubleSpread: Boolean = false,
    val pdfMarginCrop: Boolean = false,
    val pdfDarkInvert: Boolean = false,
    val ttsRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val libraryViewMode: LibraryViewMode = LibraryViewMode.GRID,
    val librarySortOrder: LibrarySortOrder = LibrarySortOrder.LAST_OPENED
) {
    val textJustified: Boolean get() = textAlignment == ReaderTextAlignment.JUSTIFY
}
