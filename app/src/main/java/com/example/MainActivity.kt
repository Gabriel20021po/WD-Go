package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Splitscreen
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.model.AppScreen
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.FilePropertiesDialog
import com.example.ui.components.ImagePreviewDialog
import com.example.ui.components.NewFileDialog
import com.example.ui.components.NewFolderDialog
import com.example.ui.components.RenameDialog
import com.example.ui.components.TextEditorDialog
import com.example.ui.screens.DualPaneExplorerScreen
import com.example.ui.screens.ExplorerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.StorageAnalysisScreen
import com.example.ui.screens.WirelessTransferScreen
import com.example.ui.theme.GoFilesTheme
import com.example.util.SystemShortcutHelper
import com.example.viewmodel.FileViewModel

data class NavItem(
    val screen: AppScreen,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {

    private val viewModel: FileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GoFilesTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: FileViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Request permissions on startup
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkPermissionAndLoad()
    }

    // Google Drive & Cloud Import/Export launchers (SAF)
    val importDriveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFromUris(uris)
        }
    }

    val exportDriveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            viewModel.exportToUri(uri)
        } else {
            viewModel.setPendingExportFile(null)
        }
    }

    // Google Photos & Media Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFromUris(uris)
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    // Refresh on return to foreground (e.g. from All Files Access settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionAndLoad()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Snackbar notifications
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val navItems = listOf(
        NavItem(AppScreen.HOME, "Início", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem(AppScreen.EXPLORER, "Arquivos", Icons.Filled.Folder, Icons.Outlined.Folder),
        NavItem(AppScreen.DUAL_PANE, "Painel Duplo", Icons.Filled.Splitscreen, Icons.Outlined.Splitscreen),
        NavItem(AppScreen.STORAGE_ANALYSIS, "Espaço", Icons.Filled.PieChart, Icons.Outlined.PieChart),
        NavItem(AppScreen.WIRELESS_TRANSFER, "Wi-Fi PC", Icons.Filled.Wifi, Icons.Outlined.Wifi)
    )

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!uiState.isSelectionMode) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar")
                ) {
                    navItems.forEach { item ->
                        val isSelected = uiState.currentScreen == item.screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.setScreen(item.screen) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_${item.title}")
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentScreen) {
                AppScreen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onImportFromDrive = { importDriveLauncher.launch(arrayOf("*/*")) },
                    onImportFromPhotos = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    }
                )
                AppScreen.EXPLORER -> ExplorerScreen(
                    viewModel = viewModel,
                    onImportFromDrive = { importDriveLauncher.launch(arrayOf("*/*")) },
                    onImportFromPhotos = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    onExportToDrive = { file ->
                        viewModel.setPendingExportFile(file)
                        exportDriveLauncher.launch(file.name)
                    }
                )
                AppScreen.DUAL_PANE -> DualPaneExplorerScreen(viewModel = viewModel)
                AppScreen.STORAGE_ANALYSIS -> StorageAnalysisScreen(viewModel = viewModel)
                AppScreen.WIRELESS_TRANSFER -> WirelessTransferScreen(viewModel = viewModel)
            }
        }

        // Global Dialogs
        uiState.previewImageItem?.let { item ->
            ImagePreviewDialog(
                item = item,
                onDismiss = { viewModel.dismissImagePreview() }
            )
        }

        uiState.previewTextItem?.let { pair ->
            TextEditorDialog(
                item = pair.first,
                initialContent = pair.second,
                onSave = { viewModel.saveEditedTextFile(it) },
                onDismiss = { viewModel.dismissTextEditor() }
            )
        }

        uiState.propertiesTarget?.let { item ->
            FilePropertiesDialog(
                item = item,
                checksum = uiState.propertiesChecksum,
                onDismiss = { viewModel.dismissProperties() }
            )
        }

        if (uiState.showNewFolderDialog) {
            NewFolderDialog(
                onConfirm = { viewModel.performCreateFolder(it) },
                onDismiss = { viewModel.dismissNewFolderDialog() }
            )
        }

        if (uiState.showNewFileDialog) {
            NewFileDialog(
                onConfirm = { name, content -> viewModel.performCreateTextFile(name, content) },
                onDismiss = { viewModel.dismissNewFileDialog() }
            )
        }

        uiState.renameTarget?.let { item ->
            RenameDialog(
                item = item,
                onConfirm = { viewModel.performRename(it) },
                onDismiss = { viewModel.dismissRenameDialog() }
            )
        }

        uiState.deleteTargets?.let { items ->
            DeleteConfirmDialog(
                items = items,
                onConfirm = { viewModel.confirmDelete() },
                onDismiss = { viewModel.dismissDeleteDialog() }
            )
        }
    }
}
