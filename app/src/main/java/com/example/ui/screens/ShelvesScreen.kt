package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BookWithRelations
import com.example.data.model.ReadStatus
import com.example.data.model.ShelfEntity
import com.example.ui.components.BookCover
import com.example.ui.viewmodel.LibraryUiState
import kotlin.math.roundToInt

private sealed interface ActiveCollection {
    data class Smart(val title: String, val books: List<BookWithRelations>) : ActiveCollection
    data class Custom(val shelf: ShelfEntity) : ActiveCollection
    data class Series(val seriesName: String) : ActiveCollection
}

@Composable
fun ShelvesScreen(
    state: LibraryUiState,
    onOpenBook: (Long) -> Unit,
    onCreateShelf: (String) -> Unit,
    onRenameShelf: (ShelfEntity, String) -> Unit,
    onDeleteShelf: (ShelfEntity) -> Unit,
    onSelectTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeCollection by remember { mutableStateOf<ActiveCollection?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingShelf by remember { mutableStateOf<ShelfEntity?>(null) }

    if (activeCollection != null) {
        BackHandler { activeCollection = null }
        val (title, books) = when (val col = activeCollection!!) {
            is ActiveCollection.Smart -> col.title to col.books
            is ActiveCollection.Custom -> col.shelf.name to state.allBooks.filter { b ->
                b.shelves.any { it.id == col.shelf.id }
            }
            is ActiveCollection.Series -> col.seriesName to state.allBooks
                .filter { it.book.series.equals(col.seriesName, ignoreCase = true) }
                .sortedBy { it.book.seriesIndex }
        }

        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activeCollection = null }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to shelves")
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${books.size} book${if (books.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (books.isEmpty()) {
                BoxEmptyHint("No books in this collection yet.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(books, key = { it.book.id }) { item ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenBook(item.book.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BookCover(book = item.book, showFormatBadge = false, modifier = Modifier.width(50.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.book.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${item.book.progressPercent.roundToInt()}% • ${item.book.format.displayName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    val readingBooks = state.allBooks.filter { it.book.readStatus == ReadStatus.READING }
    val annotatedBooks = state.allBooks.filter { it.highlights.isNotEmpty() }
    val recentBooks = state.allBooks.sortedByDescending { it.book.addedAt }.take(12)
    val seriesNames = state.allBooks.map { it.book.series.trim() }.filter { it.isNotEmpty() }.distinct().sorted()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Shelves & Collections", style = MaterialTheme.typography.headlineMedium)
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.heightIn(min = 44.dp).testTag("create_shelf_button")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("New Shelf")
                }
            }
        }

        item {
            Text("Smart Shelves", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            ShelfSummaryCard(
                icon = Icons.Outlined.AutoStories,
                title = "Currently Reading",
                count = readingBooks.size,
                onClick = { activeCollection = ActiveCollection.Smart("Currently Reading", readingBooks) }
            )
        }
        item {
            ShelfSummaryCard(
                icon = Icons.Outlined.FormatQuote,
                title = "Annotated Books",
                count = annotatedBooks.size,
                onClick = { activeCollection = ActiveCollection.Smart("Annotated Books", annotatedBooks) }
            )
        }
        item {
            ShelfSummaryCard(
                icon = Icons.Outlined.History,
                title = "Recently Added",
                count = recentBooks.size,
                onClick = { activeCollection = ActiveCollection.Smart("Recently Added", recentBooks) }
            )
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Personal Shelves", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (state.shelves.isEmpty()) {
            item {
                Text(
                    "Create custom shelves like Fiction, Reference, or Manga to group books without moving files.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.shelves, key = { it.id }) { shelf ->
                val count = state.allBooks.count { b -> b.shelves.any { it.id == shelf.id } }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeCollection = ActiveCollection.Custom(shelf) }
                        .testTag("shelf_card_${shelf.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CollectionsBookmark, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(shelf.name, style = MaterialTheme.typography.titleMedium)
                            Text("$count book${if (count == 1) "" else "s"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { editingShelf = shelf }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Rename shelf")
                        }
                        IconButton(onClick = { onDeleteShelf(shelf) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete shelf")
                        }
                    }
                }
            }
        }

        if (seriesNames.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Series", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(seriesNames) { sName ->
                val count = state.allBooks.count { it.book.series.equals(sName, ignoreCase = true) }
                ShelfSummaryCard(
                    icon = Icons.Outlined.CollectionsBookmark,
                    title = sName,
                    count = count,
                    onClick = { activeCollection = ActiveCollection.Series(sName) }
                )
            }
        }

        if (state.tags.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Tags", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.clickable { onSelectTag(tag.name) }
                        ) {
                            Text(
                                "#${tag.name}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        ShelfNameDialog(
            title = "Create Shelf",
            initialName = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreateShelf(name)
                showCreateDialog = false
            }
        )
    }

    editingShelf?.let { shelf ->
        ShelfNameDialog(
            title = "Rename Shelf",
            initialName = shelf.name,
            onDismiss = { editingShelf = null },
            onConfirm = { name ->
                onRenameShelf(shelf, name)
                editingShelf = null
            }
        )
    }
}

@Composable
private fun ShelfSummaryCard(
    icon: ImageVector,
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BoxEmptyHint(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ShelfNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Shelf Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("shelf_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                modifier = Modifier.testTag("confirm_shelf_name_button")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
