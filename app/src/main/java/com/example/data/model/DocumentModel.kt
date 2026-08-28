package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

enum class ScanFilter(val displayName: String) {
    ORIGINAL("Original"),
    BW("B&W"),
    GRAYSCALE("Grayscale"),
    MAGIC_COLOR("Magic Color"),
    LIGHTEN("Lighten")
}

enum class ScanMode(val title: String) {
    AUTO("Auto"),
    SINGLE("Single Page"),
    BATCH("Batch"),
    ID_CARD("ID Card"),
    BOOK("Book")
}

enum class ExportFormat(val ext: String, val mimeType: String) {
    PDF("PDF", "application/pdf"),
    JPG("JPG", "image/jpeg"),
    PNG("PNG", "image/png")
}

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val folderName: String,
    val pageCount: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val passwordLock: String? = null,
    val watermarkText: String? = null,
    val ocrText: String = "",
    val thumbnailUri: String = "",
    val filterType: String = ScanFilter.MAGIC_COLOR.name,
    val isAutoEnhanced: Boolean = true,
    val fileSizeKb: Int = 1240
)

@Entity(tableName = "document_pages")
data class DocumentPageEntity(
    @PrimaryKey val pageId: String,
    val documentId: String,
    val pageIndex: Int,
    val imageUri: String,
    val rotationAngle: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val ocrText: String = "",
    val filterType: String = ScanFilter.MAGIC_COLOR.name,
    val cornerTLX: Float = 0.05f,
    val cornerTLY: Float = 0.05f,
    val cornerTRX: Float = 0.95f,
    val cornerTRY: Float = 0.05f,
    val cornerBRX: Float = 0.95f,
    val cornerBRY: Float = 0.95f,
    val cornerBLX: Float = 0.05f,
    val cornerBLY: Float = 0.95f
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val name: String,
    val iconName: String,
    val colorHex: String,
    val docCount: Int = 0,
    val isDefault: Boolean = false
)

data class AppSettings(
    val themeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM
    val defaultSaveFormat: ExportFormat = ExportFormat.PDF,
    val autoCloudSync: Boolean = true,
    val appLockPin: String? = null,
    val watermarkTemplate: String = "CONFIDENTIAL - SCANPRO",
    val storageUsedMb: Float = 142.8f,
    val totalStorageMb: Float = 15360f // 15 GB
)
