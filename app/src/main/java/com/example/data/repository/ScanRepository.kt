package com.example.data.repository

import com.example.data.local.ScanDao
import com.example.data.model.AppSettings
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentPageEntity
import com.example.data.model.FolderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ScanRepository(private val scanDao: ScanDao) {

    val allDocuments: Flow<List<DocumentEntity>> = scanDao.getAllDocuments()
    val allFolders: Flow<List<FolderEntity>> = scanDao.getAllFolders()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun getDocumentsByFolder(folderName: String): Flow<List<DocumentEntity>> {
        return scanDao.getDocumentsByFolder(folderName)
    }

    suspend fun getDocumentById(id: String): DocumentEntity? {
        return scanDao.getDocumentById(id)
    }

    fun getPagesForDocument(docId: String): Flow<List<DocumentPageEntity>> {
        return scanDao.getPagesForDocument(docId)
    }

    suspend fun getPagesListForDocument(docId: String): List<DocumentPageEntity> {
        return scanDao.getPagesListForDocument(docId)
    }

    suspend fun saveNewScanDocument(
        title: String,
        folderName: String,
        pages: List<DocumentPageEntity>,
        filterType: String,
        isAutoEnhanced: Boolean
    ): String {
        val docId = "doc_" + UUID.randomUUID().toString().take(8)
        val now = System.currentTimeMillis()
        val firstPageImage = pages.firstOrNull()?.imageUri ?: ""
        val combinedOcr = pages.joinToString("\n\n--- Page Break ---\n\n") { it.ocrText }

        val newDoc = DocumentEntity(
            id = docId,
            title = title,
            folderName = folderName,
            pageCount = pages.size,
            createdAt = now,
            modifiedAt = now,
            thumbnailUri = firstPageImage,
            ocrText = combinedOcr,
            filterType = filterType,
            isAutoEnhanced = isAutoEnhanced,
            fileSizeKb = pages.size * 950
        )

        scanDao.insertDocument(newDoc)

        val indexedPages = pages.mapIndexed { index, page ->
            page.copy(
                pageId = "page_" + UUID.randomUUID().toString().take(8),
                documentId = docId,
                pageIndex = index
            )
        }
        scanDao.insertPages(indexedPages)
        scanDao.incrementFolderDocCount(folderName)

        return docId
    }

    suspend fun updateDocument(document: DocumentEntity) {
        scanDao.updateDocument(document)
    }

    suspend fun updatePage(page: DocumentPageEntity) {
        scanDao.updatePage(page)
    }

    suspend fun deleteDocument(document: DocumentEntity) {
        scanDao.deleteDocumentById(document.id)
        scanDao.deletePagesByDocId(document.id)
        scanDao.decrementFolderDocCount(document.folderName)
    }

    suspend fun deleteDocuments(documents: List<DocumentEntity>) {
        val ids = documents.map { it.id }
        scanDao.deleteDocumentsByIds(ids)
        documents.forEach { doc ->
            scanDao.deletePagesByDocId(doc.id)
            scanDao.decrementFolderDocCount(doc.folderName)
        }
    }

    suspend fun createFolder(name: String, iconName: String, colorHex: String) {
        scanDao.insertFolder(
            FolderEntity(
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                docCount = 0
            )
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
    }
}
