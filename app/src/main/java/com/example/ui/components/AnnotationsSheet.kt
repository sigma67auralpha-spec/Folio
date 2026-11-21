package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BookmarkEntity
import com.example.data.model.HighlightColor
import com.example.data.model.HighlightEntity
import com.example.engine.BookChapter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderIndexAndNotesSheet(
    chapters: List<BookChapter>,
    totalPages: Int,
    currentUnit: Int,
    bookmarks: List<BookmarkEntity>,
    highlights: List<HighlightEntity>,
    onDismiss: () -> Unit,
    onJumpToUnit: (Int) -> Unit,
    onDeleteBookmark: (BookmarkEntity) -> Unit,
    onDeleteHighlight: (HighlightEntity) -> Unit,
    onExportNotesMarkdown: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Contents") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Notes (${highlights.size})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Bookmarks (${bookmarks.size})") })
            }

            when (selectedTab) {
                0 -> {
                    if (chapters.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            items(chapters, key = { it.index }) { ch ->
                                val isCurrent = ch.index == currentUnit
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            onJumpToUnit(ch.index)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = ch.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${ch.index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            items(totalPages) { page ->
                                val isCurrent = page == currentUnit
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clickable {
                                            onJumpToUnit(page)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Page ${page + 1}",
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        OutlinedButton(
                            onClick = onExportNotesMarkdown,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("export_notes_md_button")
                        ) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export Notes to Markdown (.md)")
                        }
                        Spacer(Modifier.height(10.dp))
                        if (highlights.isEmpty()) {
                            Text(
                                "Long-press any paragraph while reading to highlight passages and add personal notes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(highlights, key = { it.id }) { hl ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        tonalElevation = 1.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onJumpToUnit(hl.chapterIndex)
                                                onDismiss()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .padding(top = 4.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(hl.color.hex).copy(alpha = 0.95f))
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "“${hl.quoteText}”",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (hl.noteText.isNotBlank()) {
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        text = hl.noteText,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                            IconButton(onClick = { onDeleteHighlight(hl) }) {
                                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete highlight")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (bookmarks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                            Text(
                                "No bookmarks yet. Tap the bookmark icon in the reader bar to mark a spot.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            items(bookmarks, key = { it.id }) { bm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clickable {
                                            onJumpToUnit(bm.unitIndex)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bm.label, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            "Section ${bm.unitIndex + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeleteBookmark(bm) }) {
                                        Icon(Icons.Outlined.BookmarkRemove, contentDescription = "Delete bookmark")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightNoteDialog(
    quotePreview: String,
    initialNote: String = "",
    initialColor: HighlightColor = HighlightColor.AMBER,
    onDismiss: () -> Unit,
    onSave: (note: String, color: HighlightColor) -> Unit
) {
    var note by remember { mutableStateOf(initialNote) }
    var color by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Highlight & Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "“${quotePreview.take(180)}${if (quotePreview.length > 180) "…" else ""}”",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HighlightColor.entries.forEach { option ->
                        val selected = option == color
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(option.hex).copy(alpha = 0.95f))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { color = option }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Personal margin note (optional)") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("highlight_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(note, color) },
                modifier = Modifier.testTag("save_highlight_button")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
