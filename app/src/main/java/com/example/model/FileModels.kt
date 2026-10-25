package com.example.model

enum class SortOption(val title: String) {
    NAME_ASC("Nome (A-Z)"),
    NAME_DESC("Nome (Z-A)"),
    DATE_NEWEST("Mais recente"),
    DATE_OLDEST("Mais antigo"),
    SIZE_LARGEST("Maior tamanho"),
    SIZE_SMALLEST("Menor tamanho"),
    TYPE("Tipo de arquivo")
}

enum class ViewMode {
    LIST,
    GRID,
    COMPACT
}

enum class FileCategory(val title: String, val iconName: String) {
    ALL("Todos", "Folder"),
    IMAGES("Imagens", "Image"),
    VIDEOS("Vídeos", "VideoLibrary"),
    AUDIO("Áudios", "Audiotrack"),
    DOCUMENTS("Documentos", "Description"),
    APKS("APKs / Apps", "Android"),
    ARCHIVES("Arquivos ZIP", "Archive"),
    DOWNLOADS("Downloads", "Download"),
    RECENT("Recentes", "History"),
    LARGE("Arquivos Grandes", "SdStorage")
}

data class FileItem(
    val path: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long,
    val extension: String,
    val mimeType: String,
    val isHidden: Boolean,
    val childrenCount: Int = 0,
    val isFavorite: Boolean = false
) {
    val formattedSize: String
        get() {
            if (isDirectory) {
                return if (childrenCount >= 0) "$childrenCount itens" else "Pasta"
            }
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            if (digitGroups >= units.size) digitGroups = units.size - 1
            return String.format(java.util.Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }

    val formattedDate: String
        get() {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(lastModified))
        }
}

enum class ClipboardAction {
    COPY,
    CUT
}

data class ClipboardState(
    val items: List<FileItem> = emptyList(),
    val action: ClipboardAction = ClipboardAction.COPY
)

data class StorageStats(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val imagesBytes: Long = 0L,
    val videosBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val docsBytes: Long = 0L,
    val apksBytes: Long = 0L,
    val archivesBytes: Long = 0L,
    val otherBytes: Long = 0L
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) else 0f
}

enum class AppScreen {
    HOME,
    EXPLORER,
    DUAL_PANE,
    STORAGE_ANALYSIS,
    WIRELESS_TRANSFER
}

data class SystemShortcut(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconType: String,
    val badge: String? = null
)
