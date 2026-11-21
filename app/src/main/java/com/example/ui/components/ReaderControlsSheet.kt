package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.FormatCategory
import com.example.data.model.OrientationLock
import com.example.data.model.ReaderFont
import com.example.data.model.ReaderPreferences
import com.example.data.model.ReaderThemeMode
import com.example.engine.TtsState
import com.example.ui.theme.ReaderAmoledBg
import com.example.ui.theme.ReaderAmoledInk
import com.example.ui.theme.ReaderPaperBg
import com.example.ui.theme.ReaderPaperInk
import com.example.ui.theme.ReaderSepiaBg
import com.example.ui.theme.ReaderSepiaInk
import com.example.ui.theme.ReaderSlateBg
import com.example.ui.theme.ReaderSlateInk
import java.util.Locale
import kotlin.math.roundToInt

private data class CustomThemePreset(val name: String, val bgHex: Long, val inkHex: Long)

private val customThemePresets = listOf(
    CustomThemePreset("Ivory", 0xFFF9F4E8, 0xFF231F1B),
    CustomThemePreset("Sage Mist", 0xFFE6EFE8, 0xFF1A2920),
    CustomThemePreset("Dusk", 0xFF23212E, 0xFFE2DFF0),
    CustomThemePreset("Navy", 0xFF141E2B, 0xFFD9E4F2),
    CustomThemePreset("Espresso", 0xFF221A16, 0xFFEADFD5)
)

private val customBgSwatches = listOf(
    0xFFFFFFFFL,
    0xFFF9F4E8L,
    0xFFEBE4D4L,
    0xFFE6EFE8L,
    0xFF23212EL,
    0xFF141E2BL,
    0xFF000000L
)

