package com.example.server

import android.content.Context
import com.example.util.FileUtils
import com.example.util.NetworkEndpoint
import com.example.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64

data class ServerStatus(
    val isRunning: Boolean = false,
    val port: Int = 8080,
    val ipAddress: String = "127.0.0.1",
    val url: String = "",
    val authEnabled: Boolean = true,
    val username: String = "admin",
    val password: String = "1234",
    val availableEndpoints: List<NetworkEndpoint> = emptyList()
)

class LocalFileServer(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null

    private val _status = MutableStateFlow(
        ServerStatus(
            availableEndpoints = NetworkUtils.getAllAvailableEndpoints(context)
        )
    )
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    fun updateCredentials(username: String, password: String, authEnabled: Boolean) {
        _status.value = _status.value.copy(
            username = username.trim().ifEmpty { "admin" },
            password = password.trim().ifEmpty { "1234" },
            authEnabled = authEnabled
        )
    }

    fun refreshNetworkEndpoints() {
        val endpoints = NetworkUtils.getAllAvailableEndpoints(context)
        val ip = NetworkUtils.getLocalIpAddress(context)
        val port = _status.value.port
        _status.value = _status.value.copy(
            ipAddress = ip,
            availableEndpoints = endpoints,
            url = if (_status.value.isRunning) "http://$ip:$port" else ""
        )
    }

    fun startServer(port: Int = 8080) {
        if (_status.value.isRunning) return

        refreshNetworkEndpoints()
        val ip = _status.value.ipAddress
        val url = "http://$ip:$port"

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                _status.value = _status.value.copy(
                    isRunning = true,
                    port = port,
                    ipAddress = ip,
                    url = url
                )

                while (isActive) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                // Stopped or error
            } finally {
                stopServer()
            }
        }
    }

    fun stopServer() {
        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null

        _status.value = _status.value.copy(
            isRunning = false,
            url = ""
        )
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = socket.getOutputStream()
            val writer = PrintWriter(out)

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0]
            val fullPath = parts[1]

            // Drain headers and extract Authorization header
            var headerLine: String?
            var authHeader: String? = null
            var contentLength = 0
            while (reader.readLine().also { headerLine = it } != null) {
                if (headerLine.isNullOrEmpty()) break
                if (headerLine!!.startsWith("Authorization:", ignoreCase = true)) {
                    authHeader = headerLine!!.substringAfter(":").trim()
                }
                if (headerLine!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = headerLine!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            // Check authentication
            val currentStatus = _status.value
            if (currentStatus.authEnabled) {
                val authorized = checkAuth(authHeader, currentStatus.username, currentStatus.password)
                if (!authorized) {
                    sendUnauthorizedResponse(writer, out)
                    return@withContext
                }
            }

            val decodedUrl = URLDecoder.decode(fullPath, "UTF-8")
            val uriPath = decodedUrl.substringBefore("?")
            val queryString = if (decodedUrl.contains("?")) decodedUrl.substringAfter("?") else ""

            when {
                uriPath == "/" || uriPath.startsWith("/browse") -> {
                    val requestedDir = getQueryParam(queryString, "path")
                    val targetDir = if (!requestedDir.isNullOrBlank()) File(requestedDir) else FileUtils.rootStorageDirectory
                    val html = buildHtmlPage(targetDir, currentStatus.username)
                    sendResponse(writer, out, "200 OK", "text/html; charset=utf-8", html.toByteArray())
                }
                uriPath == "/download" -> {
                    val filePath = getQueryParam(queryString, "file")
                    if (filePath != null) {
                        val file = File(filePath)
                        if (file.exists() && file.isFile) {
                            sendFileResponse(out, file)
                        } else {
                            sendResponse(writer, out, "404 Not Found", "text/plain", "Arquivo não encontrado".toByteArray())
                        }
                    } else {
                        sendResponse(writer, out, "400 Bad Request", "text/plain", "Parâmetro file ausente".toByteArray())
                    }
                }
                else -> {
                    sendResponse(writer, out, "404 Not Found", "text/plain", "Página não encontrada".toByteArray())
                }
            }
        } catch (e: Exception) {
            // Client error
        } finally {
            try {
                socket.close()
            } catch (ignored: Exception) {}
        }
    }

    private fun checkAuth(header: String?, user: String, pass: String): Boolean {
        if (header == null || !header.startsWith("Basic ", ignoreCase = true)) return false
        val base64Credentials = header.substring(6).trim()
        return try {
            val decoded = String(Base64.getDecoder().decode(base64Credentials), Charsets.UTF_8)
            val colonIndex = decoded.indexOf(':')
            if (colonIndex > 0) {
                val reqUser = decoded.substring(0, colonIndex)
                val reqPass = decoded.substring(colonIndex + 1)
                reqUser == user && reqPass == pass
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private fun sendUnauthorizedResponse(writer: PrintWriter, out: OutputStream) {
        val html = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <title>Acesso Protegido - WD-GOFiles</title>
                <style>
                    body { font-family: sans-serif; background: #0F172A; color: #F8FAFC; text-align: center; padding: 40px; }
                    .box { max-width: 450px; margin: 40px auto; background: #1E293B; border-radius: 16px; padding: 30px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }
                    h2 { color: #38BDF8; }
                    p { color: #94A3B8; font-size: 14px; }
                    .tip { background: #334155; padding: 12px; border-radius: 8px; margin-top: 20px; font-size: 13px; color: #CBD5E1; }
                </style>
            </head>
            <body>
                <div class="box">
                    <h2>🔒 Acesso Protegido</h2>
                    <p>Este servidor de arquivos WD-GOFiles requer autenticação por nome de usuário e senha.</p>
                    <div class="tip">Recarregue a página e insira as credenciais configuradas no aplicativo.</div>
                </div>
            </body>
            </html>
        """.trimIndent()

        writer.print("HTTP/1.1 401 Unauthorized\r\n")
        writer.print("WWW-Authenticate: Basic realm=\"WD-GOFiles Secure Server\"\r\n")
        writer.print("Content-Type: text/html; charset=utf-8\r\n")
        writer.print("Content-Length: ${html.toByteArray().size}\r\n")
        writer.print("Connection: close\r\n\r\n")
        writer.flush()
        out.write(html.toByteArray())
        out.flush()
    }

    private fun getQueryParam(query: String, param: String): String? {
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0 && pair.substring(0, idx) == param) {
                return pair.substring(idx + 1)
            }
        }
        return null
    }

    private fun sendResponse(writer: PrintWriter, out: OutputStream, status: String, contentType: String, data: ByteArray) {
        writer.print("HTTP/1.1 $status\r\n")
        writer.print("Content-Type: $contentType\r\n")
        writer.print("Content-Length: ${data.size}\r\n")
        writer.print("Connection: close\r\n\r\n")
        writer.flush()
        out.write(data)
        out.flush()
    }

    private fun sendFileResponse(out: OutputStream, file: File) {
        val mime = FileUtils.getMimeType(file)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: $mime\r\n" +
                "Content-Length: ${file.length()}\r\n" +
                "Content-Disposition: attachment; filename=\"${file.name}\"\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray())

        FileInputStream(file).use { fis ->
            val buffer = ByteArray(32768)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                out.write(buffer, 0, bytesRead)
            }
        }
        out.flush()
    }

    private fun buildHtmlPage(directory: File, currentUser: String): String {
        val parent = directory.parentFile
        val parentLink = if (parent != null && parent.canRead()) {
            """<a class="btn-back" href="/browse?path=${URLEncoder.encode(parent.absolutePath, "UTF-8")}">⬅ Pasta Anterior</a>"""
        } else ""

        val files = try {
            directory.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val rows = StringBuilder()
        for (f in files) {
            val isDir = f.isDirectory
            val icon = if (isDir) "📁" else "📄"
            val size = if (isDir) "Pasta" else FileUtils.toFileItem(f).formattedSize
            val link = if (isDir) {
                """<a href="/browse?path=${URLEncoder.encode(f.absolutePath, "UTF-8")}">$icon ${f.name}</a>"""
            } else {
                """<a href="/download?file=${URLEncoder.encode(f.absolutePath, "UTF-8")}" download>$icon ${f.name}</a>"""
            }
            val downloadBtn = if (!isDir) {
                """<a class="download-link" href="/download?file=${URLEncoder.encode(f.absolutePath, "UTF-8")}" download>Baixar</a>"""
            } else ""

            rows.append(
                """<tr>
                    <td>$link</td>
                    <td>$size</td>
                    <td>$downloadBtn</td>
                </tr>"""
            )
        }

        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>WD-GOFiles - Servidor Protegido</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background: #0F172A; color: #F8FAFC; margin: 0; padding: 24px; }
                    .container { max-width: 900px; margin: 0 auto; background: #1E293B; border-radius: 16px; padding: 24px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }
                    .header { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #334155; padding-bottom: 16px; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
                    .brand { display: flex; align-items: center; gap: 12px; font-size: 24px; font-weight: 700; color: #38BDF8; }
                    .user-badge { background: #064E3B; color: #34D399; padding: 4px 12px; border-radius: 20px; font-size: 13px; font-weight: 600; }
                    .path-bar { background: #090D16; padding: 12px 16px; border-radius: 8px; font-size: 14px; color: #94A3B8; word-break: break-all; margin-bottom: 20px; border: 1px solid #334155; }
                    .btn-back { display: inline-block; background: #0284C7; color: white; padding: 8px 16px; border-radius: 8px; text-decoration: none; font-size: 14px; margin-bottom: 16px; font-weight: 600; }
                    .btn-back:hover { background: #0369A1; }
                    table { width: 100%; border-collapse: collapse; }
                    th, td { text-align: left; padding: 12px 16px; border-bottom: 1px solid #334155; }
                    th { color: #94A3B8; font-size: 13px; text-transform: uppercase; }
                    tr:hover { background: rgba(56, 189, 248, 0.05); }
                    a { color: #F8FAFC; text-decoration: none; font-weight: 500; }
                    a:hover { color: #38BDF8; text-decoration: underline; }
                    .download-link { background: #10B981; color: white; padding: 4px 12px; border-radius: 6px; font-size: 12px; }
                    .download-link:hover { background: #059669; color: white; text-decoration: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="brand">📂 WD-GOFiles Servidor de Arquivos</div>
                        <div class="user-badge">🔒 Conectado como: $currentUser</div>
                    </div>
                    <div class="path-bar">📁 ${directory.absolutePath}</div>
                    $parentLink
                    <table>
                        <thead>
                            <tr>
                                <th>Nome</th>
                                <th style="width: 140px;">Tamanho</th>
                                <th style="width: 100px;">Ação</th>
                            </tr>
                        </thead>
                        <tbody>
                            $rows
                        </tbody>
                    </table>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
