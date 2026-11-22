package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.FormatCategory
import com.example.data.model.OrientationLock
import com.example.data.model.ReaderPreferences
import com.example.engine.TtsState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by rememberSaveable { mutableIntStateOf(if (category == FormatCategory.REFLOWABLE) 1 else 0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.22f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            if (category == FormatCategory.REFLOWABLE) {
                val tabs = listOf("General", "Reader", "Colors")
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, maxLines = 1) },
                            modifier = Modifier.testTag("controls_tab_${title.lowercase()}")
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> ReaderControlsGeneralTab(
                            preferences = preferences,
                            onUpdatePrefs = onUpdatePrefs
                        )
                        1 -> ReaderControlsTypographyTab(
                            preferences = preferences,
                            ttsState = ttsState,
                            onUpdatePrefs = onUpdatePrefs,
                            onToggleTts = onToggleTts,
                            onSetSleepTimer = onSetSleepTimer
                        )
                        else -> ReaderControlsColorTab(
                            preferences = preferences,
                            onUpdatePrefs = onUpdatePrefs
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        if (category == FormatCategory.PDF) "PDF Viewing Controls" else "Comic & Manga Controls",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (category == FormatCategory.PDF) {
                        ControlsToggleRow(
                            title = "Auto-Crop White Margins",
                            subtitle = "Zooms cleanly into printed text columns",
                            checked = preferences.pdfMarginCrop,
                            testTag = "pdf_margin_crop_switch",
                            onCheckedChange = { v -> onUpdatePrefs { it.copy(pdfMarginCrop = v) } }
                        )
                        ControlsToggleRow(
                            title = "Night Mode Invert",
                            subtitle = "Dark page background with soft light text",
                            checked = preferences.pdfDarkInvert,
                            testTag = "pdf_dark_invert_switch",
                            onCheckedChange = { v -> onUpdatePrefs { it.copy(pdfDarkInvert = v) } }
                        )
                    } else {
                        ControlsToggleRow(
                            title = "Right-to-Left (Manga)",
                            subtitle = "Reverses page-turn direction",
                            checked = preferences.comicRtl,
                            testTag = "comic_rtl_switch",
                            onCheckedChange = { v -> onUpdatePrefs { it.copy(comicRtl = v) } }
                        )
                        ControlsToggleRow(
                            title = "Double-Page Spread",
                            subtitle = "View two comic pages side-by-side",
                            checked = preferences.comicDoubleSpread,
                            testTag = "comic_spread_switch",
                            onCheckedChange = { v -> onUpdatePrefs { it.copy(comicDoubleSpread = v) } }
                        )
                    }
                    HorizontalDivider()
                    ControlsSectionTitle("Eye Comfort & Screen")
                    ControlsLabeledSlider(
                        label = "Warmth Tint",
                        valueText = "${(preferences.warmthTint / 0.45f * 100f).roundToInt()}%",
                        value = preferences.warmthTint,
                        range = 0f..0.45f,
                        onValueChange = { v -> onUpdatePrefs { it.copy(warmthTint = v) } }
                    )
                    ControlsToggleRow(
                        title = "Fullscreen",
                        subtitle = "Hide system status and navigation bars",
                        checked = preferences.fullscreenMode,
                        testTag = "sheet_fullscreen_switch",
                        onCheckedChange = { v -> onUpdatePrefs { it.copy(fullscreenMode = v) } }
                    )
                    ControlsToggleRow(
                        title = "Custom Brightness",
                        subtitle = "Override device screen brightness while reading",
                        checked = preferences.customBrightnessEnabled,
                        testTag = "sheet_custom_brightness_switch",
                        onCheckedChange = { v -> onUpdatePrefs { it.copy(customBrightnessEnabled = v) } }
                    )
                    if (preferences.customBrightnessEnabled) {
                        ControlsLabeledSlider(
                            label = "Brightness Level",
                            valueText = "${(preferences.screenBrightness * 100f).roundToInt()}%",
                            value = preferences.screenBrightness.coerceIn(0.05f, 1f),
                            range = 0.05f..1f,
                            onValueChange = { v -> onUpdatePrefs { it.copy(screenBrightness = v) } }
                        )
                    }
                    Text("Orientation", style = MaterialTheme.typography.bodyMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OrientationLock.entries.forEach { lock ->
                            FilterChip(
                                selected = preferences.orientationLock == lock,
                                onClick = { onUpdatePrefs { it.copy(orientationLock = lock) } },
                                label = { Text(lock.label) },
                                modifier = Modifier.testTag("sheet_orientation_${lock.name.lowercase()}")
                            )
                        }
                    }
                    ControlsToggleRow(
                        title = "Keep Screen Awake",
                        subtitle = null,
                        checked = preferences.keepScreenAwake,
                        testTag = "sheet_keep_awake_switch",
                        onCheckedChange = { v -> onUpdatePrefs { it.copy(keepScreenAwake = v) } }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
internal fun ControlsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
internal fun ControlsLabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Surface(
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 2.dp
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps
        )
    }
}

@Composable
internal fun ControlsToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    testTag: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> ControlsSegmentedRow(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selected
            SegmentedButton(
                selected = isSelected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null
                        )
                    }
                }
            ) {
                Text(labelOf(option), maxLines = 1)
            }
        }
    }
}
