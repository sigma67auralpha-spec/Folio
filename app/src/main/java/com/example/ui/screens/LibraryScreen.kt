package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BookEntity
import com.example.data.model.BookWithRelations
import com.example.data.model.LibrarySortOrder
import com.example.data.model.LibraryViewMode
import com.example.ui.components.BookCover
import com.example.ui.components.formatBadgeColors
import com.example.ui.viewmodel.LibraryFilterChip
import com.example.ui.viewmodel.LibraryUiState
import kotlin.math.roundToInt

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onOpenBook: (Long) -> Unit,
    onImportBooks: () -> Unit,
    onLinkFolder: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectFilter: (LibraryFilterChip) -> Unit,
    onClearTagFilter: () -> Unit,
    onToggleViewMode: () -> Unit,
    onSelectSortOrder: (LibrarySortOrder) -> Unit,
    onEditMetadata: (BookWithRelations) -> Unit,
    onAssignShelves: (BookWithRelations) -> Unit,
    onRefreshBook: (BookEntity) -> Unit,
    onExportNotes: (BookEntity) -> Unit,
    onRemoveBook: (BookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var contextMenuBook by remember { mutableStateOf<BookWithRelations?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${state.allBooks.size} local book${if (state.allBooks.size == 1) "" else "s"} • Private",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleViewMode,
                        modifier = Modifier.testTag("toggle_view_mode_button")
                    ) {
                        Icon(
                            imageVector = if (state.preferences.libraryViewMode == LibraryViewMode.GRID)
                                Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                            contentDescription = "Toggle grid or list view"
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier.testTag("sort_menu_button")
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sort library")
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            LibrarySortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            order.label,
                                            fontWeight = if (order == state.preferences.librarySortOrder)
                                                FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onSelectSortOrder(order)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onLinkFolder,
                        modifier = Modifier.testTag("link_folder_button")
                    ) {
                        Icon(Icons.Outlined.CreateNewFolder, contentDescription = "Link local folder")
                    }
                }
            }

            if (state.isImporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search title, author, series, or #tag") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("library_search_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.activeTagFilter != null) {
                    FilterChip(
                        selected = true,
                        onClick = onClearTagFilter,
                        label = { Text("#${state.activeTagFilter} ×") }
                    )
                }
                LibraryFilterChip.entries.forEach { chip ->
                    FilterChip(
                        selected = state.activeFilter == chip,
                        onClick = { onSelectFilter(chip) },
                        label = { Text(chip.label) },
                        modifier = Modifier.testTag("filter_chip_${chip.name.lowercase()}")
                    )
                }
            }

            if (state.filteredBooks.isEmpty()) {
                EmptyLibraryPlaceholder(
                    hasAnyBooks = state.allBooks.isNotEmpty(),
                    onImportBooks = onImportBooks,
                    onLinkFolder = onLinkFolder
                )
            } else if (state.preferences.libraryViewMode == LibraryViewMode.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = state.preferences.libraryDensity.minCellDp.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 88.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val hero = state.continueReadingBook
                    if (state.preferences.showContinueReading && hero != null && state.searchQuery.isBlank() && state.activeFilter == LibraryFilterChip.ALL) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "continue_reading_hero") {
                            ContinueReadingCard(
                                item = hero,
                                showProgressBar = state.preferences.showProgressBars,
                                onResume = { onOpenBook(hero.book.id) }
                            )
                        }
                    }
                    items(state.filteredBooks, key = { it.book.id }) { item ->
                        BookGridCard(
                            item = item,
                            showFormatBadge = state.preferences.showFormatBadges,
                            showProgressBar = state.preferences.showProgressBars,
                            onOpen = { onOpenBook(item.book.id) },
                            onOpenMenu = { contextMenuBook = item }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 88.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val hero = state.continueReadingBook
                    if (state.preferences.showContinueReading && hero != null && state.searchQuery.isBlank() && state.activeFilter == LibraryFilterChip.ALL) {
                        item(key = "continue_reading_hero_list") {
                            ContinueReadingCard(
                                item = hero,
                                showProgressBar = state.preferences.showProgressBars,
                                onResume = { onOpenBook(hero.book.id) }
                            )
                        }
                    }
                    items(state.filteredBooks, key = { it.book.id }) { item ->
                        BookListRow(
                            item = item,
                            showFormatBadge = state.preferences.showFormatBadges,
                            showProgressBar = state.preferences.showProgressBars,
                            onOpen = { onOpenBook(item.book.id) },
                            onOpenMenu = { contextMenuBook = item }
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onImportBooks,
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text("Import") },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("import_books_fab")
        )
    }

    contextMenuBook?.let { item ->
        BookActionsBottomSheet(
            item = item,
            onDismiss = { contextMenuBook = null },
            onOpen = {
                contextMenuBook = null
                onOpenBook(item.book.id)
            },
            onEditMetadata = {
                contextMenuBook = null
                onEditMetadata(item)
            },
            onAssignShelves = {
                contextMenuBook = null
                onAssignShelves(item)
            },
            onRefreshBook = {
                contextMenuBook = null
                onRefreshBook(item.book)
            },
            onExportNotes = {
                contextMenuBook = null
                onExportNotes(item.book)
            },
            onRemove = {
                contextMenuBook = null
                onRemoveBook(item.book)
            }
        )
    }
}

