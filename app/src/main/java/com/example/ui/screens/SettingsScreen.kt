package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import com.example.data.model.AppAccent
import com.example.data.model.LibraryDensity
import com.example.data.model.OrientationLock
import com.example.data.model.PageAnimation
import com.example.data.model.ReaderPreferences
import com.example.data.model.TapZonePreset
import com.example.data.model.WatchedFolderEntity
import com.example.ui.viewmodel.LibraryUiState

@Composable
fun SettingsScreen(
    state: LibraryUiState,
    onUpdatePrefs: ((ReaderPreferences) -> ReaderPreferences) -> Unit,
    onImportCustomFont: () -> Unit,
    onLinkFolder: () -> Unit,
    onRescanFolder: (WatchedFolderEntity) -> Unit,
    onRemoveFolder: (WatchedFolderEntity) -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = state.preferences
    val isDark = prefs.appDarkTheme ?: isSystemInDarkTheme()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Personal Settings", style = MaterialTheme.typography.headlineMedium)
        }

        // Privacy guarantee card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Private & Offline by Design", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Folio requests zero internet permission, no accounts, and stores your library, notes, and stats strictly on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // App Appearance & Layout
        item {
            SettingsCard("Appearance & Layout") {
                Text("App Theme", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "System", false to "Light", true to "Dark").forEach { (value, label) ->
                        FilterChip(
                            selected = prefs.appDarkTheme == value,
                            onClick = { onUpdatePrefs { it.copy(appDarkTheme = value) } },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Accent Color", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppAccent.entries.forEach { accent ->
                        val selected = prefs.appAccent == accent
                        val swatchColor = Color(if (isDark) accent.darkHex else accent.lightHex)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdatePrefs { it.copy(appAccent = accent) } }
                                .testTag("accent_${accent.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = "${accent.label} selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(accent.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Library Grid Density", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LibraryDensity.entries.forEach { density ->
                        FilterChip(
                            selected = prefs.libraryDensity == density,
                            onClick = { onUpdatePrefs { it.copy(libraryDensity = density) } },
                            label = { Text(density.label) },
                            modifier = Modifier.testTag("density_${density.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Modular Features (Turn ON / OFF)
        item {
            SettingsCard("Modular Features (Turn On / Off)") {
                Text(
                    "Keep only the tools you enjoy. Turn off anything you find distracting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FeatureToggleRow(
                    title = "Continue Reading Card",
                    subtitle = "Show the featured resume banner at the top of Library",
                    checked = prefs.showContinueReading,
                    testTag = "toggle_continue_reading_switch",
                    onCheckedChange = { v -> onUpdatePrefs { it.copy(showContinueReading = v) } }
                )
                FeatureToggleRow(
                    title = "Reading Progress Bars",
                    subtitle = "Display progress bars on covers and the reader bottom edge",
                    checked = prefs.showProgressBars,
                    testTag = "toggle_progress_bars_switch",
                    onCheckedChange = { v -> onUpdatePrefs { it.copy(showProgressBars = v) } }
                )
                FeatureToggleRow(
                    title = "Format Badges on Covers",
                    subtitle = "Show EPUB / PDF / CBZ pills on library book covers",
                    checked = prefs.showFormatBadges,
                    testTag = "toggle_format_badges_switch",
                    onCheckedChange = { v -> onUpdatePrefs { it.copy(showFormatBadges = v) } }
                )
                FeatureToggleRow(
                    title = "Reading Journal Tab",
                    subtitle = "Track daily streaks, reading minutes, and books finished",
                    checked = prefs.showStats,
                    testTag = "toggle_show_stats_switch",
                    onCheckedChange = { v -> onUpdatePrefs { it.copy(showStats = v) } }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                FeatureToggleRow(
                    title = "Reader Chapter Footer",
                    subtitle = "Show subtle chapter title and page position while reading",
                    checked = prefs.showReaderFooter,
                    testTag = "toggle_reader_footer_switch",
                    onCheckedChange = { v -> onUpdatePrefs { it.copy(showReaderFooter = v) } }
                )
                FeatureToggleRow(
                    title = "Chapter Reading Time (ETA)",
                    subtitle = "Estimate minutes left in current chapter in the footer",
                    checked = prefs.showEtaInReader,
                    testTag = "toggle_reader_eta_switch",
                    onCheckedChange = { v -> onUpdatePrefs { it.copy(showEtaInReader = v) } }
                )
                FeatureToggleRow(
                    title = "Clock in Reader Footer",
                    subtitle = "Display current time subtly so you never lose track",
                    checked = prefs.showClockInReader,
                    testTag = "toggle_reader_clock_switch",
                    onCheckedChange = { v -> onUpdatePrefs { it.copy(showClockInReader = v) } }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                FeatureToggleRow(
                    title = "Highlights & Margin Notes",
                    subtitle = "Long-press any paragraph to highlight or attach a note",
                    checked = prefs.enableHighlights,
                    testTag = "toggle_highlights_switch",
                    onCheckedChange = { v -> onUpdatePrefs { it.copy(enableHighlights = v) } }
                )
                FeatureToggleRow(
                    title = "Offline Text-to-Speech",
                    subtitle = "Show Read Aloud controls in the reader toolbar and sheet",
                    checked = prefs.enableTts,
                    testTag = "toggle_tts_switch",
                    onCheckedChange = { v -> onUpdatePrefs { it.copy(enableTts = v) } }
                )
                FeatureToggleRow(
                    title = "Keep Screen Awake",
                    subtitle = "Prevent screen sleep automatically while a book is open",
                    checked = prefs.keepScreenAwake,
                    testTag = "toggle_keep_awake_switch",
                    onCheckedChange = { v -> onUpdatePrefs { it.copy(keepScreenAwake = v) } }
                )
            }
        }

        // Typography & Custom Fonts
        item {
            SettingsCard("Fonts, Gestures & Orientation") {
                OutlinedButton(
                    onClick = onImportCustomFont,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("import_font_button")
                ) {
                    Icon(Icons.Outlined.FontDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (prefs.customFontName != null) "Custom Font: ${prefs.customFontName} (Replace)"
                        else "Import Custom .ttf / .otf Font"
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text("Tap Zones", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                TapZonePreset.entries.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onUpdatePrefs { it.copy(tapZonePreset = preset) } }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = prefs.tapZonePreset == preset,
                            onClick = { onUpdatePrefs { it.copy(tapZonePreset = preset) } }
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(preset.label, style = MaterialTheme.typography.bodyMedium)
                            Text(preset.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Page Transition", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PageAnimation.entries.forEach { anim ->
                        FilterChip(
                            selected = prefs.pageAnimation == anim,
                            onClick = { onUpdatePrefs { it.copy(pageAnimation = anim) } },
                            label = { Text(anim.label) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Reader Orientation Lock", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrientationLock.entries.forEach { lock ->
                        FilterChip(
                            selected = prefs.orientationLock == lock,
                            onClick = { onUpdatePrefs { it.copy(orientationLock = lock) } },
                            label = { Text(lock.label) },
                            modifier = Modifier.testTag("settings_orientation_${lock.name.lowercase()}")
                        )
                    }
                }
                FeatureToggleRow(
                    title = "Volume-Key Chapter / Page Turn",
                    subtitle = null,
                    checked = prefs.volumeKeyPaging,
                    testTag = "toggle_volume_paging_switch",
                    onCheckedChange = { v -> onUpdatePrefs { it.copy(volumeKeyPaging = v) } }
                )
                FeatureToggleRow(
                    title = "Left-Edge Brightness Swipe",
                    subtitle = null,
                    checked = prefs.swipeBrightness,
                    testTag = "toggle_swipe_brightness_switch",
                    onCheckedChange = { v -> onUpdatePrefs { it.copy(swipeBrightness = v) } }
                )
            }
        }

        // Watched Folders
        item {
            SettingsCard("Linked Local Folders") {
                OutlinedButton(
                    onClick = onLinkFolder,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Link Folder (Local / Syncthing / Drive)")
                }
                if (state.watchedFolders.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    state.watchedFolders.forEach { folder ->
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(folder.displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onRescanFolder(folder) }) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Rescan ${folder.displayName}")
                            }
                            IconButton(onClick = { onRemoveFolder(folder) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Unlink ${folder.displayName}")
                            }
                        }
                    }
                }
            }
        }

        // Portable Backup & Restore
        item {
            SettingsCard("Portable Backup & Restore") {
                Text(
                    "Export your library index, shelves, reading progress, covers, and highlights as a single portable .zip file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onExportBackup,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("export_backup_button")
                    ) {
                        Icon(Icons.Outlined.Backup, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Export ZIP")
                    }
                    OutlinedButton(
                        onClick = onRestoreBackup,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("restore_backup_button")
                    ) {
                        Icon(Icons.Outlined.Restore, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Restore ZIP")
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    onUpdatePrefs { current ->
                        ReaderPreferences(
                            customFontPath = current.customFontPath,
                            customFontName = current.customFontName
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("reset_preferences_button")
            ) {
                Icon(Icons.Outlined.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Reset Customization & Toggles to Defaults")
            }
        }
    }
}

@Composable
private fun FeatureToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
