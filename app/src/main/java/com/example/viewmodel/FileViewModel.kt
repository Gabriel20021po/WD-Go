package com.example.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppScreen
import com.example.model.ClipboardAction
import com.example.model.ClipboardState
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.SortOption
import com.example.model.StorageStats
import com.example.model.ViewMode
import com.example.server.LocalFileServer
import com.example.server.ServerStatus
import com.example.util.FileUtils
import com.example.util.SystemShortcutHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class FileUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val currentDirectory: File = FileUtils.rootStorageDirectory,
    val files: List<FileItem> = emptyList(),
    val selectedFiles: Set<FileItem> = emptySet(),
    val isSelectionMode: Boolean = false,
    val sortOption: SortOption = SortOption.NAME_ASC,
    val viewMode: ViewMode = ViewMode.LIST,
    val showHiddenFiles: Boolean = false,
    val searchQuery: String = "",
    val activeCategory: FileCategory = FileCategory.ALL,
    val clipboard: ClipboardState? = null,
    val storageStats: StorageStats = StorageStats(),
    val hasStoragePermission: Boolean = false,
    val favoritePaths: List<String> = emptyList(),
    // Dual pane states
    val dualPaneDirLeft: File = FileUtils.rootStorageDirectory,
    val dualPaneDirRight: File = File(FileUtils.rootStorageDirectory, "Download"),
    val dualPaneFilesLeft: List<FileItem> = emptyList(),
    val dualPaneFilesRight: List<FileItem> = emptyList(),
    // Dialog states
    val previewImageItem: FileItem? = null,
    val previewTextItem: Pair<FileItem, String>? = null,
    val propertiesTarget: FileItem? = null,
    val propertiesChecksum: String? = null,
    val showNewFolderDialog: Boolean = false,
    val showNewFileDialog: Boolean = false,
    val renameTarget: FileItem? = null,
    val deleteTargets: List<FileItem>? = null,
    val statusMessage: String? = null,
    val pendingExportFile: File? = null,
    val isLoading: Boolean = false
)

class FileViewModel(application: Application) : AndroidViewModel(application) {

    private val localServer = LocalFileServer(application)
    val serverStatus: StateFlow<ServerStatus> = localServer.status

