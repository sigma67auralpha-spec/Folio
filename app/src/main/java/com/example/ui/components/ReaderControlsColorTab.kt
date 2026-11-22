package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ReaderColorPreset
import com.example.data.model.ReaderPreferences
import com.example.data.model.ReaderThemeMode
import com.example.data.model.defaultReaderColorPresets
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
internal fun ReaderControlsColorTab(
    preferences: ReaderPreferences,
    onUpdatePrefs: ((ReaderPreferences) -> ReaderPreferences) -> Unit
) {
    val presets = preferences.colorPresets.ifEmpty { defaultReaderColorPresets() }
    val activePreset = presets.firstOrNull { it.id == preferences.activeColorPresetId } ?: presets.first()

    val currentBgHex = if (preferences.themeMode == ReaderThemeMode.CUSTOM) preferences.customBgHex else activePreset.bgHex
    val currentInkHex = if (preferences.themeMode == ReaderThemeMode.CUSTOM) preferences.customInkHex else activePreset.inkHex

    fun updateActiveColors(newBgHex: Long = currentBgHex, newInkHex: Long = currentInkHex) {
        onUpdatePrefs { current ->
            val list = current.colorPresets.ifEmpty { defaultReaderColorPresets() }
            val updatedList = list.map { p ->
                if (p.id == activePreset.id) p.copy(bgHex = newBgHex, inkHex = newInkHex) else p
            }
            current.copy(
                themeMode = ReaderThemeMode.CUSTOM,
                activeColorPresetId = activePreset.id,
                customBgHex = newBgHex,
                customInkHex = newInkHex,
                colorPresets = updatedList
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ControlsSectionTitle("Color Presets")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = preferences.activeColorPresetId == preset.id
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onUpdatePrefs {
                            it.copy(
                                themeMode = ReaderThemeMode.CUSTOM,
                                activeColorPresetId = preset.id,
                                customBgHex = preset.bgHex,
                                customInkHex = preset.inkHex
                            )
                        }
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(preset.bgHex))
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color(preset.inkHex),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    },
                    label = { Text(preset.name) },
                    modifier = Modifier.testTag("color_preset_${preset.id}")
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = activePreset.name,
                        onValueChange = { newName ->
                            onUpdatePrefs { current ->
                                current.copy(
                                    colorPresets = current.colorPresets.map { p ->
                                        if (p.id == activePreset.id) p.copy(name = newName.take(28)) else p
                                    }
                                )
                            }
                        },
                        singleLine = true,
                        label = { Text("Preset Name") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val isDarkShuffle = Random.nextBoolean()
                            val bg = if (isDarkShuffle) {
                                packRgb(Random.nextInt(12, 42), Random.nextInt(12, 42), Random.nextInt(16, 48))
                            } else {
                                packRgb(Random.nextInt(232, 255), Random.nextInt(226, 252), Random.nextInt(212, 248))
                            }
                            val ink = if (isDarkShuffle) {
                                packRgb(Random.nextInt(205, 242), Random.nextInt(202, 240), Random.nextInt(198, 242))
                            } else {
                                packRgb(Random.nextInt(22, 65), Random.nextInt(22, 62), Random.nextInt(22, 68))
                            }
                            updateActiveColors(newBgHex = bg, newInkHex = ink)
                        }
                    ) {
                        Icon(Icons.Outlined.Shuffle, contentDescription = "Shuffle theme colors")
                    }
                    IconButton(
                        onClick = {
                            onUpdatePrefs { current ->
                                val nextIdx = current.colorPresets.size + 1
                                val newId = "custom_${System.currentTimeMillis()}"
                                val created = ReaderColorPreset(
                                    id = newId,
                                    name = "Preset $nextIdx",
                                    bgHex = currentBgHex,
                                    inkHex = currentInkHex
                                )
                                current.copy(
                                    themeMode = ReaderThemeMode.CUSTOM,
                                    activeColorPresetId = newId,
                                    colorPresets = current.colorPresets + created
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Save as new color preset")
                    }
                    if (presets.size > 1) {
                        IconButton(
                            onClick = {
                                onUpdatePrefs { current ->
                                    val remaining = current.colorPresets.filterNot { it.id == activePreset.id }
                                        .ifEmpty { defaultReaderColorPresets() }
                                    val fallback = remaining.first()
                                    current.copy(
                                        themeMode = ReaderThemeMode.CUSTOM,
                                        activeColorPresetId = fallback.id,
                                        customBgHex = fallback.bgHex,
                                        customInkHex = fallback.inkHex,
                                        colorPresets = remaining
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete color preset")
                        }
                    }
                }

                // Background RGB section
                val bgR = ((currentBgHex shr 16) and 0xFF).toInt()
                val bgG = ((currentBgHex shr 8) and 0xFF).toInt()
                val bgB = (currentBgHex and 0xFF).toInt()
                ColorHeaderRow(title = "Background Color", previewHex = currentBgHex)
                RgbChannelSlider("Red", bgR, defaultVal = 249) { r ->
                    updateActiveColors(newBgHex = packRgb(r, bgG, bgB))
                }
                RgbChannelSlider("Green", bgG, defaultVal = 244) { g ->
                    updateActiveColors(newBgHex = packRgb(bgR, g, bgB))
                }
                RgbChannelSlider("Blue", bgB, defaultVal = 232) { b ->
                    updateActiveColors(newBgHex = packRgb(bgR, bgG, b))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                // Text RGB section
                val inkR = ((currentInkHex shr 16) and 0xFF).toInt()
                val inkG = ((currentInkHex shr 8) and 0xFF).toInt()
                val inkB = (currentInkHex and 0xFF).toInt()
                ColorHeaderRow(title = "Text Color", previewHex = currentInkHex)
                RgbChannelSlider("Red", inkR, defaultVal = 35) { r ->
                    updateActiveColors(newInkHex = packRgb(r, inkG, inkB))
                }
                RgbChannelSlider("Green", inkG, defaultVal = 31) { g ->
                    updateActiveColors(newInkHex = packRgb(inkR, g, inkB))
                }
                RgbChannelSlider("Blue", inkB, defaultVal = 27) { b ->
                    updateActiveColors(newInkHex = packRgb(inkR, inkG, b))
                }
            }
        }

        ControlsLabeledSlider(
            label = "Warmth Filter",
            valueText = "${(preferences.warmthTint / 0.45f * 100f).roundToInt()}%",
            value = preferences.warmthTint,
            range = 0f..0.45f,
            onValueChange = { v -> onUpdatePrefs { it.copy(warmthTint = v) } }
        )

        ControlsToggleRow(
            title = "Quick Preset Swipe",
            subtitle = "Swipe left or right on the top reader bar to switch presets",
            checked = preferences.quickColorPresetSwipe,
            testTag = "sheet_quick_color_swipe_switch",
            onCheckedChange = { v -> onUpdatePrefs { it.copy(quickColorPresetSwipe = v) } }
        )
    }
}

@Composable
private fun ColorHeaderRow(title: String, previewHex: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(previewHex))
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                String.format("#%06X", previewHex and 0xFFFFFFL),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RgbChannelSlider(
    channelName: String,
    value: Int,
    defaultVal: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(50.dp)) {
            Text(channelName, style = MaterialTheme.typography.bodySmall)
            Text("$value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(0, 255)) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { onChange(defaultVal) }) {
            Icon(
                Icons.Outlined.Replay,
                contentDescription = "Reset $channelName channel",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun packRgb(r: Int, g: Int, b: Int): Long =
    0xFF000000L or ((r.coerceIn(0, 255).toLong()) shl 16) or ((g.coerceIn(0, 255).toLong()) shl 8) or b.coerceIn(0, 255).toLong()
