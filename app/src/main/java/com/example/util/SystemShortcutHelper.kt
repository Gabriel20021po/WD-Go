package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object SystemShortcutHelper {

    fun hasAllFilesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun requestAllFilesPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (ex: Exception) {
                    Toast.makeText(context, "Não foi possível abrir configurações de permissão", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Permissão concedida no Android anterior ao 11", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * FolderV Signature Feature 1: Open System Files (AOSP DocumentsUI)
     */
    fun launchSystemFilesApp(context: Context, inSplitScreen: Boolean = false): Boolean {
        val intents = listOf(
            // Intent 1: Action browse document root
            Intent("android.provider.action.BROWSE").apply {
                data = Uri.parse("content://com.android.externalstorage.documents/root/primary")
            },
            // Intent 2: View document root
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse("content://com.android.externalstorage.documents/root/primary"), "vnd.android.document/root")
            },
            // Intent 3: Explicit DocumentsUI FilesActivity
            Intent().apply {
                setClassName("com.google.android.documentsui", "com.android.documentsui.files.FilesActivity")
            },
            Intent().apply {
                setClassName("com.android.documentsui", "com.android.documentsui.files.FilesActivity")
            },
            // Intent 4: Generic open document tree
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        )

        for (intent in intents) {
            try {
                if (inSplitScreen) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                    intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (ignored: Exception) {
                // Try next intent
            }
        }

        Toast.makeText(context, "Arquivos do sistema AOSP indisponíveis neste dispositivo", Toast.LENGTH_SHORT).show()
        return false
    }

    /**
     * FolderV Signature Feature 2: Direct shortcut to Android/data
     */
    fun launchAndroidDataShortcut(context: Context, inSplitScreen: Boolean = false): Boolean {
        val dataUri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Android%2Fdata")
        val intents = listOf(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(dataUri, "vnd.android.document/directory")
            },
            Intent("android.provider.action.BROWSE").apply {
                data = dataUri
            },
            Intent().apply {
                setClassName("com.google.android.documentsui", "com.android.documentsui.files.FilesActivity")
                data = dataUri
            },
            Intent().apply {
                setClassName("com.android.documentsui", "com.android.documentsui.files.FilesActivity")
                data = dataUri
            }
        )

        for (intent in intents) {
            try {
                if (inSplitScreen) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                    intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (ignored: Exception) {
            }
        }

        // Fallback: notify user and launch root documents
        Toast.makeText(context, "Abrindo no gerenciador do sistema...", Toast.LENGTH_SHORT).show()
        return launchSystemFilesApp(context, inSplitScreen)
    }

    /**
     * FolderV Signature Feature 3: Direct shortcut to Android/obb
     */
    fun launchAndroidObbShortcut(context: Context, inSplitScreen: Boolean = false): Boolean {
        val obbUri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Android%2Fobb")
        val intents = listOf(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(obbUri, "vnd.android.document/directory")
            },
            Intent("android.provider.action.BROWSE").apply {
                data = obbUri
            },
            Intent().apply {
                setClassName("com.google.android.documentsui", "com.android.documentsui.files.FilesActivity")
                data = obbUri
            },
            Intent().apply {
                setClassName("com.android.documentsui", "com.android.documentsui.files.FilesActivity")
                data = obbUri
            }
        )

        for (intent in intents) {
            try {
                if (inSplitScreen) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                    intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (ignored: Exception) {
            }
        }

        Toast.makeText(context, "Abrindo no gerenciador do sistema...", Toast.LENGTH_SHORT).show()
        return launchSystemFilesApp(context, inSplitScreen)
    }

    /**
     * Open file with external application via FileProvider
     */
    fun openFileWithApp(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val mime = FileUtils.getMimeType(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Abrir com").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Nenhum aplicativo compatível encontrado", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share file(s) with external apps
     */
    fun shareFiles(context: Context, files: List<File>) {
        if (files.isEmpty()) return
        try {
            if (files.size == 1) {
                val file = files.first()
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = FileUtils.getMimeType(file)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Compartilhar arquivo").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } else {
                val uris = ArrayList<Uri>()
                for (file in files) {
                    uris.add(
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )
                    )
                }
                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Compartilhar ${files.size} arquivos").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao compartilhar arquivo(s)", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Launch Google Drive App or Web Interface
     */
    fun openGoogleDriveApp(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.docs")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir o Google Drive", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Send specific file directly to Google Drive upload target
     */
    fun sendFileToDrive(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = FileUtils.getMimeType(file)
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.google.android.apps.docs")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to chooser if Google Drive package is not dedicated
            shareFiles(context, listOf(file))
        }
    }

    /**
     * Launch Google Photos App or Web Interface
     */
    fun openGooglePhotosApp(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.photos")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://photos.google.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir o Google Fotos", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Send photo/video directly to Google Photos upload target
     */
    fun sendToGooglePhotos(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = FileUtils.getMimeType(file)
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.google.android.apps.photos")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            shareFiles(context, listOf(file))
        }
    }

    /**
     * Open Android Hotspot / Tethering settings directly
     */
    fun openTetheringSettings(context: Context) {
        val intents = listOf(
            Intent("android.settings.TETHER_SETTINGS"),
            Intent("android.settings.WIFI_AP_SETTINGS"),
            Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (ignored: Exception) {}
        }
        Toast.makeText(context, "Abra as configurações do celular e ative o Ponto de Acesso", Toast.LENGTH_SHORT).show()
    }
}
