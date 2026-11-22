package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.HorizontalGestureMode
import com.example.data.model.OrientationLock
import com.example.data.model.PageAnimation
import com.example.data.model.ReaderPreferences
import com.example.data.model.TapZonePreset
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReaderControlsGeneralTab(
    preferences: ReaderPreferences,
    onUpdatePrefs: ((ReaderPreferences) -> ReaderPreferences) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ControlsSectionTitle("Reading Mode & Gestures")
        Text("Horizontal Swipe Gesture", style = MaterialTheme.typography.bodyMedium)
        ControlsSegmentedRow(
            options = HorizontalGestureMode.entries,
            selected = preferences.horizontalGestureMode,
            labelOf = { it.label },
            onSelect = { mode -> onUpdatePrefs { it.copy(horizontalGestureMode = mode) } }
        )

        Text("Tap Zone Layout", style = MaterialTheme.typography.bodyMedium)
        ControlsSegmentedRow(
            options = TapZonePreset.entries,
            selected = preferences.tapZonePreset,
            labelOf = {
                when (it) {
                    TapZonePreset.CLASSIC_LR -> "Classic L/R"
                    TapZonePreset.ONE_HAND_RIGHT -> "One-Hand"
                    TapZonePreset.SCROLL_ONLY -> "Center Only"
                }
            },
            onSelect = { zone -> onUpdatePrefs { it.copy(tapZonePreset = zone) } }
        )

        Text("Chapter Transition", style = MaterialTheme.typography.bodyMedium)
        ControlsSegmentedRow(
            options = PageAnimation.entries,
            selected = preferences.pageAnimation,
            labelOf = { it.label },
            onSelect = { anim -> onUpdatePrefs { it.copy(pageAnimation = anim) } }
        )

        HorizontalDivider()
        ControlsSectionTitle("Margins & Bounds")
        ControlsLabeledSlider(
            label = "Side Margins",
            valueText = "${preferences.horizontalMarginDp.roundToInt()} dp",
            value = preferences.horizontalMarginDp,
            range = 0f..48f,
            onValueChange = { v -> onUpdatePrefs { it.copy(horizontalMarginDp = v) } }
        )
        ControlsLabeledSlider(
            label = "Vertical Margins",
            valueText = "${preferences.verticalPaddingDp.roundToInt()} dp",
            value = preferences.verticalPaddingDp,
            range = 0f..48f,
            onValueChange = { v -> onUpdatePrefs { it.copy(verticalPaddingDp = v) } }
        )
        ControlsToggleRow(
            title = "Cutout & Status Safe Margin",
            subtitle = "Keep text safely clear of display punch-holes and notches",
            checked = preferences.cutoutMargin,
            testTag = "sheet_cutout_margin_switch",
            onCheckedChange = { v -> onUpdatePrefs { it.copy(cutoutMargin = v) } }
        )
        ControlsLabeledSlider(
            label = "Bottom Bar Margin",
            valueText = "${preferences.bottomBarMarginDp.roundToInt()} dp",
            value = preferences.bottomBarMarginDp,
            range = 0f..32f,
            onValueChange = { v -> onUpdatePrefs { it.copy(bottomBarMarginDp = v) } }
        )
        ControlsLabeledSlider(
            label = "Max Column Width",
            valueText = "${preferences.maxColumnWidthDp.roundToInt()} dp",
            value = preferences.maxColumnWidthDp,
            range = 420f..900f,
            onValueChange = { v -> onUpdatePrefs { it.copy(maxColumnWidthDp = v) } }
        )

        HorizontalDivider()
        ControlsSectionTitle("Reading Speed")
        ControlsToggleRow(
            title = "Highlight Word Beginnings",
            subtitle = "Emphasize opening letters of words for faster fixation",
            checked = preferences.bionicReading,
            testTag = "sheet_bionic_reading_switch",
            onCheckedChange = { v -> onUpdatePrefs { it.copy(bionicReading = v) } }
        )
        ControlsToggleRow(
            title = "Perception Expander",
            subtitle = "Subtle side guide rails to keep eyes focused toward center",
            checked = preferences.perceptionExpander,
            testTag = "sheet_perception_expander_switch",
            onCheckedChange = { v -> onUpdatePrefs { it.copy(perceptionExpander = v) } }
        )

        HorizontalDivider()
        ControlsSectionTitle("System & Screen")
        ControlsToggleRow(
            title = "Custom Brightness",
            subtitle = "Use dedicated reading brightness level",
            checked = preferences.customBrightnessEnabled,
            testTag = "sheet_custom_brightness_switch",
            onCheckedChange = { v -> onUpdatePrefs { it.copy(customBrightnessEnabled = v) } }
        )
        if (preferences.customBrightnessEnabled) {
            ControlsLabeledSlider(
                label = "Screen Brightness",
                valueText = "${(preferences.screenBrightness * 100f).roundToInt()}%",
                value = preferences.screenBrightness.coerceIn(0.05f, 1f),
                range = 0.05f..1f,
                onValueChange = { v -> onUpdatePrefs { it.copy(screenBrightness = v) } }
            )
        }
        ControlsToggleRow(
            title = "Left-Edge Brightness Swipe",
            subtitle = "Drag along the left edge to adjust brightness",
            checked = preferences.swipeBrightness,
            onCheckedChange = { v -> onUpdatePrefs { it.copy(swipeBrightness = v) } }
        )
        ControlsToggleRow(
            title = "Volume Key Paging",
            subtitle = "Turn chapters or pages using physical volume buttons",
            checked = preferences.volumeKeyPaging,
            onCheckedChange = { v -> onUpdatePrefs { it.copy(volumeKeyPaging = v) } }
        )

        Text("Screen Orientation", style = MaterialTheme.typography.bodyMedium)
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

        HorizontalDivider()
        ControlsSectionTitle("Misc")
        ControlsToggleRow(
            title = "Fullscreen",
            subtitle = "Hide system status and navigation bars",
            checked = preferences.fullscreenMode,
            testTag = "sheet_fullscreen_switch",
            onCheckedChange = { v -> onUpdatePrefs { it.copy(fullscreenMode = v) } }
        )
        ControlsToggleRow(
            title = "Keep Screen Awake",
            subtitle = null,
            checked = preferences.keepScreenAwake,
            testTag = "sheet_keep_awake_switch",
            onCheckedChange = { v -> onUpdatePrefs { it.copy(keepScreenAwake = v) } }
        )
        ControlsToggleRow(
            title = "Hide Bars on Fast Scroll",
            subtitle = "Dismiss top and bottom toolbars automatically when scrolling",
            checked = preferences.hideBarsOnFastScroll,
            testTag = "sheet_hide_bars_scroll_switch",
            onCheckedChange = { v -> onUpdatePrefs { it.copy(hideBarsOnFastScroll = v) } }
        )
    }
}
