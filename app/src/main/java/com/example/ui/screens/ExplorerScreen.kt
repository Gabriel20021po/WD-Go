package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.SortOption
import com.example.model.ViewMode
import com.example.ui.components.BreadcrumbBar
import com.example.ui.components.CompressZipDialog
import com.example.ui.components.FileItemGridCard
import com.example.ui.components.FileItemRow
import com.example.ui.components.FloatingClipboardBar
import com.example.util.FileUtils
import com.example.util.SystemShortcutHelper
import com.example.viewmodel.FileViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    viewModel: FileViewModel,
    onImportFromDrive: () -> Unit = {},
    onImportFromPhotos: () -> Unit = {},
    onExportToDrive: (File) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }

    // Intercept Back button
    BackHandler {
        when {
            uiState.isSelectionMode -> viewModel.clearSelection()
            uiState.searchQuery.isNotEmpty() -> viewModel.setSearchQuery("")
            else -> viewModel.navigateUp()
        }
    }

    Scaffold(
        topBar = {
            if (uiState.isSelectionMode) {
                // Selection Mode Toolbar
                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.selectedFiles.size} selecionado(s)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.clearSelection() },
                            modifier = Modifier.testTag("clear_selection_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancelar")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(imageVector = Icons.Default.SelectAll, contentDescription = "Selecionar Tudo")
                        }
                        IconButton(onClick = { viewModel.copySelected() }) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar")
                        }
                        IconButton(onClick = { viewModel.cutSelected() }) {
                            Icon(imageVector = Icons.Default.ContentCut, contentDescription = "Recortar")
                        }
                        IconButton(
                            onClick = {
                                val files = uiState.selectedFiles.map { File(it.path) }
                                SystemShortcutHelper.shareFiles(context, files)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Compartilhar")
                        }
                        IconButton(onClick = { showCompressDialog = true }) {
                            Icon(imageVector = Icons.Default.Archive, contentDescription = "Compactar ZIP")
                        }
                        IconButton(
                            onClick = { viewModel.requestDeleteSelected() },
                            modifier = Modifier.testTag("delete_selected_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                // Normal Toolbar
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (uiState.activeCategory != FileCategory.ALL) uiState.activeCategory.title else "Explorador",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        },
                        actions = {
                            // View Mode Toggle
                            IconButton(onClick = { viewModel.toggleViewMode() }) {
                                Icon(
                                    imageVector = if (uiState.viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                                    contentDescription = "Alternar Visualização"
                                )
                            }

                            // Sort Menu
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(imageVector = Icons.Default.Sort, contentDescription = "Ordenar")
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    SortOption.values().forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (uiState.sortOption == option) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                    }
                                                    Text(
                                                        text = option.title,
                                                        fontWeight = if (uiState.sortOption == option) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            },
                                            onClick = {
                                                showSortMenu = false
                                                viewModel.setSortOption(option)
                                            }
                                        )
                                    }
                                }
                            }

                            // More Options
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Mais")
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Nova Pasta") },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.openNewFolderDialog()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Novo Arquivo") },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.openNewFileDialog()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("☁ Baixar do Google Drive") },
                                        onClick = {
                                            showMoreMenu = false
                                            onImportFromDrive()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🌸 Importar do Google Fotos") },
                                        onClick = {
                                            showMoreMenu = false
                                            onImportFromPhotos()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (uiState.showHiddenFiles) "Ocultar arquivos ocultos" else "Mostrar arquivos ocultos") },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.toggleShowHidden()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Selecionar Tudo") },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.selectAll()
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )

                    // Search Field
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Pesquisar na pasta atual...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpar busca",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .height(50.dp)
                            .testTag("explorer_search_bar")
                    )

                    // Breadcrumb Bar
                    if (uiState.activeCategory == FileCategory.ALL) {
                        BreadcrumbBar(
                            currentDirectory = uiState.currentDirectory,
                            rootDirectory = FileUtils.rootStorageDirectory,
                            onNavigateTo = { viewModel.navigateTo(it) },
                            onNavigateUp = { viewModel.navigateUp() }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                Column(horizontalAlignment = Alignment.End) {
                    if (showFabMenu) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 6.dp,
                            shadowElevation = 6.dp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth(0.5f)
                                        .clickable {
                                            showFabMenu = false
                                            viewModel.openNewFolderDialog()
                                        }
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreateNewFolder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Nova Pasta", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth(0.5f)
                                        .clickable {
                                            showFabMenu = false
                                            viewModel.openNewFileDialog()
                                        }
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NoteAdd,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Novo Arquivo", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .clickable {
                                            showFabMenu = false
                                            onImportFromDrive()
                                        }
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = Color(0xFF22C55E),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Baixar do Drive", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .clickable {
                                            showFabMenu = false
                                            onImportFromPhotos()
                                        }
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = Color(0xFFEC4899),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Importar do Fotos", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("explorer_fab")
                    ) {
                        Icon(
                            imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Criar novo item"
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.files.isEmpty() -> {
                    // Empty folder state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "Nenhum arquivo encontrado" else "Esta pasta está vazia",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "Tente pesquisar com outro termo" else "Crie pastas ou novos arquivos usando o botão +",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                uiState.viewMode == ViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(100.dp),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.files, key = { it.path }) { item ->
                            val isSelected = uiState.selectedFiles.contains(item)
                            FileItemGridCard(
                                item = item,
                                isSelected = isSelected,
                                isSelectionMode = uiState.isSelectionMode,
                                onItemClick = {
                                    handleItemClick(item, viewModel, context)
                                },
                                onItemLongClick = {
                                    viewModel.toggleSelect(item)
                                },
                                onToggleSelect = {
                                    viewModel.toggleSelect(item)
                                }
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.files, key = { it.path }) { item ->
                            val isSelected = uiState.selectedFiles.contains(item)
                            val isZip = item.extension == "zip"
                            FileItemRow(
                                item = item,
                                isSelected = isSelected,
                                isSelectionMode = uiState.isSelectionMode,
                                onItemClick = {
                                    handleItemClick(item, viewModel, context)
                                },
                                onItemLongClick = {
                                    viewModel.toggleSelect(item)
                                },
                                onToggleSelect = {
                                    viewModel.toggleSelect(item)
                                },
                                onOpenProperties = {
                                    viewModel.openProperties(item)
                                },
                                onRename = {
                                    viewModel.requestRename(item)
                                },
                                onDelete = {
                                    viewModel.requestDeleteSingle(item)
                                },
                                onShare = {
                                    SystemShortcutHelper.shareFiles(context, listOf(File(item.path)))
                                },
                                onExtractZip = if (isZip) { { viewModel.extractZip(item) } } else null,
                                onExportToDrive = { onExportToDrive(File(item.path)) },
                                onSendToPhotos = { SystemShortcutHelper.sendToGooglePhotos(context, File(item.path)) }
                            )
                        }
                    }
                }
            }

            // Floating Clipboard Bar at bottom
            if (uiState.clipboard != null && !uiState.isSelectionMode) {
                FloatingClipboardBar(
                    clipboard = uiState.clipboard!!,
                    onPaste = { viewModel.pasteClipboard() },
                    onCancel = { viewModel.clearClipboard() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    if (showCompressDialog) {
        CompressZipDialog(
            itemCount = uiState.selectedFiles.size,
            onConfirm = {
                showCompressDialog = false
                viewModel.compressSelected(it)
            },
            onDismiss = { showCompressDialog = false }
        )
    }
}

private fun handleItemClick(item: FileItem, viewModel: FileViewModel, context: android.content.Context) {
    if (item.isDirectory) {
        viewModel.navigateTo(File(item.path))
    } else {
        val cat = FileUtils.getCategoryForFile(File(item.path))
        when (cat) {
            FileCategory.IMAGES -> {
                viewModel.openImagePreview(item)
            }
            FileCategory.DOCUMENTS -> {
                // If text/code/md/json, open in editor
                if (listOf("txt", "md", "json", "xml", "log", "ini", "conf", "yaml", "html", "js", "kt", "py", "sh").contains(item.extension)) {
                    viewModel.openTextFileForEdit(item)
                } else {
                    SystemShortcutHelper.openFileWithApp(context, File(item.path))
                }
            }
            FileCategory.ARCHIVES -> {
                if (item.extension == "zip") {
                    viewModel.extractZip(item)
                } else {
                    SystemShortcutHelper.openFileWithApp(context, File(item.path))
                }
            }
            else -> {
                SystemShortcutHelper.openFileWithApp(context, File(item.path))
            }
        }
    }
}
