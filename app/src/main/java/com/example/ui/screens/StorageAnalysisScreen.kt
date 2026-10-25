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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.model.FileCategory
import com.example.model.StorageStats
import com.example.ui.components.FileItemRow
import com.example.ui.theme.ApkTagColor
import com.example.ui.theme.AudioTagColor
import com.example.ui.theme.DocumentTagColor
import com.example.ui.theme.ImageTagColor
import com.example.ui.theme.VideoTagColor
import com.example.ui.theme.ZipTagColor
import com.example.util.SystemShortcutHelper
import com.example.viewmodel.FileViewModel
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalysisScreen(
    viewModel: FileViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val stats = uiState.storageStats

    BackHandler {
        viewModel.setScreen(AppScreen.HOME)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Análise de Armazenamento",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = { viewModel.setScreen(AppScreen.HOME) },
                    modifier = Modifier.testTag("storage_analysis_back_btn")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refreshStorageStats() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Atualizar")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Main gauge card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Armazenamento Total",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val usedGb = stats.usedBytes.toDouble() / (1024.0 * 1024 * 1024)
                                val totalGb = stats.totalBytes.toDouble() / (1024.0 * 1024 * 1024)
                                Text(
                                    text = String.format(Locale.US, "%.1f GB / %.1f GB", usedGb, totalGb),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val percent = (stats.usedPercentage * 100).toInt()
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$percent%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Multi-color segmented progress bar
                        MultiColorStorageBar(stats = stats)

                        Spacer(modifier = Modifier.height(12.dp))

                        val freeGb = stats.freeBytes.toDouble() / (1024.0 * 1024 * 1024)
                        Text(
                            text = String.format(Locale.US, "Espaço Livre Restante: %.1f GB", freeGb),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Quick Cleanup Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Limpeza Rápida",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Remover arquivos temporários e caches desnecessários",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.cleanTempFiles() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_quick_clean")
                        ) {
                            Text("Limpar", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Categories Breakdown List
            item {
                Text(
                    text = "Detalhamento por Tipo de Mídia",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CategoryBreakdownRow("Imagens", stats.imagesBytes, ImageTagColor) {
                            viewModel.navigateToCategory(FileCategory.IMAGES)
                        }
                        CategoryBreakdownRow("Vídeos", stats.videosBytes, VideoTagColor) {
                            viewModel.navigateToCategory(FileCategory.VIDEOS)
                        }
                        CategoryBreakdownRow("Áudios", stats.audioBytes, AudioTagColor) {
                            viewModel.navigateToCategory(FileCategory.AUDIO)
                        }
                        CategoryBreakdownRow("Documentos", stats.docsBytes, DocumentTagColor) {
                            viewModel.navigateToCategory(FileCategory.DOCUMENTS)
                        }
                        CategoryBreakdownRow("Aplicativos / APKs", stats.apksBytes, ApkTagColor) {
                            viewModel.navigateToCategory(FileCategory.APKS)
                        }
                        CategoryBreakdownRow("Arquivos ZIP / Compactados", stats.archivesBytes, ZipTagColor) {
                            viewModel.navigateToCategory(FileCategory.ARCHIVES)
                        }
                        CategoryBreakdownRow("Outros Arquivos do Sistema", stats.otherBytes, Color(0xFF64748B), null)
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiColorStorageBar(stats: StorageStats) {
    val total = stats.totalBytes.toFloat().coerceAtLeast(1f)
    val imgW = stats.imagesBytes / total
    val vidW = stats.videosBytes / total
    val audW = stats.audioBytes / total
    val docW = stats.docsBytes / total
    val apkW = stats.apksBytes / total
    val zipW = stats.archivesBytes / total
    val otherW = stats.otherBytes / total

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF334155))
    ) {
        if (imgW > 0.001f) Box(modifier = Modifier.weight(imgW.coerceAtLeast(0.01f)).height(12.dp).background(ImageTagColor))
        if (vidW > 0.001f) Box(modifier = Modifier.weight(vidW.coerceAtLeast(0.01f)).height(12.dp).background(VideoTagColor))
        if (audW > 0.001f) Box(modifier = Modifier.weight(audW.coerceAtLeast(0.01f)).height(12.dp).background(AudioTagColor))
        if (docW > 0.001f) Box(modifier = Modifier.weight(docW.coerceAtLeast(0.01f)).height(12.dp).background(DocumentTagColor))
        if (apkW > 0.001f) Box(modifier = Modifier.weight(apkW.coerceAtLeast(0.01f)).height(12.dp).background(ApkTagColor))
        if (zipW > 0.001f) Box(modifier = Modifier.weight(zipW.coerceAtLeast(0.01f)).height(12.dp).background(ZipTagColor))
        if (otherW > 0.001f) Box(modifier = Modifier.weight(otherW.coerceAtLeast(0.01f)).height(12.dp).background(Color(0xFF64748B)))
        val freeW = (stats.freeBytes / total).coerceAtLeast(0.01f)
        Box(modifier = Modifier.weight(freeW).height(12.dp).background(Color(0xFF1E293B)))
    }
}

@Composable
private fun CategoryBreakdownRow(
    title: String,
    bytes: Long,
    color: Color,
    onClick: (() -> Unit)?
) {
    val sizeStr = formatBytes(bytes)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = sizeStr,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onClick != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text("➔", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups >= units.size) digitGroups = units.size - 1
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
