package com.example.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.os.StatFs
import android.webkit.MimeTypeMap
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.SortOption
import com.example.model.StorageStats
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileUtils {

    val rootStorageDirectory: File
        get() = Environment.getExternalStorageDirectory()

    fun getMimeType(file: File): String {
        val extension = file.extension.lowercase(Locale.ROOT)
        val mapped = try {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } catch (e: Throwable) {
            null
        }
        return mapped ?: when (extension) {
            "apk" -> "application/vnd.android.package-archive"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"
            "json" -> "application/json"
            "md" -> "text/markdown"
            "txt", "log", "conf", "ini", "yaml", "yml" -> "text/plain"
            "mp4", "mkv", "avi", "mov", "webm" -> "video/*"
            "mp3", "m4a", "wav", "flac", "ogg", "aac" -> "audio/*"
            "png" -> "image/png"
            "jpg", "jpeg", "webp", "gif", "bmp", "svg" -> "image/*"
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            else -> "*/*"
        }
    }

    fun getCategoryForFile(file: File): FileCategory {
        if (file.isDirectory) return FileCategory.ALL
        val ext = file.extension.lowercase(Locale.ROOT)
        return when (ext) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg" -> FileCategory.IMAGES
            "mp4", "mkv", "avi", "mov", "webm", "flv", "3gp" -> FileCategory.VIDEOS
            "mp3", "wav", "m4a", "flac", "ogg", "aac", "wma" -> FileCategory.AUDIO
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "rtf" -> FileCategory.DOCUMENTS
            "apk", "xapk", "apks" -> FileCategory.APKS
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> FileCategory.ARCHIVES
            else -> FileCategory.ALL
        }
    }

    fun toFileItem(file: File): FileItem {
        val isDir = file.isDirectory
        val children = if (isDir) {
            try {
                file.listFiles()?.size ?: 0
            } catch (e: Exception) {
                0
            }
        } else 0
        return FileItem(
            path = file.absolutePath,
            name = file.name,
            size = if (isDir) 0L else file.length(),
            isDirectory = isDir,
            lastModified = file.lastModified(),
            extension = file.extension.lowercase(Locale.ROOT),
            mimeType = getMimeType(file),
            isHidden = file.name.startsWith("."),
            childrenCount = children
        )
    }

    fun listFiles(
        directory: File,
        showHidden: Boolean = false,
        sortOption: SortOption = SortOption.NAME_ASC,
        searchQuery: String = ""
    ): List<FileItem> {
        val files = try {
            directory.listFiles() ?: emptyArray()
        } catch (e: Exception) {
            emptyArray()
        }

        var list = files.asSequence()
            .filter { showHidden || !it.name.startsWith(".") }
            .filter {
                if (searchQuery.isBlank()) true
                else it.name.contains(searchQuery, ignoreCase = true)
            }
            .map { toFileItem(it) }
            .toList()

        list = when (sortOption) {
            SortOption.NAME_ASC -> list.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))
            SortOption.NAME_DESC -> list.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.name.lowercase(Locale.ROOT) })
            SortOption.DATE_NEWEST -> list.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.lastModified })
            SortOption.DATE_OLDEST -> list.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.lastModified })
            SortOption.SIZE_LARGEST -> list.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.size })
            SortOption.SIZE_SMALLEST -> list.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.size })
            SortOption.TYPE -> list.sortedWith(compareBy({ !it.isDirectory }, { it.extension }, { it.name.lowercase(Locale.ROOT) }))
        }

        return list
    }

    fun scanCategoryFiles(root: File, category: FileCategory, maxResults: Int = 300): List<FileItem> {
        val results = mutableListOf<FileItem>()
        fun scan(dir: File) {
            if (results.size >= maxResults) return
            val children = try {
                dir.listFiles() ?: emptyArray()
            } catch (e: Exception) {
                emptyArray()
            }
            for (f in children) {
                if (results.size >= maxResults) break
                if (f.name.startsWith(".") || f.name.equals("Android", ignoreCase = true)) continue
                if (f.isDirectory) {
                    scan(f)
                } else {
                    val match = when (category) {
                        FileCategory.ALL -> true
                        FileCategory.DOWNLOADS -> dir.name.equals("Download", ignoreCase = true)
                        FileCategory.RECENT -> (System.currentTimeMillis() - f.lastModified()) < (7L * 24 * 3600 * 1000)
                        FileCategory.LARGE -> f.length() > (50L * 1024 * 1024) // > 50MB
                        else -> getCategoryForFile(f) == category
                    }
                    if (match) {
                        results.add(toFileItem(f))
                    }
                }
            }
        }
        scan(root)

        return if (category == FileCategory.RECENT) {
            results.sortedByDescending { it.lastModified }
        } else if (category == FileCategory.LARGE) {
            results.sortedByDescending { it.size }
        } else {
            results.sortedBy { it.name.lowercase(Locale.ROOT) }
        }
    }

    fun calculateStorageStats(root: File): StorageStats {
        return try {
            val stat = StatFs(root.path)
            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes
            val usedBytes = totalBytes - freeBytes

            var imgBytes = 0L
            var vidBytes = 0L
            var audBytes = 0L
            var docBytes = 0L
            var apkBytes = 0L
            var zipBytes = 0L

            fun inspectFast(dir: File, depth: Int = 0) {
                if (depth > 2) return
                val list = dir.listFiles() ?: return
                for (f in list) {
                    if (f.name.startsWith(".")) continue
                    if (f.isDirectory) {
                        inspectFast(f, depth + 1)
                    } else {
                        val len = f.length()
                        when (getCategoryForFile(f)) {
                            FileCategory.IMAGES -> imgBytes += len
                            FileCategory.VIDEOS -> vidBytes += len
                            FileCategory.AUDIO -> audBytes += len
                            FileCategory.DOCUMENTS -> docBytes += len
                            FileCategory.APKS -> apkBytes += len
                            FileCategory.ARCHIVES -> zipBytes += len
                            else -> {}
                        }
                    }
                }
            }
            inspectFast(root)

            val knownMedia = imgBytes + vidBytes + audBytes + docBytes + apkBytes + zipBytes
            val otherBytes = (usedBytes - knownMedia).coerceAtLeast(0L)

            StorageStats(
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                usedBytes = usedBytes,
                imagesBytes = imgBytes,
                videosBytes = vidBytes,
                audioBytes = audBytes,
                docsBytes = docBytes,
                apksBytes = apkBytes,
                archivesBytes = zipBytes,
                otherBytes = otherBytes
            )
        } catch (e: Exception) {
            StorageStats(totalBytes = 64L * 1024 * 1024 * 1024, freeBytes = 32L * 1024 * 1024 * 1024, usedBytes = 32L * 1024 * 1024 * 1024)
        }
    }

    fun copy(source: File, targetDir: File): Boolean {
        return try {
            if (!targetDir.exists()) targetDir.mkdirs()
            val dest = File(targetDir, source.name)
            if (source.isDirectory) {
                if (!dest.exists()) dest.mkdirs()
                source.listFiles()?.forEach { child ->
                    copy(child, dest)
                }
                true
            } else {
                source.copyTo(dest, overwrite = true)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun move(source: File, targetDir: File): Boolean {
        return try {
            val dest = File(targetDir, source.name)
            if (source.renameTo(dest)) {
                true
            } else {
                val copied = copy(source, targetDir)
                if (copied) {
                    deleteRecursive(source)
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun deleteRecursive(file: File): Boolean {
        return try {
            if (file.isDirectory) {
                file.listFiles()?.forEach { deleteRecursive(it) }
            }
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    fun rename(file: File, newName: String): Boolean {
        return try {
            val newFile = File(file.parentFile, newName)
            file.renameTo(newFile)
        } catch (e: Exception) {
            false
        }
    }

    fun createFolder(parent: File, name: String): Boolean {
        return try {
            val dir = File(parent, name)
            if (!dir.exists()) dir.mkdirs() else false
        } catch (e: Exception) {
            false
        }
    }

    fun createTextFile(parent: File, name: String, content: String = ""): Boolean {
        return try {
            val file = File(parent, name)
            if (!file.exists()) {
                file.writeText(content)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun zipFiles(sources: List<File>, destinationZip: File): Boolean {
        return try {
            ZipOutputStream(FileOutputStream(destinationZip)).use { zos ->
                for (source in sources) {
                    addFileToZip(source, source.name, zos)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addFileToZip(file: File, path: String, zos: ZipOutputStream) {
        if (file.isDirectory) {
            val entryName = if (path.endsWith("/")) path else "$path/"
            zos.putNextEntry(ZipEntry(entryName))
            zos.closeEntry()
            file.listFiles()?.forEach { child ->
                addFileToZip(child, "$entryName${child.name}", zos)
            }
        } else {
            zos.putNextEntry(ZipEntry(path))
            FileInputStream(file).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeEntry()
        }
    }

    fun extractZip(zipFile: File, outputDir: File): Boolean {
        return try {
            if (!outputDir.exists()) outputDir.mkdirs()
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFile = File(outputDir, entry.name)
                    // Guard against Zip Slip vulnerability
                    if (!newFile.canonicalPath.startsWith(outputDir.canonicalPath)) {
                        throw SecurityException("Zip Slip detected")
                    }
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun calculateChecksum(file: File, algorithm: String = "SHA-256"): String {
        return try {
            val digest = MessageDigest.getInstance(algorithm)
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Erro ao calcular checksum"
        }
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var name = "drive_file_${System.currentTimeMillis()}"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val queried = it.getString(nameIndex)
                        if (!queried.isNullOrBlank()) {
                            name = queried
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return name
    }

    fun copyUriToFile(context: Context, uri: Uri, destDir: File): File? {
        return try {
            if (!destDir.exists()) destDir.mkdirs()
            val fileName = getFileNameFromUri(context, uri)
            val destFile = File(destDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun copyFileToUri(context: Context, srcFile: File, destUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destUri)?.use { output ->
                FileInputStream(srcFile).use { input ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
