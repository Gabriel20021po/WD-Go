package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.model.FileItem
import com.example.ui.components.getFileIconAndColor
import com.example.ui.theme.FolderColor
import com.example.viewmodel.FileViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualPaneExplorerScreen(
    viewModel: FileViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    text = "Painel Duplo (Dual Pane)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = { viewModel.setScreen(AppScreen.HOME) },
                    modifier = Modifier.testTag("dual_pane_back_btn")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.loadDualPane() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Atualizar")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Split view container: Left and Right (or Top and Bottom if narrow)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 600.dp

            if (isWide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Pane
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        PaneView(
                            title = "Painel Esquerdo",
                            currentDir = uiState.dualPaneDirLeft,
                            files = uiState.dualPaneFilesLeft,
                            onNavigate = { viewModel.dualPaneNavigate(isLeft = true, dir = it) },
                            onCopyAcross = { viewModel.dualPaneCopyAcross(fromLeft = true, item = it) },
                            copyButtonLabel = "Copiar ➔"
                        )
                    }

                    VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Right Pane
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        PaneView(
                            title = "Painel Direito",
                            currentDir = uiState.dualPaneDirRight,
                            files = uiState.dualPaneFilesRight,
                            onNavigate = { viewModel.dualPaneNavigate(isLeft = false, dir = it) },
                            onCopyAcross = { viewModel.dualPaneCopyAcross(fromLeft = false, item = it) },
                            copyButtonLabel = "⬅ Copiar"
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Pane (Left)
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        PaneView(
                            title = "Painel Superior",
                            currentDir = uiState.dualPaneDirLeft,
                            files = uiState.dualPaneFilesLeft,
                            onNavigate = { viewModel.dualPaneNavigate(isLeft = true, dir = it) },
                            onCopyAcross = { viewModel.dualPaneCopyAcross(fromLeft = true, item = it) },
                            copyButtonLabel = "Copiar ⬇"
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.primary,
                        thickness = 2.dp
                    )

                    // Bottom Pane (Right)
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        PaneView(
                            title = "Painel Inferior",
                            currentDir = uiState.dualPaneDirRight,
                            files = uiState.dualPaneFilesRight,
                            onNavigate = { viewModel.dualPaneNavigate(isLeft = false, dir = it) },
                            onCopyAcross = { viewModel.dualPaneCopyAcross(fromLeft = false, item = it) },
                            copyButtonLabel = "⬆ Copiar"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaneView(
    title: String,
    currentDir: File,
    files: List<FileItem>,
    onNavigate: (File) -> Unit,
    onCopyAcross: (FileItem) -> Unit,
    copyButtonLabel: String
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Pane Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val parent = currentDir.parentFile
                    if (parent != null && parent.canRead()) {
                        IconButton(
                            onClick = { onNavigate(parent) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Subir pasta",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "📁 ${currentDir.name.ifEmpty { "Raiz" }}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${files.size} itens",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Items list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
        ) {
            items(files, key = { it.path }) { item ->
                val (icon, tint) = getFileIconAndColor(item)
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (item.isDirectory) {
                                onNavigate(File(item.path))
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(tint.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = item.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.formattedSize,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Copy Across Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clickable { onCopyAcross(item) }
                                .padding(2.dp)
                        ) {
                            Text(
                                text = copyButtonLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
