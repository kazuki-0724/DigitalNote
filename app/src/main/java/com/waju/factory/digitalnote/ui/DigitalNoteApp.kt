package com.waju.factory.digitalnote.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.waju.factory.digitalnote.data.local.AppDatabase
import com.waju.factory.digitalnote.data.repository.NoteRepository
import com.waju.factory.digitalnote.navigation.AppRoute
import com.waju.factory.digitalnote.ui.components.HomeTopBar
import com.waju.factory.digitalnote.ui.components.NoteUpsertDialog
import com.waju.factory.digitalnote.ui.components.SectionTopBar
import com.waju.factory.digitalnote.ui.screens.CanvasScreen
import com.waju.factory.digitalnote.ui.screens.EditorScreen
import com.waju.factory.digitalnote.ui.screens.NotesScreen
import com.waju.factory.digitalnote.ui.viewmodel.CanvasViewModel
import com.waju.factory.digitalnote.ui.viewmodel.CanvasViewModelFactory
import com.waju.factory.digitalnote.ui.viewmodel.EditorViewModel
import com.waju.factory.digitalnote.ui.viewmodel.EditorViewModelFactory
import com.waju.factory.digitalnote.ui.viewmodel.NotesViewModel
import com.waju.factory.digitalnote.ui.viewmodel.NotesViewModelFactory
import com.waju.factory.digitalnote.ui.theme.NoteCoverColors
import kotlinx.coroutines.launch