    private val _uiState = MutableStateFlow(
        FileUiState(
            hasStoragePermission = SystemShortcutHelper.hasAllFilesPermission(),
            favoritePaths = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
            )
        )
    )
    val uiState: StateFlow<FileUiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndLoad()
    }

    fun checkPermissionAndLoad() {
        val hasPerm = SystemShortcutHelper.hasAllFilesPermission()
        _uiState.update { it.copy(hasStoragePermission = hasPerm) }
        loadCurrentDirectory()
        refreshStorageStats()
    }

    fun setScreen(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen, searchQuery = "") }
        if (screen == AppScreen.EXPLORER) {
            loadCurrentDirectory()
        } else if (screen == AppScreen.DUAL_PANE) {
            loadDualPane()
        } else if (screen == AppScreen.STORAGE_ANALYSIS) {
            refreshStorageStats()
        }
    }

    fun navigateTo(directory: File) {
        _uiState.update {
            it.copy(
                currentDirectory = directory,
                currentScreen = AppScreen.EXPLORER,
                activeCategory = FileCategory.ALL,
                selectedFiles = emptySet(),
                isSelectionMode = false,
                searchQuery = ""
            )
        }
        loadCurrentDirectory()
    }

    fun navigateUp(): Boolean {
        val current = _uiState.value.currentDirectory
        val parent = current.parentFile
        val root = FileUtils.rootStorageDirectory

        if (_uiState.value.activeCategory != FileCategory.ALL) {
            _uiState.update { it.copy(activeCategory = FileCategory.ALL) }
            loadCurrentDirectory()
            return true
        }

        return if (parent != null && (current != root && parent.canRead())) {
            navigateTo(parent)
            true
        } else {
            if (_uiState.value.currentScreen != AppScreen.HOME) {
                setScreen(AppScreen.HOME)
                true
            } else {
                false
            }
        }
    }

    fun navigateToCategory(category: FileCategory) {
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.EXPLORER,
                activeCategory = category,
                selectedFiles = emptySet(),
                isSelectionMode = false,
                isLoading = true
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val root = FileUtils.rootStorageDirectory
            val items = FileUtils.scanCategoryFiles(root, category)
            _uiState.update {
                it.copy(
                    files = items,
                    isLoading = false
                )
            }
        }
    }

    fun loadCurrentDirectory() {
        val state = _uiState.value
        if (state.activeCategory != FileCategory.ALL) {
            navigateToCategory(state.activeCategory)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val dir = _uiState.value.currentDirectory
            val items = FileUtils.listFiles(
                directory = dir,
                showHidden = _uiState.value.showHiddenFiles,
                sortOption = _uiState.value.sortOption,
                searchQuery = _uiState.value.searchQuery
            )
            _uiState.update {
                it.copy(
                    files = items,
                    isLoading = false
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadCurrentDirectory()
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        loadCurrentDirectory()
    }

    fun toggleViewMode() {
        _uiState.update {
            val next = if (it.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
            it.copy(viewMode = next)
        }
    }

    fun toggleShowHidden() {
        _uiState.update { it.copy(showHiddenFiles = !it.showHiddenFiles) }
        loadCurrentDirectory()
    }

    // Selection
    fun toggleSelect(item: FileItem) {
        val current = _uiState.value.selectedFiles.toMutableSet()
        if (current.contains(item)) {
            current.remove(item)
        } else {
            current.add(item)
        }
        _uiState.update {
            it.copy(
                selectedFiles = current,
                isSelectionMode = current.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update {
            it.copy(
                selectedFiles = it.files.toSet(),
                isSelectionMode = true
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedFiles = emptySet(),
                isSelectionMode = false
            )
        }
    }

    // Clipboard
    fun copySelected() {
        val selected = _uiState.value.selectedFiles.toList()
        if (selected.isEmpty()) return
        _uiState.update {
            it.copy(
                clipboard = ClipboardState(selected, ClipboardAction.COPY),
                selectedFiles = emptySet(),
                isSelectionMode = false,
                statusMessage = "${selected.size} item(ns) copiado(s)"
            )
        }
    }

    fun cutSelected() {
        val selected = _uiState.value.selectedFiles.toList()
        if (selected.isEmpty()) return
        _uiState.update {
            it.copy(
                clipboard = ClipboardState(selected, ClipboardAction.CUT),
                selectedFiles = emptySet(),
                isSelectionMode = false,
                statusMessage = "${selected.size} item(ns) recortado(s)"
            )
        }
    }

    fun clearClipboard() {
        _uiState.update { it.copy(clipboard = null) }
    }

    fun pasteClipboard(targetDir: File = _uiState.value.currentDirectory) {
        val clip = _uiState.value.clipboard ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            var successCount = 0
            for (item in clip.items) {
                val source = File(item.path)
                val ok = if (clip.action == ClipboardAction.COPY) {
                    FileUtils.copy(source, targetDir)
                } else {
                    FileUtils.move(source, targetDir)
                }
                if (ok) successCount++
            }
            _uiState.update {
                it.copy(
                    clipboard = if (clip.action == ClipboardAction.CUT) null else it.clipboard,
                    isLoading = false,
                    statusMessage = "$successCount item(ns) colado(s) com sucesso!"
                )
            }
            loadCurrentDirectory()
            refreshStorageStats()
        }
    }

    // CRUD
    fun requestDeleteSelected() {
        val targets = _uiState.value.selectedFiles.toList()
        if (targets.isNotEmpty()) {
            _uiState.update { it.copy(deleteTargets = targets) }
        }
    }

    fun requestDeleteSingle(item: FileItem) {
        _uiState.update { it.copy(deleteTargets = listOf(item)) }
    }

    fun confirmDelete() {
        val targets = _uiState.value.deleteTargets ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, deleteTargets = null) }
            var count = 0
            for (item in targets) {
                if (FileUtils.deleteRecursive(File(item.path))) {
                    count++
                }
            }
            _uiState.update {
                it.copy(
                    selectedFiles = emptySet(),
                    isSelectionMode = false,
                    isLoading = false,
                    statusMessage = "$count item(ns) excluído(s)"
                )
            }
            loadCurrentDirectory()
            refreshStorageStats()
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(deleteTargets = null) }
    }

    fun requestRename(item: FileItem) {
        _uiState.update { it.copy(renameTarget = item) }
    }

    fun dismissRenameDialog() {
        _uiState.update { it.copy(renameTarget = null) }
    }

    fun performRename(newName: String) {
        val item = _uiState.value.renameTarget ?: return
        if (newName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val ok = FileUtils.rename(File(item.path), newName.trim())
            _uiState.update {
                it.copy(
                    renameTarget = null,
                    statusMessage = if (ok) "Renomeado para $newName" else "Erro ao renomear"
                )
            }
            loadCurrentDirectory()
        }
    }

    fun openNewFolderDialog() {
        _uiState.update { it.copy(showNewFolderDialog = true) }
    }

    fun dismissNewFolderDialog() {
        _uiState.update { it.copy(showNewFolderDialog = false) }
    }

    fun performCreateFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val dir = _uiState.value.currentDirectory
            val ok = FileUtils.createFolder(dir, name.trim())
            _uiState.update {
                it.copy(
                    showNewFolderDialog = false,
                    statusMessage = if (ok) "Pasta criada com sucesso!" else "Falha ao criar pasta"
                )
            }
            loadCurrentDirectory()
        }
    }

    fun openNewFileDialog() {
        _uiState.update { it.copy(showNewFileDialog = true) }
    }

    fun dismissNewFileDialog() {
        _uiState.update { it.copy(showNewFileDialog = false) }
    }

    fun performCreateTextFile(name: String, content: String = "") {
        if (name.isBlank()) return
        val finalName = if (!name.contains(".")) "$name.txt" else name
        viewModelScope.launch(Dispatchers.IO) {
            val dir = _uiState.value.currentDirectory
            val ok = FileUtils.createTextFile(dir, finalName.trim(), content)
            _uiState.update {
                it.copy(
                    showNewFileDialog = false,
                    statusMessage = if (ok) "Arquivo criado: $finalName" else "Falha ao criar arquivo"
                )
            }
            loadCurrentDirectory()
        }
    }

    fun openTextFileForEdit(item: FileItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(item.path)
            val content = try {
                file.readText()
            } catch (e: Exception) {
                "Não foi possível ler o arquivo: ${e.message}"
            }
            _uiState.update { it.copy(previewTextItem = Pair(item, content)) }
        }
    }

    fun saveEditedTextFile(content: String) {
        val pair = _uiState.value.previewTextItem ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                File(pair.first.path).writeText(content)
                _uiState.update {
                    it.copy(
                        previewTextItem = null,
                        statusMessage = "Arquivo salvo com sucesso!"
                    )
                }
                loadCurrentDirectory()
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Erro ao salvar arquivo") }
            }
        }
    }

    fun dismissTextEditor() {
        _uiState.update { it.copy(previewTextItem = null) }
    }

    fun openImagePreview(item: FileItem) {
        _uiState.update { it.copy(previewImageItem = item) }
    }

    fun dismissImagePreview() {
        _uiState.update { it.copy(previewImageItem = null) }
    }

    fun openProperties(item: FileItem) {
        _uiState.update { it.copy(propertiesTarget = item, propertiesChecksum = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val checksum = if (!item.isDirectory && item.size < 50 * 1024 * 1024) {
                FileUtils.calculateChecksum(File(item.path), "SHA-256")
            } else {
                "Cálculo ignorado para pastas ou arquivos > 50MB"
            }
            _uiState.update { it.copy(propertiesChecksum = checksum) }
        }
    }

    fun dismissProperties() {
        _uiState.update { it.copy(propertiesTarget = null, propertiesChecksum = null) }
    }

    // Zip compression & extraction
    fun compressSelected(zipName: String = "compactado.zip") {
        val items = _uiState.value.selectedFiles.toList()
        if (items.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val dir = _uiState.value.currentDirectory
            val finalZipName = if (!zipName.endsWith(".zip")) "$zipName.zip" else zipName
            val destZip = File(dir, finalZipName)
            val sources = items.map { File(it.path) }
            val ok = FileUtils.zipFiles(sources, destZip)
            _uiState.update {
                it.copy(
                    selectedFiles = emptySet(),
                    isSelectionMode = false,
                    isLoading = false,
                    statusMessage = if (ok) "Arquivo ZIP criado: $finalZipName" else "Erro ao compactar arquivos"
                )
            }
            loadCurrentDirectory()
        }
    }

    fun extractZip(item: FileItem) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val zipFile = File(item.path)
            val targetDir = File(zipFile.parentFile, zipFile.nameWithoutExtension)
            val ok = FileUtils.extractZip(zipFile, targetDir)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    statusMessage = if (ok) "Extraído para /${targetDir.name}" else "Erro ao descompactar arquivo"
                )
            }
            loadCurrentDirectory()
        }
    }

    // Favorites
    fun toggleFavorite(path: String) {
        val favorites = _uiState.value.favoritePaths.toMutableList()
        if (favorites.contains(path)) {
            favorites.remove(path)
        } else {
            favorites.add(path)
        }
        _uiState.update { it.copy(favoritePaths = favorites) }
    }

    // Storage Stats
    fun refreshStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = FileUtils.calculateStorageStats(FileUtils.rootStorageDirectory)
            _uiState.update { it.copy(storageStats = stats) }
        }
    }

    fun cleanTempFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val root = FileUtils.rootStorageDirectory
            var cleanedCount = 0
            fun cleanDir(dir: File) {
                dir.listFiles()?.forEach { f ->
                    if (f.name.endsWith(".tmp", ignoreCase = true) ||
                        f.name.endsWith(".log", ignoreCase = true) ||
                        f.name.equals(".thumbcache", ignoreCase = true)
                    ) {
                        if (f.delete()) cleanedCount++
                    } else if (f.isDirectory && !f.name.startsWith(".")) {
                        cleanDir(f)
                    }
                }
            }
            cleanDir(root)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    statusMessage = "Limpeza concluída: $cleanedCount arquivo(s) temporário(s) removido(s)"
                )
            }
            refreshStorageStats()
            loadCurrentDirectory()
        }
    }

    // Dual Pane support
    fun loadDualPane() {
        viewModelScope.launch(Dispatchers.IO) {
            val left = FileUtils.listFiles(_uiState.value.dualPaneDirLeft)
            val right = FileUtils.listFiles(_uiState.value.dualPaneDirRight)
            _uiState.update {
                it.copy(
                    dualPaneFilesLeft = left,
                    dualPaneFilesRight = right
                )
            }
        }
    }

    fun dualPaneNavigate(isLeft: Boolean, dir: File) {
        if (isLeft) {
            _uiState.update { it.copy(dualPaneDirLeft = dir) }
        } else {
            _uiState.update { it.copy(dualPaneDirRight = dir) }
        }
        loadDualPane()
    }

    fun dualPaneCopyAcross(fromLeft: Boolean, item: FileItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetDir = if (fromLeft) _uiState.value.dualPaneDirRight else _uiState.value.dualPaneDirLeft
            val source = File(item.path)
            val ok = FileUtils.copy(source, targetDir)
            _uiState.update {
                it.copy(statusMessage = if (ok) "${item.name} copiado para painel oposto!" else "Erro ao copiar")
            }
            loadDualPane()
        }
    }

    // Server
    fun toggleWirelessServer() {
        if (localServer.status.value.isRunning) {
            localServer.stopServer()
            _uiState.update { it.copy(statusMessage = "Servidor desconectado") }
        } else {
            localServer.startServer(8080)
            _uiState.update { it.copy(statusMessage = "Servidor iniciado na porta 8080!") }
        }
    }

    fun updateServerCredentials(user: String, pass: String, authEnabled: Boolean) {
        localServer.updateCredentials(user, pass, authEnabled)
        _uiState.update { it.copy(statusMessage = "Credenciais do servidor salvas!") }
    }

    fun refreshServerNetwork() {
        localServer.refreshNetworkEndpoints()
    }

    // Google Drive & Cloud Import/Export via Storage Access Framework
    fun setPendingExportFile(file: File?) {
        _uiState.update { it.copy(pendingExportFile = file) }
    }

    fun importFromUris(uris: List<android.net.Uri>, targetDir: File = _uiState.value.currentDirectory) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val context = getApplication<Application>()
            var count = 0
            for (uri in uris) {
                val created = FileUtils.copyUriToFile(context, uri, targetDir)
                if (created != null && created.exists()) {
                    count++
                }
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    statusMessage = "$count arquivo(s) importado(s) do Google Drive para ${targetDir.name}!"
                )
            }
            loadCurrentDirectory()
            refreshStorageStats()
        }
    }

    fun exportToUri(destUri: android.net.Uri) {
        val file = _uiState.value.pendingExportFile ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val context = getApplication<Application>()
            val ok = FileUtils.copyFileToUri(context, file, destUri)
            _uiState.update {
                it.copy(
                    pendingExportFile = null,
                    isLoading = false,
                    statusMessage = if (ok) "${file.name} transferido para o Google Drive com sucesso!" else "Falha ao salvar no Google Drive"
                )
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        localServer.stopServer()
    }
}
