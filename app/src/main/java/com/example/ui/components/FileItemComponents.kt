package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.ui.theme.ApkTagColor
import com.example.ui.theme.AudioTagColor
import com.example.ui.theme.DocumentTagColor
import com.example.ui.theme.FolderColor
import com.example.ui.theme.ImageTagColor
import com.example.ui.theme.SystemTagColor
import com.example.ui.theme.VideoTagColor
import com.example.ui.theme.ZipTagColor
import com.example.util.FileUtils
import java.io.File

fun getFileIconAndColor(item: FileItem): Pair<ImageVector, Color> {
    if (item.isDirectory) {
        return Pair(Icons.Default.Folder, FolderColor)
    }
    return when (FileUtils.getCategoryForFile(File(item.path))) {
        FileCategory.IMAGES -> Pair(Icons.Default.Image, ImageTagColor)
        FileCategory.VIDEOS -> Pair(Icons.Default.VideoLibrary, VideoTagColor)
        FileCategory.AUDIO -> Pair(Icons.Default.Audiotrack, AudioTagColor)
        FileCategory.DOCUMENTS -> Pair(Icons.Default.Description, DocumentTagColor)
        FileCategory.APKS -> Pair(Icons.Default.Android, ApkTagColor)
        FileCategory.ARCHIVES -> Pair(Icons.Default.Archive, ZipTagColor)
        else -> {
            when (item.extension) {
                "json", "xml", "html", "js", "kt", "java", "py", "sh" -> Pair(Icons.Default.Code, SystemTagColor)
                else -> Pair(Icons.Default.InsertDriveFile, Color(0xFF64748B))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onOpenProperties: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onExtractZip: (() -> Unit)? = null,
    onExportToDrive: (() -> Unit)? = null,
    onSendToPhotos: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val (icon, tint) = getFileIconAndColor(item)
    val isImg = !item.isDirectory && FileUtils.getCategoryForFile(File(item.path)) == FileCategory.IMAGES

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        color = backgroundColor,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelect() else onItemClick()
                },
                onLongClick = onItemLongClick
            )
            .testTag("file_item_${item.name}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth()
        ) {
            // Checkbox in selection mode
            if (isSelectionMode) {
                IconButton(
                    onClick = onToggleSelect,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Selecionar",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Thumbnail / Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (isImg) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(item.path))
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // File Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.formattedSize,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = item.formattedDate,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // More Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Mais opções",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Compartilhar") },
                        onClick = {
                            showMenu = false
                            onShare()
                        }
                    )
                    if (onExportToDrive != null && !item.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("☁ Salvar no Google Drive") },
                            onClick = {
                                showMenu = false
                                onExportToDrive()
                            }
                        )
                    }
                    if (onSendToPhotos != null && !item.isDirectory && (item.mimeType.startsWith("image/") || item.mimeType.startsWith("video/"))) {
                        DropdownMenuItem(
                            text = { Text("🌸 Enviar ao Google Fotos") },
                            onClick = {
                                showMenu = false
                                onSendToPhotos()
                            }
                        )
                    }
                    if (onExtractZip != null) {
                        DropdownMenuItem(
                            text = { Text("Descompactar ZIP") },
                            onClick = {
                                showMenu = false
                                onExtractZip()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Renomear") },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Propriedades") },
                        onClick = {
                            showMenu = false
                            onOpenProperties()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemGridCard(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onToggleSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = getFileIconAndColor(item)
    val isImg = !item.isDirectory && FileUtils.getCategoryForFile(File(item.path)) == FileCategory.IMAGES

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelect() else onItemClick() },
                onLongClick = onItemLongClick
            )
            .testTag("grid_item_${item.name}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (isImg) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(item.path))
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(36.dp)
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .size(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = item.formattedSize,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
