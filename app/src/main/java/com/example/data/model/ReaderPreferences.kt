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
    PLUS_JAKARTA_SANS("Sans"),
    JETBRAINS_MONO("Mono"),
    SYSTEM_SERIF("System Serif"),
    CUSTOM_FILE("Custom Font")
}

enum class TapZonePreset(val label: String, val description: String) {
    CLASSIC_LR("Classic Left / Right", "Left 30% back, right 30% forward, center menu"),
    ONE_HAND_RIGHT("One-Handed Right", "Top 35% back, bottom/right 40% forward, center menu"),
    SCROLL_ONLY("Scroll Only", "Disable page-jump taps; center tap toggles menu")
}

enum class PageAnimation(val label: String) {
    NONE("Instant"),
    SLIDE("Slide"),
    FADE("Fade")
}

enum class OrientationLock(val label: String) {
    SYSTEM("System"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
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

data class ReaderPreferences(
    val themeMode: ReaderThemeMode = ReaderThemeMode.PAPER,
    val customBgHex: Long = 0xFFF9F4E8,
    val customInkHex: Long = 0xFF231F1B,
    val readerFont: ReaderFont = ReaderFont.LITERATA,
    val customFontPath: String? = null,
    val customFontName: String? = null,
    val fontSizeSp: Float = 18f,
    val lineHeightMultiplier: Float = 1.55f,
    val letterSpacingEm: Float = 0f,
    val paragraphSpacingDp: Float = 12f,
    val firstLineIndentDp: Float = 0f,
    val horizontalMarginDp: Float = 22f,
    val verticalPaddingDp: Float = 16f,
    val maxColumnWidthDp: Float = 720f,
    val textJustified: Boolean = false,
    val autoHyphenation: Boolean = true,
    val warmthTint: Float = 0f,
    val tapZonePreset: TapZonePreset = TapZonePreset.CLASSIC_LR,
    val volumeKeyPaging: Boolean = true,
    val swipeBrightness: Boolean = true,
    val screenBrightness: Float = -1f,
    val keepScreenAwake: Boolean = true,
    val pageAnimation: PageAnimation = PageAnimation.SLIDE,
    val orientationLock: OrientationLock = OrientationLock.SYSTEM,
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
    val comicRtl: Boolean = false,
    val comicDoubleSpread: Boolean = false,
    val pdfMarginCrop: Boolean = false,
    val pdfDarkInvert: Boolean = false,
    val ttsRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val libraryViewMode: LibraryViewMode = LibraryViewMode.GRID,
    val librarySortOrder: LibrarySortOrder = LibrarySortOrder.LAST_OPENED
)
