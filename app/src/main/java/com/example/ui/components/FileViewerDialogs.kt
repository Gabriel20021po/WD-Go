package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.FileItem
import java.io.File

@Composable
fun ImagePreviewDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(item.path))
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Top Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "${item.formattedSize} • ${item.formattedDate}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_image_preview_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun TextEditorDialog(
    item: FileItem,
    initialContent: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Toolbar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "Editor de Texto / Código",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { onSave(content) },
                            modifier = Modifier.testTag("save_text_file_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Salvar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_text_editor_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("text_editor_content"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val lineCount = content.lines().size
                    Text(
                        text = "$lineCount linhas • ${content.length} caracteres",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { onSave(content) },
                        modifier = Modifier.testTag("btn_save_bottom")
                    ) {
                        Text("Salvar Alterações")
                    }
                }
            }
        }
    }
}

@Composable
fun FilePropertiesDialog(
    item: FileItem,
    checksum: String?,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Propriedades",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                PropertyRow("Nome:", item.name)
                PropertyRow("Tipo:", if (item.isDirectory) "Pasta de Diretório" else item.mimeType)
                PropertyRow("Caminho:", item.path)
                PropertyRow("Tamanho:", if (item.isDirectory) "${item.childrenCount} itens" else "${item.formattedSize} (${item.size} bytes)")
                PropertyRow("Modificado:", item.formattedDate)

                if (!item.isDirectory) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Checksum SHA-256:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (checksum == null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Calculando hash...", fontSize = 11.sp)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = checksum,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { clipboardManager.setText(AnnotatedString(checksum)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar hash",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_close_properties")
            ) {
                Text("Fechar")
            }
        }
    )
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NewFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Pasta", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Digite o nome da nova pasta:", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = { Text("Ex: Meus Documentos") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_folder_name")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (folderName.isNotBlank()) onConfirm(folderName) },
                enabled = folderName.isNotBlank(),
                modifier = Modifier.testTag("btn_confirm_new_folder")
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun NewFileDialog(
    onConfirm: (name: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    var fileName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Arquivo", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Digite o nome do arquivo (ex: notas.txt, script.sh):", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    placeholder = { Text("arquivo.txt") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_file_name")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (fileName.isNotBlank()) onConfirm(fileName, "") },
                enabled = fileName.isNotBlank(),
                modifier = Modifier.testTag("btn_confirm_new_file")
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun RenameDialog(
    item: FileItem,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(item.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renomear", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_rename")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (newName.isNotBlank() && newName != item.name) onConfirm(newName) },
                enabled = newName.isNotBlank() && newName != item.name,
                modifier = Modifier.testTag("btn_confirm_rename")
            ) {
                Text("Renomear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    items: List<FileItem>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir ${items.size} item(ns)?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "Os itens selecionados serão apagados permanentemente do armazenamento.",
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("btn_confirm_delete")
            ) {
                Text("Excluir Permanentemente")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun CompressZipDialog(
    itemCount: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var zipName by remember { mutableStateOf("arquivo_compactado.zip") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compactar em ZIP", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Compactar $itemCount item(ns) em um arquivo .zip:", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = zipName,
                    onValueChange = { zipName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (zipName.isNotBlank()) onConfirm(zipName) },
                enabled = zipName.isNotBlank()
            ) {
                Text("Compactar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
