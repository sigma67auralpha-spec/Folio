package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.BookWithRelations
import com.example.ui.components.AssignShelvesDialog
import com.example.ui.components.MetadataEditorDialog
import com.example.ui.components.RemoveBookDialog
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShelvesScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.FolioTheme
import com.example.ui.viewmodel.LibraryViewModel
import com.example.widget.FolioWidgetUpdater

data class WidgetNavRequest(
    val seq: Long,
    val tabRoute: String? = null,
    val bookId: Long? = null,
    val externalUri: Uri? = null
)

class MainActivity : ComponentActivity() {
    private var widgetNavState by mutableStateOf<WidgetNavRequest?>(null)
    private var navSeq = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIncomingIntent(intent)
        setContent {
            FolioAppRoot(widgetNavRequest = widgetNavState)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIncomingIntent(intent)
    }

    private fun consumeIncomingIntent(intent: Intent?) {
        intent ?: return
        val tab = intent.getStringExtra(FolioWidgetUpdater.EXTRA_OPEN_TAB)
        val bookId = intent.getLongExtra(FolioWidgetUpdater.EXTRA_OPEN_BOOK_ID, -1L)
        if (tab == FolioWidgetUpdater.TAB_STATS) {
            widgetNavState = WidgetNavRequest(seq = ++navSeq, tabRoute = FolioTab.STATS.route)
            intent.removeExtra(FolioWidgetUpdater.EXTRA_OPEN_TAB)
        } else if (bookId > 0L) {
            widgetNavState = WidgetNavRequest(seq = ++navSeq, bookId = bookId)
            intent.removeExtra(FolioWidgetUpdater.EXTRA_OPEN_BOOK_ID)
        } else if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            if (uri != null) {
                widgetNavState = WidgetNavRequest(seq = ++navSeq, externalUri = uri)
                intent.data = null
                intent.clipData = null
            }
        }
    }
}