@Composable
private fun ContinueReadingCard(
    item: BookWithRelations,
    showProgressBar: Boolean,
    onResume: () -> Unit
) {
    val book = item.book
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onResume)
            .testTag("continue_reading_card")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(
                book = book,
                showFormatBadge = false,
                modifier = Modifier.width(68.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.continue_reading).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showProgressBar) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showProgressBar) "${book.progressPercent.roundToInt()}% • ${book.format.displayName}"
                        else book.format.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onResume,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 38.dp).testTag("resume_reading_button")
                    ) {
                        Text(stringResource(R.string.resume_reading))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookGridCard(
    item: BookWithRelations,
    showFormatBadge: Boolean,
    showProgressBar: Boolean,
    onOpen: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val book = item.book
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onOpen, onLongClick = onOpenMenu)
            .testTag("book_grid_card_${book.id}")
    ) {
        Box {
            BookCover(book = book, showFormatBadge = showFormatBadge, modifier = Modifier.fillMaxWidth())
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
            ) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "Options for ${book.title}",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = book.author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (showProgressBar && book.progressPercent > 0.5f) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookListRow(
    item: BookWithRelations,
    showFormatBadge: Boolean,
    showProgressBar: Boolean,
    onOpen: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val book = item.book
    val (badgeBg, badgeFg) = formatBadgeColors(book.format)
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onOpenMenu)
            .testTag("book_list_row_${book.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(book = book, showFormatBadge = false, modifier = Modifier.width(54.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showFormatBadge) {
                        Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = book.format.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeFg,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = if (showProgressBar) "${book.progressPercent.roundToInt()}% • ${book.readStatus.label}"
                        else book.readStatus.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (book.series.isNotBlank()) {
                        Text(
                            text = "• ${book.series}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            IconButton(onClick = onOpenMenu) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Book actions")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookActionsBottomSheet(
    item: BookWithRelations,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onEditMetadata: () -> Unit,
    onAssignShelves: () -> Unit,
    onRefreshBook: () -> Unit,
    onExportNotes: () -> Unit,
    onRemove: () -> Unit
) {
    val book = item.book
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookCover(book = book, showFormatBadge = false, modifier = Modifier.width(56.dp))
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${book.format.displayName} • ${formatFileSize(book.fileSizeBytes)} • ${book.totalUnits} ${if (book.format.name in listOf("PDF", "CBZ", "CBR")) "pages" else "chapters"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            if (book.description.isNotBlank()) {
                Text(
                    text = book.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
            if (item.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                "#${tag.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(4.dp))
            SheetActionRow(Icons.AutoMirrored.Outlined.MenuBook, "Read Book", onOpen)
            SheetActionRow(Icons.Outlined.Edit, "Edit Metadata & Cover", onEditMetadata)
            SheetActionRow(Icons.Outlined.FolderCopy, "Organize into Shelves", onAssignShelves)
            SheetActionRow(Icons.Outlined.Refresh, "Re-parse Book Metadata & Cover", onRefreshBook)
            SheetActionRow(Icons.Outlined.FileDownload, "Export Notes & Highlights (.md)", onExportNotes)
            SheetActionRow(Icons.Outlined.DeleteOutline, "Remove from Library", onRemove, isDestructive = true)
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "Local"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format("%.1f MB", mb) else "${kb.roundToInt().coerceAtLeast(1)} KB"
}

@Composable
private fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@Composable
private fun EmptyLibraryPlaceholder(
    hasAnyBooks: Boolean,
    onImportBooks: () -> Unit,
    onLinkFolder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (hasAnyBooks) "No matching books" else stringResource(R.string.empty_library_title),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (hasAnyBooks) "Try adjusting your search or format filter." else stringResource(R.string.empty_library_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!hasAnyBooks) {
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onImportBooks,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("empty_import_button")
                ) {
                    Text(stringResource(R.string.import_books))
                }
                OutlinedButton(
                    onClick = onLinkFolder,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("empty_link_folder_button")
                ) {
                    Text(stringResource(R.string.scan_folder))
                }
            }
        }
    }
}
