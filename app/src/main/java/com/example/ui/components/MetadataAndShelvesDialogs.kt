package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.BookWithRelations
import com.example.data.model.ShelfEntity

@Composable
fun MetadataEditorDialog(
    item: BookWithRelations,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        author: String,
        series: String,
        seriesIndex: Float,
        description: String,
        coverUri: Uri?,
        tags: List<String>
    ) -> Unit
) {
    val book = item.book
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author) }
    var series by remember { mutableStateOf(book.series) }
    var seriesIdxText by remember {
        mutableStateOf(if (book.seriesIndex > 0f) book.seriesIndex.toString().removeSuffix(".0") else "")
    }
    var description by remember { mutableStateOf(book.description) }
    var tagsText by remember { mutableStateOf(item.tags.joinToString(", ") { it.name }) }
    var pickedCoverUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) pickedCoverUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Metadata & Cover", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("pick_cover_button")
                ) {
                    Icon(Icons.Outlined.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (pickedCoverUri != null) "Custom Cover Selected" else "Choose Custom Cover Image")
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_title_input")
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_author_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = series,
                        onValueChange = { series = it },
                        label = { Text("Series") },
                        singleLine = true,
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = seriesIdxText,
                        onValueChange = { seriesIdxText = it },
                        label = { Text("#") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Tags (comma separated)") },
                    placeholder = { Text("sci-fi, classics, study") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Synopsis / Notes") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tags = tagsText.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    onSave(
                        title,
                        author,
                        series,
                        seriesIdxText.toFloatOrNull() ?: 0f,
                        description,
                        pickedCoverUri,
                        tags
                    )
                },
                modifier = Modifier.testTag("save_metadata_button")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AssignShelvesDialog(
    item: BookWithRelations,
    allShelves: List<ShelfEntity>,
    onDismiss: () -> Unit,
    onCreateShelf: (String) -> Unit,
    onSave: (Set<Long>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(item.shelves.map { it.id }.toSet()) }
    var newShelfName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Organize Shelves", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    item.book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newShelfName,
                        onValueChange = { newShelfName = it },
                        label = { Text("New Shelf") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("new_shelf_inline_input")
                    )
                    OutlinedButton(
                        onClick = {
                            if (newShelfName.isNotBlank()) {
                                onCreateShelf(newShelfName.trim())
                                newShelfName = ""
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Add") }
                }
                Spacer(Modifier.height(4.dp))
                if (allShelves.isEmpty()) {
                    Text(
                        "No shelves yet. Create one above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        allShelves.forEach { shelf ->
                            val checked = shelf.id in selectedIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable {
                                        selectedIds = if (checked) selectedIds - shelf.id else selectedIds + shelf.id
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        selectedIds = if (checked) selectedIds - shelf.id else selectedIds + shelf.id
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(shelf.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedIds) },
                modifier = Modifier.testTag("save_shelves_button")
            ) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RemoveBookDialog(
    bookTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (deleteLocalCopy: Boolean) -> Unit
) {
    var deleteLocalCopy by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove Book") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Remove \"$bookTitle\" from Folio?", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { deleteLocalCopy = false },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = !deleteLocalCopy, onClick = { deleteLocalCopy = false })
                    Spacer(Modifier.width(6.dp))
                    Text("Remove from library index only", style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { deleteLocalCopy = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = deleteLocalCopy, onClick = { deleteLocalCopy = true })
                    Spacer(Modifier.width(6.dp))
                    Text("Delete Folio's cached copy too", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(deleteLocalCopy) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_remove_book_button")
            ) { Text("Remove") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