private enum class FolioTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    LIBRARY("library", R.string.nav_library, Icons.Filled.LocalLibrary, Icons.Outlined.LocalLibrary),
    SHELVES("shelves", R.string.nav_shelves, Icons.Filled.CollectionsBookmark, Icons.Outlined.CollectionsBookmark),
    STATS("stats", R.string.nav_stats, Icons.Filled.BarChart, Icons.Outlined.BarChart),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun FolioAppRoot(
    widgetNavRequest: WidgetNavRequest? = null,
    libraryViewModel: LibraryViewModel = viewModel()
) {
    val state by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val darkTheme = state.preferences.appDarkTheme ?: isSystemInDarkTheme()

    FolioTheme(darkTheme = darkTheme, accent = state.preferences.appAccent) {
        val navController = rememberNavController()
        val navBackStack by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStack?.destination?.route ?: FolioTab.LIBRARY.route
        val isReaderRoute = currentRoute.startsWith("reader/")

        LaunchedEffect(widgetNavRequest?.seq) {
            val req = widgetNavRequest ?: return@LaunchedEffect
            when {
                req.externalUri != null -> {
                    libraryViewModel.openExternalBookUri(req.externalUri) { openedBookId ->
                        navController.navigate("reader/$openedBookId") {
                            launchSingleTop = true
                        }
                    }
                }
                req.bookId != null -> {
                    navController.navigate("reader/${req.bookId}") {
                        launchSingleTop = true
                    }
                }
                req.tabRoute == FolioTab.STATS.route -> {
                    navController.navigate(FolioTab.STATS.route) {
                        popUpTo(FolioTab.LIBRARY.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }

        val snackbarHostState = remember { SnackbarHostState() }
        var editingBook by remember { mutableStateOf<BookWithRelations?>(null) }
        var shelvingBook by remember { mutableStateOf<BookWithRelations?>(null) }
        var removingBook by remember { mutableStateOf<BookEntity?>(null) }
        var pendingNotesExportBook by remember { mutableStateOf<BookEntity?>(null) }

        val importFilesLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            libraryViewModel.importFiles(uris)
        }

        val linkFolderLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) libraryViewModel.linkFolder(uri)
        }

        val importFontLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) libraryViewModel.importCustomFont(uri)
        }

        val exportBackupLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/zip")
        ) { uri ->
            if (uri != null) libraryViewModel.exportBackup(uri)
        }

        val restoreBackupLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) libraryViewModel.restoreBackup(uri)
        }

        val exportNotesLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/markdown")
        ) { uri ->
            val targetBook = pendingNotesExportBook
            if (uri != null && targetBook != null) {
                libraryViewModel.exportNotesMarkdown(targetBook, uri)
            }
            pendingNotesExportBook = null
        }

        LaunchedEffect(state.statusMessage) {
            state.statusMessage?.let { msg ->
                snackbarHostState.showSnackbar(msg)
                libraryViewModel.clearStatusMessage()
            }
        }

        val visibleTabs = remember(state.preferences.showStats) {
            FolioTab.entries.filter { it != FolioTab.STATS || state.preferences.showStats }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp && !isReaderRoute

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!isReaderRoute && !useNavRail) {
                        NavigationBar {
                            visibleTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(FolioTab.LIBRARY.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes)) },
                                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isReaderRoute) Modifier else Modifier.padding(innerPadding))
                ) {
                    if (useNavRail) {
                        NavigationRail(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                        ) {
                            visibleTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(FolioTab.LIBRARY.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes)) },
                                    modifier = Modifier.testTag("nav_rail_${tab.route}")
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = FolioTab.LIBRARY.route,
                        modifier = Modifier.weight(1f).fillMaxSize()
                    ) {
                        composable(FolioTab.LIBRARY.route) {
                            LibraryScreen(
                                state = state,
                                onOpenBook = { bookId -> navController.navigate("reader/$bookId") },
                                onImportBooks = { importFilesLauncher.launch(BookFormat.supportedMimeTypes) },
                                onLinkFolder = { linkFolderLauncher.launch(null) },
                                onSearchChange = { libraryViewModel.setSearchQuery(it) },
                                onSelectFilter = { libraryViewModel.setFilter(it) },
                                onClearTagFilter = { libraryViewModel.setTagFilter(null) },
                                onToggleViewMode = { libraryViewModel.toggleViewMode() },
                                onToggleGroupSeries = { libraryViewModel.toggleGroupSeries() },
                                onSelectSortOrder = { libraryViewModel.setSortOrder(it) },
                                onEditMetadata = { editingBook = it },
                                onAssignShelves = { shelvingBook = it },
                                onRefreshBook = { libraryViewModel.reparseBook(it) },
                                onExportNotes = { book ->
                                    pendingNotesExportBook = book
                                    val safe = book.title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                                    exportNotesLauncher.launch("${safe}_notes.md")
                                },
                                onRemoveBook = { removingBook = it }
                            )
                        }

                        composable(FolioTab.SHELVES.route) {
                            ShelvesScreen(
                                state = state,
                                onOpenBook = { bookId -> navController.navigate("reader/$bookId") },
                                onCreateShelf = { libraryViewModel.createShelf(it) },
                                onRenameShelf = { s, name -> libraryViewModel.renameShelf(s, name) },
                                onDeleteShelf = { libraryViewModel.deleteShelf(it) },
                                onSelectTag = { tag ->
                                    libraryViewModel.setTagFilter(tag)
                                    navController.navigate(FolioTab.LIBRARY.route) {
                                        popUpTo(FolioTab.LIBRARY.route) { saveState = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(FolioTab.STATS.route) {
                            StatsScreen(state = state)
                        }

                        composable(FolioTab.SETTINGS.route) {
                            SettingsScreen(
                                state = state,
                                onUpdatePrefs = { libraryViewModel.updatePreferences(it) },
                                onImportCustomFont = {
                                    importFontLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "*/*"))
                                },
                                onLinkFolder = { linkFolderLauncher.launch(null) },
                                onRescanFolder = { libraryViewModel.rescanFolder(it) },
                                onRemoveFolder = { libraryViewModel.removeWatchedFolder(it) },
                                onExportBackup = {
                                    exportBackupLauncher.launch("folio_library_backup.zip")
                                },
                                onRestoreBackup = {
                                    restoreBackupLauncher.launch(arrayOf("application/zip", "*/*"))
                                },
                                onResetStats = { libraryViewModel.resetReadingStats() }
                            )
                        }

                        composable(
                            route = "reader/{bookId}",
                            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
                        ) { entry ->
                            val bookId = entry.arguments?.getLong("bookId") ?: return@composable
                            ReaderScreen(
                                bookId = bookId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }

        editingBook?.let { item ->
            MetadataEditorDialog(
                item = item,
                onDismiss = { editingBook = null },
                onSave = { title, author, series, idx, desc, coverUri, tags ->
                    libraryViewModel.updateBookMetadata(item.book, title, author, series, idx, desc, coverUri, tags)
                    editingBook = null
                }
            )
        }

        shelvingBook?.let { item ->
            AssignShelvesDialog(
                item = item,
                allShelves = state.shelves,
                onDismiss = { shelvingBook = null },
                onCreateShelf = { libraryViewModel.createShelf(it) },
                onSave = { shelfIds ->
                    libraryViewModel.setBookShelves(item.book.id, shelfIds)
                    shelvingBook = null
                }
            )
        }

        removingBook?.let { book ->
            RemoveBookDialog(
                bookTitle = book.title,
                onDismiss = { removingBook = null },
                onConfirm = { deleteLocalCopy ->
                    libraryViewModel.deleteBook(book, deleteLocalCopy)
                    removingBook = null
                }
            )
        }
    }
}