private val customInkSwatches = listOf(
    0xFF1A1714L,
    0xFF2E3830L,
    0xFF5A4636L,
    0xFFD8D6D0L,
    0xFFE2DFF0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderControlsSheet(
    category: FormatCategory,
    preferences: ReaderPreferences,
    ttsState: TtsState,
    onDismiss: () -> Unit,
    onUpdatePrefs: ((ReaderPreferences) -> ReaderPreferences) -> Unit,
    onToggleTts: () -> Unit,
    onSetSleepTimer: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Reading Controls", style = MaterialTheme.typography.titleLarge)

            if (category == FormatCategory.REFLOWABLE) {
                Text("Theme", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val themes = listOf(
                        Triple(ReaderThemeMode.PAPER, ReaderPaperBg, ReaderPaperInk),
                        Triple(ReaderThemeMode.SEPIA, ReaderSepiaBg, ReaderSepiaInk),
                        Triple(ReaderThemeMode.SLATE, ReaderSlateBg, ReaderSlateInk),
                        Triple(ReaderThemeMode.AMOLED, ReaderAmoledBg, ReaderAmoledInk),
                        Triple(ReaderThemeMode.CUSTOM, Color(preferences.customBgHex), Color(preferences.customInkHex))
                    )
                    themes.forEach { (mode, bg, ink) ->
                        val selected = preferences.themeMode == mode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdatePrefs { it.copy(themeMode = mode) } }
                                .testTag("theme_mode_${mode.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(bg)
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Aa", color = ink, style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(mode.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (preferences.themeMode == ReaderThemeMode.CUSTOM) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Custom Palette Presets", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                customThemePresets.forEach { preset ->
                                    val active = preferences.customBgHex == preset.bgHex
                                    FilterChip(
                                        selected = active,
                                        onClick = {
                                            onUpdatePrefs {
                                                it.copy(customBgHex = preset.bgHex, customInkHex = preset.inkHex)
                                            }
                                        },
                                        label = { Text(preset.name) }
                                    )
                                }
                            }
                            Text("Page Surface", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                customBgSwatches.forEach { hex ->
                                    val active = preferences.customBgHex == hex
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(hex))
                                            .border(
                                                width = if (active) 2.5.dp else 1.dp,
                                                color = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                                shape = CircleShape
                                            )
                                            .clickable { onUpdatePrefs { it.copy(customBgHex = hex) } }
                                    )
                                }
                            }
                            Text("Ink Tone", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                customInkSwatches.forEach { hex ->
                                    val active = preferences.customInkHex == hex
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(hex))
                                            .border(
                                                width = if (active) 2.5.dp else 1.dp,
                                                color = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                                shape = CircleShape
                                            )
                                            .clickable { onUpdatePrefs { it.copy(customInkHex = hex) } }
                                    )
                                }
                            }
                        }
                    }
                }

                Text("Typeface", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReaderFont.entries.filter {
                        it != ReaderFont.CUSTOM_FILE || preferences.customFontPath != null
                    }.forEach { font ->
                        val label = if (font == ReaderFont.CUSTOM_FILE) {
                            preferences.customFontName ?: "Custom"
                        } else font.label
                        FilterChip(
                            selected = preferences.readerFont == font,
                            onClick = { onUpdatePrefs { it.copy(readerFont = font) } },
                            label = { Text(label) },
                            modifier = Modifier.testTag("font_chip_${font.name.lowercase()}")
                        )
                    }
                }

                LabeledSlider(
                    label = "Font Size",
                    valueText = "${preferences.fontSizeSp.roundToInt()} sp",
                    value = preferences.fontSizeSp,
                    range = 13f..30f,
                    onValueChange = { v -> onUpdatePrefs { it.copy(fontSizeSp = v) } }
                )
                LabeledSlider(
                    label = "Line Spacing",
                    valueText = String.format(Locale.US, "%.2fx", preferences.lineHeightMultiplier),
                    value = preferences.lineHeightMultiplier,
                    range = 1.2f..2.1f,
                    onValueChange = { v -> onUpdatePrefs { it.copy(lineHeightMultiplier = v) } }
                )
                LabeledSlider(
                    label = "Letter Spacing",
                    valueText = if (kotlin.math.abs(preferences.letterSpacingEm) < 0.005f) "Normal"
                    else String.format(Locale.US, "%+.2fem", preferences.letterSpacingEm),
                    value = preferences.letterSpacingEm,
                    range = -0.02f..0.08f,
                    onValueChange = { v -> onUpdatePrefs { it.copy(letterSpacingEm = v) } }
                )
                LabeledSlider(
                    label = "Paragraph Spacing",
                    valueText = "${preferences.paragraphSpacingDp.roundToInt()} dp",
                    value = preferences.paragraphSpacingDp,
                    range = 4f..28f,
                    onValueChange = { v -> onUpdatePrefs { it.copy(paragraphSpacingDp = v) } }
                )
                LabeledSlider(
                    label = "First-Line Indent",
                    valueText = if (preferences.firstLineIndentDp < 1f) "Off" else "${preferences.firstLineIndentDp.roundToInt()} dp",
                    value = preferences.firstLineIndentDp,
                    range = 0f..28f,
                    onValueChange = { v -> onUpdatePrefs { it.copy(firstLineIndentDp = v) } }
                )
                LabeledSlider(
                    label = "Side Margins",
                    valueText = "${preferences.horizontalMarginDp.roundToInt()} dp",
                    value = preferences.horizontalMarginDp,
                    range = 8f..48f,
                    onValueChange = { v -> onUpdatePrefs { it.copy(horizontalMarginDp = v) } }
                )
                LabeledSlider(
                    label = "Max Column Width",
                    valueText = "${preferences.maxColumnWidthDp.roundToInt()} dp",
                    value = preferences.maxColumnWidthDp,
                    range = 480f..860f,
                    onValueChange = { v -> onUpdatePrefs { it.copy(maxColumnWidthDp = v) } }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Justify Paragraphs", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = preferences.textJustified,
                        onCheckedChange = { checked -> onUpdatePrefs { it.copy(textJustified = checked) } }
                    )
                }

                if (preferences.enableTts) {
                    HorizontalDivider()
                    Text("Offline Text-to-Speech", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onToggleTts,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("sheet_tts_toggle")
                        ) {
                            Text(if (ttsState.isSpeaking) "Stop Speaking" else "Read Aloud")
                        }
                        listOf(0, 15, 30).forEach { mins ->
                            FilterChip(
                                selected = (mins > 0 && ttsState.sleepMinutesRemaining > 0 && ttsState.sleepMinutesRemaining <= mins),
                                onClick = { onSetSleepTimer(mins) },
                                label = { Text(if (mins == 0) "Timer Off" else "${mins}m") }
                            )
                        }
                    }
                    LabeledSlider(
                        label = "Speech Speed",
                        valueText = String.format(Locale.US, "%.2fx", preferences.ttsRate),
                        value = preferences.ttsRate,
                        range = 0.6f..2.0f,
                        onValueChange = { v -> onUpdatePrefs { it.copy(ttsRate = v) } }
                    )
                    LabeledSlider(
                        label = "Speech Pitch",
                        valueText = String.format(Locale.US, "%.2fx", preferences.ttsPitch),
                        value = preferences.ttsPitch,
                        range = 0.75f..1.35f,
                        onValueChange = { v -> onUpdatePrefs { it.copy(ttsPitch = v) } }
                    )
                }
            } else if (category == FormatCategory.PDF) {
                Text("PDF Viewing", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto-Crop White Margins", style = MaterialTheme.typography.bodyLarge)
                        Text("Zooms cleanly into printed text columns", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = preferences.pdfMarginCrop,
                        onCheckedChange = { v -> onUpdatePrefs { it.copy(pdfMarginCrop = v) } },
                        modifier = Modifier.testTag("pdf_margin_crop_switch")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Night Mode Invert", style = MaterialTheme.typography.bodyLarge)
                        Text("Dark page background with soft light text", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = preferences.pdfDarkInvert,
                        onCheckedChange = { v -> onUpdatePrefs { it.copy(pdfDarkInvert = v) } },
                        modifier = Modifier.testTag("pdf_dark_invert_switch")
                    )
                }
            } else {
                Text("Comic & Manga Viewing", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Right-to-Left (Manga)", style = MaterialTheme.typography.bodyLarge)
                        Text("Reverses page-turn direction", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = preferences.comicRtl,
                        onCheckedChange = { v -> onUpdatePrefs { it.copy(comicRtl = v) } },
                        modifier = Modifier.testTag("comic_rtl_switch")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Double-Page Spread", style = MaterialTheme.typography.bodyLarge)
                        Text("View two comic pages side-by-side", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = preferences.comicDoubleSpread,
                        onCheckedChange = { v -> onUpdatePrefs { it.copy(comicDoubleSpread = v) } },
                        modifier = Modifier.testTag("comic_spread_switch")
                    )
                }
            }

            HorizontalDivider()
            Text("Eye Comfort & Screen", style = MaterialTheme.typography.labelLarge)
            LabeledSlider(
                label = "Warmth Tint",
                valueText = "${(preferences.warmthTint / 0.45f * 100f).roundToInt()}%",
                value = preferences.warmthTint,
                range = 0f..0.45f,
                onValueChange = { v -> onUpdatePrefs { it.copy(warmthTint = v) } }
            )
            Text("Orientation", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrientationLock.entries.forEach { lock ->
                    FilterChip(
                        selected = preferences.orientationLock == lock,
                        onClick = { onUpdatePrefs { it.copy(orientationLock = lock) } },
                        label = { Text(lock.label) },
                        modifier = Modifier.testTag("sheet_orientation_${lock.name.lowercase()}")
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Keep Screen Awake", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = preferences.keepScreenAwake,
                    onCheckedChange = { v -> onUpdatePrefs { it.copy(keepScreenAwake = v) } },
                    modifier = Modifier.testTag("sheet_keep_awake_switch")
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}