@Composable
fun DigitalNoteApp() {
    val context = LocalContext.current
    val repository = remember {
        val database = AppDatabase.getInstance(context)
        NoteRepository(database.noteDao(), database.strokeDao(), database.textBoxDao())
    }

    val notesViewModel: NotesViewModel = viewModel(factory = NotesViewModelFactory(repository))
    val notes by notesViewModel.notes.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.orEmpty()
    val isCanvasRoute = currentRoute.startsWith("canvas")
    val isEditorRoute = currentRoute.startsWith("editor")
    val currentCanvasNoteId = backStackEntry?.arguments?.getInt("noteId")
    val currentCanvasNoteTitle = notes.firstOrNull { it.id == currentCanvasNoteId }?.title

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var newNoteTitle by rememberSaveable { mutableStateOf("") }
    var newNoteCoverColorIndex by rememberSaveable { mutableStateOf(0) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var editingNoteId by rememberSaveable { mutableStateOf<Int?>(null) }
    var editingNoteTitle by rememberSaveable { mutableStateOf("") }
    var editingNoteCoverColorIndex by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            when {
                isEditorRoute -> Unit
                isCanvasRoute -> SectionTopBar(
                    title = currentCanvasNoteTitle?.ifBlank { "キャンバス" } ?: "キャンバス",
                    onBackToTop = {
                        navController.navigate(AppRoute.Notes.route) {
                            popUpTo(AppRoute.Notes.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
                else -> HomeTopBar()
            }
        },
        floatingActionButton = {
            if (currentRoute.startsWith(AppRoute.Notes.route)) {
                FloatingActionButton(onClick = {
                    newNoteTitle = ""
                    newNoteCoverColorIndex = 0
                    showCreateDialog = true
                }) {
                    Icon(Icons.Outlined.Add, contentDescription = "新規ノート")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Notes.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(AppRoute.Notes.route) {
                NotesScreen(
                    notes = notes,
                    modifier = Modifier.fillMaxSize(),
                    onOpenNote = { note ->
                        navController.navigate(AppRoute.Canvas.create(note.id))
                    },
                    onLongPressNote = { note ->
                        editingNoteId = note.id
                        editingNoteTitle = note.title
                        editingNoteCoverColorIndex = NoteCoverColors.indexOfFirst { it.value == note.coverColor.value }
                            .takeIf { it >= 0 } ?: 0
                        showEditDialog = true
                    }
                )
            }

            composable(
                route = AppRoute.Canvas.route,
                arguments = listOf(navArgument("noteId") { type = NavType.IntType })
            ) { entry ->
                val noteId = entry.arguments?.getInt("noteId") ?: (notes.firstOrNull()?.id ?: 1)
                val canvasViewModel: CanvasViewModel = viewModel(
                    key = "canvas_$noteId",
                    factory = CanvasViewModelFactory(repository, noteId)
                )
                val canvasState by canvasViewModel.uiState.collectAsStateWithLifecycle()

                CanvasScreen(
                    uiState = canvasState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    onToolChanged = canvasViewModel::onToolChanged,
                    onModeChanged = canvasViewModel::onModeChanged,
                    onBackgroundStyleChanged = canvasViewModel::onBackgroundStyleChanged,
                    onInputModeChanged = canvasViewModel::onInputModeChanged,
                    onPrevPage = canvasViewModel::goToPrevPage,
                    onNextPage = canvasViewModel::goToNextPage,
                    onAddPage = canvasViewModel::addPage,
                    onGoToPage = canvasViewModel::goToPage,
                    onColorChanged = canvasViewModel::onColorChanged,
                    onPaletteColorChanged = canvasViewModel::onPaletteColorChanged,
                    onStrokeWidthChanged = canvasViewModel::onStrokeWidthChanged,
                    onSensitivityChanged = canvasViewModel::onSensitivityChanged,
                    onTransform = canvasViewModel::onTransform,
                    onResetTransform = canvasViewModel::resetTransform,
                    onFitToPage = canvasViewModel::fitToPage,
                    onStrokeStart = canvasViewModel::startStroke,
                    onStrokeMove = canvasViewModel::extendStroke,
                    onStrokeEnd = canvasViewModel::finishStroke,
                    onStrokeCancel = canvasViewModel::cancelStroke,
                    onAddStickyNote = canvasViewModel::addStickyNote,
                    onUpdateStickyNoteText = canvasViewModel::updateStickyNoteText,
                    onMoveStickyNote = canvasViewModel::moveStickyNote,
                    onResizeStickyNote = canvasViewModel::resizeStickyNote,
                    onUpdateStickyNoteStyle = canvasViewModel::updateStickyNoteStyle,
                    onDeleteStickyNote = canvasViewModel::deleteStickyNote,
                    onToggleReadOnly = canvasViewModel::toggleReadOnly,
                    onUndo = canvasViewModel::undo,
                    onRedo = canvasViewModel::redo,
                    onClear = canvasViewModel::clear
                )
            }

            composable(
                route = AppRoute.Editor.route,
                arguments = listOf(navArgument("noteId") { type = NavType.IntType })
            ) { entry ->
                val noteId = entry.arguments?.getInt("noteId") ?: 1
                val editorViewModel: EditorViewModel = viewModel(
                    key = "editor_$noteId",
                    factory = EditorViewModelFactory(repository, noteId)
                )
                val editorState by editorViewModel.uiState.collectAsStateWithLifecycle()
                val note = notes.firstOrNull { it.id == noteId }

                EditorScreen(
                    noteId = noteId,
                    title = editorState.title,
                    content = editorState.content,
                    tones = note?.tones.orEmpty(),
                    onTitleChange = editorViewModel::onTitleChanged,
                    onContentChange = editorViewModel::onContentChanged,
                    onOpenCanvas = {
                        navController.navigate(AppRoute.Canvas.create(noteId))
                    },
                    onDone = {
                        editorViewModel.save()
                        navController.popBackStack()
                    },
                    onClose = { navController.popBackStack() }
                )
            }
        }

        if (showCreateDialog) {
            NoteUpsertDialog(
                title = "ノートを作成",
                noteTitle = newNoteTitle,
                selectedCoverColorIndex = newNoteCoverColorIndex,
                onTitleChange = { newNoteTitle = it },
                onCoverColorChange = { newNoteCoverColorIndex = it },
                onDismiss = {
                    showCreateDialog = false
                    newNoteTitle = ""
                    newNoteCoverColorIndex = 0
                },
                onConfirm = { title, coverColor ->
                    val safeTitle = title.trim().ifBlank { "新しいノート" }
                    scope.launch {
                        val newId = notesViewModel.createNote(safeTitle, coverColor)
                        showCreateDialog = false
                        newNoteTitle = ""
                        newNoteCoverColorIndex = 0
                        navController.navigate(AppRoute.Canvas.create(newId))
                    }
                },
                confirmText = "作成"
            )
        }

        if (showEditDialog && editingNoteId != null) {
            NoteUpsertDialog(
                title = "ノートを編集",
                noteTitle = editingNoteTitle,
                selectedCoverColorIndex = editingNoteCoverColorIndex,
                onTitleChange = { editingNoteTitle = it },
                onCoverColorChange = { editingNoteCoverColorIndex = it },
                onDismiss = {
                    showEditDialog = false
                    editingNoteId = null
                    editingNoteTitle = ""
                    editingNoteCoverColorIndex = 0
                },
                onConfirm = { title, coverColor ->
                    val noteId = editingNoteId ?: return@NoteUpsertDialog
                    notesViewModel.updateNoteAppearance(noteId, title.trim().ifBlank { "新しいノート" }, coverColor)
                    showEditDialog = false
                    editingNoteId = null
                    editingNoteTitle = ""
                    editingNoteCoverColorIndex = 0
                },
                onDelete = {
                    val noteId = editingNoteId ?: return@NoteUpsertDialog
                    notesViewModel.deleteNote(noteId)
                    showEditDialog = false
                    editingNoteId = null
                    editingNoteTitle = ""
                    editingNoteCoverColorIndex = 0
                },
                confirmText = "保存"
            )
        }
    }
}
