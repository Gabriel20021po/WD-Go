package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.util.SystemShortcutHelper
import com.example.viewmodel.FileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WirelessTransferScreen(
    viewModel: FileViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val serverStatus by viewModel.serverStatus.collectAsState()

    var usernameInput by remember(serverStatus.username) { mutableStateOf(serverStatus.username) }
    var passwordInput by remember(serverStatus.password) { mutableStateOf(serverStatus.password) }
    var authEnabledInput by remember(serverStatus.authEnabled) { mutableStateOf(serverStatus.authEnabled) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshServerNetwork()
    }

    BackHandler {
        focusManager.clearFocus()
        keyboardController?.hide()
        viewModel.setScreen(AppScreen.HOME)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
            .imePadding()
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Servidor Web & PC",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        viewModel.setScreen(AppScreen.HOME)
                    },
                    modifier = Modifier.testTag("wireless_back_btn")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            },
            actions = {
                if (serverStatus.isRunning) {
                    val activeUrl = serverStatus.url.ifEmpty { "http://${serverStatus.ipAddress}:${serverStatus.port}" }
                    IconButton(
                        onClick = {
                            shareServerLink(
                                context = context,
                                url = activeUrl,
                                username = serverStatus.username,
                                pass = serverStatus.password,
                                authEnabled = serverStatus.authEnabled
                            )
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Compartilhar link")
                    }
                }
                IconButton(onClick = { viewModel.refreshServerNetwork() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Atualizar redes")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Main Status & Power Toggle Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (serverStatus.isRunning) Color(0xFF0F2E22) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (serverStatus.isRunning) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (serverStatus.isRunning) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = if (serverStatus.isRunning) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (serverStatus.isRunning) "Servidor Online e Pronto" else "Servidor Desativado",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = if (serverStatus.isRunning) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (serverStatus.isRunning)
                                "Acesse pelo PC na mesma rede Wi-Fi ou pelo Roteador do celular"
                            else
                                "Toque abaixo para ligar o servidor de arquivos",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.toggleWirelessServer()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (serverStatus.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("toggle_server_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (serverStatus.isRunning) "Desligar Servidor" else "Iniciar Servidor",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Real Active Connection Card (Shows whenever server is running!)
            if (serverStatus.isRunning) {
                item {
                    val activeUrl = serverStatus.url.ifEmpty { "http://${serverStatus.ipAddress}:${serverStatus.port}" }
                    val directAuthUrl = if (serverStatus.authEnabled) {
                        "http://${serverStatus.username}:${serverStatus.password}@${serverStatus.ipAddress}:${serverStatus.port}"
                    } else activeUrl

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Endereço Principal Ativo",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "PORTA ${serverStatus.port}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Large URL Display Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Digite no navegador do seu Computador:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = activeUrl,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(activeUrl))
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copiar IP",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    shareServerLink(
                                                        context = context,
                                                        url = activeUrl,
                                                        username = serverStatus.username,
                                                        pass = serverStatus.password,
                                                        authEnabled = serverStatus.authEnabled
                                                    )
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Compartilhar Link",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (serverStatus.authEnabled) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Login: ${serverStatus.username} | Senha: ${serverStatus.password}",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF34D399)
                                                )
                                                Text(
                                                    text = "Link com login embutido para o PC",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(directAuthUrl))
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copiar link com login",
                                                        tint = Color(0xFF34D399),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        shareServerLink(
                                                            context = context,
                                                            url = activeUrl,
                                                            username = serverStatus.username,
                                                            pass = serverStatus.password,
                                                            authEnabled = true
                                                        )
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Share,
                                                        contentDescription = "Compartilhar com senha",
                                                        tint = Color(0xFF34D399),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Primary Share Server Link Button
                            Button(
                                onClick = {
                                    shareServerLink(
                                        context = context,
                                        url = activeUrl,
                                        username = serverStatus.username,
                                        pass = serverStatus.password,
                                        authEnabled = serverStatus.authEnabled
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_share_server_link")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Compartilhar Link do Servidor", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Test in Device's Localhost Browser (100% reliable on device & emulator!)
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val localUrl = "http://localhost:${serverStatus.port}"
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(localUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(browserIntent)
                                    } catch (ignored: Exception) {}
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testar no Navegador do Celular (Localhost)", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Authentication & Password Protection Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Proteção por Senha",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (authEnabledInput) "Exige usuário e senha no navegador" else "Acesso livre sem login",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = authEnabledInput,
                                onCheckedChange = {
                                    authEnabledInput = it
                                    viewModel.updateServerCredentials(usernameInput, passwordInput, it)
                                }
                            )
                        }

                        if (authEnabledInput) {
                            Spacer(modifier = Modifier.height(14.dp))

                            // Username Field
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                label = { Text("Nome de Usuário") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Password Field
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Senha de Acesso") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Key, contentDescription = null)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Mostrar senha"
                                        )
                                    }
                                },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        viewModel.updateServerCredentials(usernameInput, passwordInput, authEnabledInput)
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.updateServerCredentials(usernameInput, passwordInput, authEnabledInput)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salvar Nome e Senha")
                            }
                        }
                    }
                }
            }

            // Dedicated Option 1: Ponto de Acesso / Hotspot
            item {
                val hasHotspotInterface = serverStatus.availableEndpoints.any {
                    it.type == "Roteador do Celular" || it.ip.startsWith("192.168.43.")
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF0284C7).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiTethering,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Opção 1: Ponto de Acesso (Hotspot)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Para usar fora de casa ou sem roteador Wi-Fi",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Hotspot Status Notice
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (hasHotspotInterface) Color(0xFF064E3B) else MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasHotspotInterface) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (hasHotspotInterface) Color(0xFF34D399) else Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (hasHotspotInterface)
                                        "Ponto de Acesso detectado e ativo!"
                                    else
                                        "Para conectar pelo Hotspot, ligue o Ponto de Acesso nas configurações abaixo.",
                                    fontSize = 11.sp,
                                    color = if (hasHotspotInterface) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Action to open Android Tethering Settings
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                SystemShortcutHelper.openTetheringSettings(context)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiTethering,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir Configurações do Ponto de Acesso")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                shareServerLink(
                                    context = context,
                                    url = "http://192.168.43.1:${serverStatus.port}",
                                    username = serverStatus.username,
                                    pass = serverStatus.password,
                                    authEnabled = serverStatus.authEnabled
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compartilhar Link do Ponto de Acesso", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Hotspot Steps
                        NoWifiStep(
                            badge = "Passo 1",
                            title = "Ligue o Ponto de Acesso",
                            description = "Toque no botão azul acima e ative o Ponto de Acesso Wi-Fi do seu celular."
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        NoWifiStep(
                            badge = "Passo 2",
                            title = "Conecte seu Computador",
                            description = "No seu computador, selecione a rede Wi-Fi emitida pelo seu celular e conecte-se."
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        NoWifiStep(
                            badge = "Passo 3",
                            title = "Abra o Navegador no PC",
                            description = "No PC, acesse http://192.168.43.1:8080 (ou o IP principal ativo mostrado no topo)."
                        )
                    }
                }
            }

            // Other offline alternatives (USB Cable)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Usb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Opção 2: Cabo USB (Ancoragem)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Conecte o celular com cabo USB no PC e ative 'Ancoragem USB' no Android para transferir arquivos diretamente sem precisar de sinal sem fio.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoWifiStep(badge: String, title: String, description: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}

private fun shareServerLink(
    context: android.content.Context,
    url: String,
    username: String,
    pass: String,
    authEnabled: Boolean
) {
    val cleanUrl = if (url.startsWith("http")) url else "http://$url"
    val shareText = buildString {
        append("📂 WD-GOFiles - Acesso ao Servidor de Arquivos\n\n")
        append("Acesse pelo navegador do seu Computador / Notebook:\n")
        append("👉 $cleanUrl\n")
        if (authEnabled) {
            append("\n🔒 Credenciais de Acesso:\n")
            append("• Usuário: $username\n")
            append("• Senha: $pass\n")
            val hostPort = cleanUrl.removePrefix("http://").removePrefix("https://")
            append("\n🔗 Link direto (login automático no PC):\n")
            append("http://$username:$pass@$hostPort\n")
        }
        append("\n💡 Dica: Certifique-se de que o computador está conectado no mesmo Wi-Fi ou no Ponto de Acesso do celular.")
    }

    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Acesso ao Servidor WD-GOFiles")
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar Link do Servidor").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

