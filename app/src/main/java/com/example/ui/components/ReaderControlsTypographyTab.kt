package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.DoubleTapAction
import com.example.data.model.ImageColorEffect
import com.example.data.model.ProgressDisplayMode
import com.example.data.model.ReaderFont
import com.example.data.model.ReaderFontWeight
import com.example.data.model.ReaderPreferences
import com.example.data.model.ReaderTextAlignment
import com.example.engine.TtsState
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReaderControlsTypographyTab(
    preferences: ReaderPreferences,
    ttsState: TtsState,
    onUpdatePrefs: ((ReaderPreferences) -> ReaderPreferences) -> Unit,
    onToggleTts: () -> Unit,
    onSetSleepTimer: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ControlsSectionTitle("Font")
        Text("Font Family", style = MaterialTheme.typography.bodyMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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

        Text("Font Weight", style = MaterialTheme.typography.bodyMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ReaderFontWeight.entries.forEach { weight ->
                FilterChip(
                    selected = preferences.readerFontWeight == weight,
                    onClick = { onUpdatePrefs { it.copy(readerFontWeight = weight) } },
                    label = { Text(weight.label) }
                )
            }
        }

        Text("Font Style", style = MaterialTheme.typography.bodyMedium)
        ControlsSegmentedRow(
            options = listOf(false, true),
            selected = preferences.italicText,
            labelOf = { if (it) "Italic" else "Normal" },
            onSelect = { italic -> onUpdatePrefs { it.copy(italicText = italic) } }
        )

        ControlsLabeledSlider(
            label = "Font Size",
            valueText = "${preferences.fontSizeSp.roundToInt()} pt",
            value = preferences.fontSizeSp,
            range = 12f..32f,
            onValueChange = { v -> onUpdatePrefs { it.copy(fontSizeSp = v) } }
        )
        ControlsLabeledSlider(
            label = "Letter Spacing",
            valueText = if (kotlin.math.abs(preferences.letterSpacingEm) < 0.005f) "0pt"
            else String.format(Locale.US, "%+.2fem", preferences.letterSpacingEm),
            value = preferences.letterSpacingEm,
            range = -0.02f..0.10f,
            onValueChange = { v -> onUpdatePrefs { it.copy(letterSpacingEm = v) } }
        )

        HorizontalDivider()
        ControlsSectionTitle("Text")
        Text("Text Alignment", style = MaterialTheme.typography.bodyMedium)
        ControlsSegmentedRow(
            options = ReaderTextAlignment.entries,
            selected = preferences.textAlignment,
            labelOf = { it.label },
            onSelect = { align -> onUpdatePrefs { it.copy(textAlignment = align) } }
        )
        ControlsLabeledSlider(
            label = "Line Height",
            valueText = String.format(Locale.US, "%.2fx", preferences.lineHeightMultiplier),
            value = preferences.lineHeightMultiplier,
            range = 1.15f..2.2f,
            onValueChange = { v -> onUpdatePrefs { it.copy(lineHeightMultiplier = v) } }
        )
        ControlsLabeledSlider(
            label = "Paragraph Spacing",
            valueText = "${preferences.paragraphSpacingDp.roundToInt()} pt",
            value = preferences.paragraphSpacingDp,
            range = 0f..32f,
            onValueChange = { v -> onUpdatePrefs { it.copy(paragraphSpacingDp = v) } }
        )
        ControlsLabeledSlider(
            label = "Paragraph Indent",
            valueText = "${preferences.firstLineIndentDp.roundToInt()} pt",
            value = preferences.firstLineIndentDp,
            range = 0f..32f,
            onValueChange = { v -> onUpdatePrefs { it.copy(firstLineIndentDp = v) } }
        )
        ControlsToggleRow(
            title = "Auto-Hyphenation",
            subtitle = "Split long words across lines cleanly",
            checked = preferences.autoHyphenation,
            onCheckedChange = { v -> onUpdatePrefs { it.copy(autoHyphenation = v) } }
        )

        HorizontalDivider()
        ControlsSectionTitle("Images")
        ControlsToggleRow(
            title = "Images",
            subtitle = "Show embedded illustrations inside chapters",
            checked = preferences.showImages,
            testTag = "sheet_show_images_switch",
            onCheckedChange = { v -> onUpdatePrefs { it.copy(showImages = v) } }
        )
        if (preferences.showImages) {
            ControlsToggleRow(
                title = "Image Captions",
                subtitle = "Display captions below illustrations when available",
                checked = preferences.showImageCaptions,
                onCheckedChange = { v -> onUpdatePrefs { it.copy(showImageCaptions = v) } }
            )
            Text("Color Effects", style = MaterialTheme.typography.bodyMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ImageColorEffect.entries.forEach { effect ->
                    FilterChip(
                        selected = preferences.imageColorEffect == effect,
                        onClick = { onUpdatePrefs { it.copy(imageColorEffect = effect) } },
                        label = { Text(effect.label) }
                    )
                }
            }
            ControlsLabeledSlider(
                label = "Corner Rounding",
                valueText = "${preferences.imageCornerRadiusDp.roundToInt()} pt",
                value = preferences.imageCornerRadiusDp,
                range = 0f..24f,
                onValueChange = { v -> onUpdatePrefs { it.copy(imageCornerRadiusDp = v) } }
            )
            Text("Image Alignment", style = MaterialTheme.typography.bodyMedium)
            val imgAlignOptions = listOf(
                ReaderTextAlignment.START,
                ReaderTextAlignment.CENTER,
                ReaderTextAlignment.END
            )
            ControlsSegmentedRow(
                options = imgAlignOptions,
                selected = if (preferences.imageAlignment in imgAlignOptions) preferences.imageAlignment else ReaderTextAlignment.CENTER,
                labelOf = { it.label },
                onSelect = { align -> onUpdatePrefs { it.copy(imageAlignment = align) } }
            )
            ControlsLabeledSlider(
                label = "Size",
                valueText = "${(preferences.imageWidthPercent * 100f).roundToInt()}%",
                value = preferences.imageWidthPercent,
                range = 0.35f..1.0f,
                onValueChange = { v -> onUpdatePrefs { it.copy(imageWidthPercent = v) } }
            )
        }

        HorizontalDivider()
        ControlsSectionTitle("Chapters & Progress")
        Text("Chapter Title Alignment", style = MaterialTheme.typography.bodyMedium)
        ControlsSegmentedRow(
            options = ReaderTextAlignment.entries,
            selected = preferences.chapterTitleAlignment,
            labelOf = { it.label },
            onSelect = { align -> onUpdatePrefs { it.copy(chapterTitleAlignment = align) } }
        )

        Text("Progress Display", style = MaterialTheme.typography.bodyMedium)
        ControlsSegmentedRow(
            options = ProgressDisplayMode.entries,
            selected = preferences.progressDisplayMode,
            labelOf = { it.label },
            onSelect = { mode -> onUpdatePrefs { it.copy(progressDisplayMode = mode) } }
        )
        ControlsToggleRow(
            title = "Progress Bar",
            subtitle = "Show progress bar along the bottom of the screen",
            checked = preferences.showProgressBars,
            onCheckedChange = { v -> onUpdatePrefs { it.copy(showProgressBars = v) } }
        )
        ControlsToggleRow(
            title = "Chapter Footer",
            subtitle = "Show chapter title, ETA, and location",
            checked = preferences.showReaderFooter,
            onCheckedChange = { v -> onUpdatePrefs { it.copy(showReaderFooter = v) } }
        )

        HorizontalDivider()
        ControlsSectionTitle("Double-Tap Action")
        Text(
            "Double-tap any paragraph while reading to trigger:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DoubleTapAction.entries.forEach { action ->
                FilterChip(
                    selected = preferences.doubleTapAction == action,
                    onClick = { onUpdatePrefs { it.copy(doubleTapAction = action) } },
                    label = { Text(action.label) }
                )
            }
        }

        if (preferences.enableTts) {
            HorizontalDivider()
            ControlsSectionTitle("Offline Text-to-Speech")
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
                        label = { Text(if (mins == 0) "Off" else "${mins}m") }
                    )
                }
            }
            ControlsLabeledSlider(
                label = "Speech Speed",
                valueText = String.format(Locale.US, "%.2fx", preferences.ttsRate),
                value = preferences.ttsRate,
                range = 0.6f..2.0f,
                onValueChange = { v -> onUpdatePrefs { it.copy(ttsRate = v) } }
            )
            ControlsLabeledSlider(
                label = "Speech Pitch",
                valueText = String.format(Locale.US, "%.2fx", preferences.ttsPitch),
                value = preferences.ttsPitch,
                range = 0.75f..1.35f,
                onValueChange = { v -> onUpdatePrefs { it.copy(ttsPitch = v) } }
            )
        }
    }
}
