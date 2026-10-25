package com.example.ui.screens

import android.os.Environment
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Wifi
import com.example.R
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.model.FileCategory
import com.example.ui.components.FileItemRow
import com.example.ui.components.StorageOverviewCard
import com.example.ui.theme.ApkTagColor
import com.example.ui.theme.AudioTagColor
import com.example.ui.theme.DocumentTagColor
import com.example.ui.theme.FolderColor
import com.example.ui.theme.ImageTagColor
import com.example.ui.theme.SystemTagColor
import com.example.ui.theme.VideoTagColor
import com.example.ui.theme.ZipTagColor
import com.example.util.FileUtils
import com.example.util.SystemShortcutHelper
import com.example.viewmodel.FileViewModel
import java.io.File

data class ShortcutItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val badge: String? = null,
    val onClick: () -> Unit
)

data class CategoryItem(
    val category: FileCategory,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FileViewModel,
    onImportFromDrive: () -> Unit = {},
    onImportFromPhotos: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()

    val shortcuts = listOf(
        ShortcutItem(
            title = "Android / data",
            subtitle = "Acesso direto via sistema AOSP",
            icon = Icons.Default.Folder,
            color = Color(0xFF0284C7),
            badge = "FolderV",
            onClick = { SystemShortcutHelper.launchAndroidDataShortcut(context, inSplitScreen = false) }
        ),
        ShortcutItem(
            title = "Android / obb",
            subtitle = "Pastas de jogos e dados OBB",
            icon = Icons.Default.Folder,
            color = Color(0xFF0369A1),
            badge = "FolderV",
            onClick = { SystemShortcutHelper.launchAndroidObbShortcut(context, inSplitScreen = false) }
        ),
        ShortcutItem(
            title = "Baixar do Google Drive",
            subtitle = "Transferir do Drive para o celular",
            icon = Icons.Default.CloudDownload,
            color = Color(0xFF22C55E),
            badge = "Drive",
            onClick = { onImportFromDrive() }
        ),
        ShortcutItem(
            title = "Importar do Google Fotos",
            subtitle = "Transferir fotos/vídeos para o celular",
            icon = Icons.Default.PhotoLibrary,
            color = Color(0xFFEC4899),
            badge = "Fotos",
            onClick = { onImportFromPhotos() }
        ),
        ShortcutItem(
            title = "Abrir Google Drive",
            subtitle = "Gerenciador na nuvem",
            icon = Icons.Default.Cloud,
            color = Color(0xFFF59E0B),
            onClick = { SystemShortcutHelper.openGoogleDriveApp(context) }
        ),
        ShortcutItem(
            title = "Abrir Google Fotos",
            subtitle = "Galeria e backup na nuvem",
            icon = Icons.Default.Image,
            color = Color(0xFFF43F5E),
            onClick = { SystemShortcutHelper.openGooglePhotosApp(context) }
        ),
        ShortcutItem(
            title = "Arquivos do Sistema",
            subtitle = "Abrir gerenciador AOSP nativo",
            icon = Icons.Default.Storage,
            color = Color(0xFF6366F1),
            onClick = { SystemShortcutHelper.launchSystemFilesApp(context, inSplitScreen = false) }
        ),
        ShortcutItem(
            title = "Tela Dividida",
            subtitle = "Abrir sistema lado a lado",
            icon = Icons.Default.Splitscreen,
            color = Color(0xFF8B5CF6),
            badge = "Split",
            onClick = { SystemShortcutHelper.launchSystemFilesApp(context, inSplitScreen = true) }
        ),
        ShortcutItem(
            title = "Transferência Wi-Fi",
            subtitle = if (serverStatus.isRunning) "Ativo: ${serverStatus.ipAddress}:${serverStatus.port}" else "Acessar via PC no navegador",
            icon = Icons.Default.Wifi,
            color = if (serverStatus.isRunning) Color(0xFF10B981) else Color(0xFF14B8A6),
            badge = if (serverStatus.isRunning) "ONLINE" else null,
            onClick = { viewModel.setScreen(AppScreen.WIRELESS_TRANSFER) }
        ),
        ShortcutItem(
            title = "Painel Duplo",
            subtitle = "Explorador com 2 pastas simultâneas",
            icon = Icons.Default.Splitscreen,
            color = Color(0xFFEC4899),
            onClick = { viewModel.setScreen(AppScreen.DUAL_PANE) }
        )
    )

    val categories = listOf(
        CategoryItem(FileCategory.IMAGES, Icons.Default.Image, ImageTagColor),
        CategoryItem(FileCategory.VIDEOS, Icons.Default.VideoLibrary, VideoTagColor),
        CategoryItem(FileCategory.AUDIO, Icons.Default.Audiotrack, AudioTagColor),
        CategoryItem(FileCategory.DOCUMENTS, Icons.Default.Description, DocumentTagColor),
        CategoryItem(FileCategory.DOWNLOADS, Icons.Default.Download, Color(0xFF0EA5E9)),
        CategoryItem(FileCategory.APKS, Icons.Default.Android, ApkTagColor),
        CategoryItem(FileCategory.ARCHIVES, Icons.Default.Archive, ZipTagColor),
        CategoryItem(FileCategory.LARGE, Icons.Default.SdStorage, Color(0xFFF97316))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        CenterAlignedTopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_custom_logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "WD-GOFiles",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                if (serverStatus.isRunning) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        modifier = Modifier
                            .clickable { viewModel.setScreen(AppScreen.WIRELESS_TRANSFER) }
                            .padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Wi-Fi Ativo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { viewModel.refreshStorageStats() },
                    modifier = Modifier.testTag("home_refresh_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Permission Alert Banner if not granted
            if (!uiState.hasStoragePermission) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("permission_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permissão de Todos os Arquivos",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Para gerenciar pastas do sistema e arquivos, conceda acesso ao armazenamento.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { SystemShortcutHelper.requestAllFilesPermission(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_grant_storage_perm")
                                ) {
                                    Text("Conceder Acesso", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Trio Cloud & Files Hub Card (User Graphic: DRIVE, GOOGLE FOTOS, MEUS ARQUIVOS)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF38BDF8), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Central de Nuvem & Arquivos",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Google + Celular",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 1. DRIVE
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF22C55E).copy(alpha = 0.12f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onImportFromDrive() }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = Color(0xFF22C55E),
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "DRIVE",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Baixar / Enviar",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // 2. GOOGLE FOTOS
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFEC4899).copy(alpha = 0.12f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onImportFromPhotos() }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = Color(0xFFEC4899),
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "FOTOS",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Importar fotos",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // 3. MEUS ARQUIVOS
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.navigateTo(FileUtils.rootStorageDirectory) }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "ARQUIVOS",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Memória interna",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Storage Overview Card
            item {
                StorageOverviewCard(
                    stats = uiState.storageStats,
                    onAnalyzeClick = { viewModel.setScreen(AppScreen.STORAGE_ANALYSIS) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Quick Explore Button
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { viewModel.navigateTo(FileUtils.rootStorageDirectory) }
                        .testTag("btn_explore_internal_storage")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Explorar Armazenamento Interno",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Navegue por todas as pastas e arquivos do dispositivo",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Text(
                            text = "Abrir →",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // FolderV Signature Shortcuts
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Recursos & Atalhos FolderV",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Especiais",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    val chunked = shortcuts.chunked(2)
                    for (row in chunked) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (shortcut in row) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { shortcut.onClick() }
                                        .testTag("shortcut_${shortcut.title}")
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(shortcut.color.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = shortcut.icon,
                                                    contentDescription = null,
                                                    tint = shortcut.color,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            if (shortcut.badge != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = shortcut.color.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = shortcut.badge,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = shortcut.color,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = shortcut.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = shortcut.subtitle,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Categories Section
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Categorias de Mídia",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    val chunked = categories.chunked(4)
                    for (row in chunked) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (cat in row) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.navigateToCategory(cat.category) }
                                        .testTag("category_${cat.category.title}")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(cat.color.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = cat.icon,
                                                contentDescription = null,
                                                tint = cat.color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = cat.category.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Access Folders
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Acesso Rápido a Pastas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val quickPaths = listOf(
                        Pair("Download", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)),
                        Pair("DCIM / Câmera", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)),
                        Pair("Imagens", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)),
                        Pair("Documentos", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))
                    )

                    for ((name, dir) in quickPaths) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.navigateTo(dir) }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = FolderColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
