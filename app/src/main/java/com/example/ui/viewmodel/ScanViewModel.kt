package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ScanDatabase
import com.example.data.model.AppSettings
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentPageEntity
import com.example.data.model.ExportFormat
import com.example.data.model.FolderEntity
import com.example.data.model.ScanFilter
import com.example.data.model.ScanMode
import com.example.data.remote.GeminiClient
import com.example.data.repository.ScanRepository
import com.example.util.ScanImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class CapturedPage(
    val id: String = UUID.randomUUID().toString(),
    val imageUri: String,
    val originalImageUri: String = imageUri,
    val filterType: ScanFilter = ScanFilter.MAGIC_COLOR,
    val rotation: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val cornerTL: Pair<Float, Float> = 0.04f to 0.04f,
    val cornerTR: Pair<Float, Float> = 0.96f to 0.04f,
    val cornerBR: Pair<Float, Float> = 0.96f to 0.96f,
    val cornerBL: Pair<Float, Float> = 0.04f to 0.96f,
    val ocrText: String = ""
)

data class UiMessage(
    val message: String,
    val isError: Boolean = false
)

data class UserProfile(
    val name: String = "Guest User",
    val email: String = "guest@scanpro.app",
    val isLoggedIn: Boolean = false
)

class ScanViewModel(private val repository: ScanRepository) : ViewModel() {

    // Onboarding & Auth State
    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    val currentUser = MutableStateFlow(UserProfile())

    fun loginUser(name: String, email: String) {
        currentUser.value = UserProfile(
            name = name,
            email = email,
            isLoggedIn = true
        )
    }

    fun logoutUser() {
        currentUser.value = UserProfile(
            name = "Guest User",
            email = "guest@scanpro.app",
            isLoggedIn = false
        )
    }

    // Search Query & Selected Folder
    val searchQuery = MutableStateFlow("")
    val selectedFolderName = MutableStateFlow<String?>(null)

    // Layout Toggle (Grid vs List) for Folder View
    val isFolderGridMode = MutableStateFlow(true)

    // Multi-Select Mode
    val isMultiSelectMode = MutableStateFlow(false)
    val selectedDocIds = MutableStateFlow<Set<String>>(emptySet())

    // All Documents & Folders
    val allDocuments: StateFlow<List<DocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFolders: StateFlow<List<FolderEntity>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Documents
    val filteredDocuments: StateFlow<List<DocumentEntity>> = combine(
        allDocuments,
        searchQuery,
        selectedFolderName
    ) { docs, query, folder ->
        docs.filter { doc ->
            val matchesFolder = folder == null || doc.folderName.equals(folder, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    doc.title.contains(query, ignoreCase = true) ||
                    doc.ocrText.contains(query, ignoreCase = true) ||
                    doc.folderName.contains(query, ignoreCase = true)
            matchesFolder && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App Settings
    val settings: StateFlow<AppSettings> = repository.settings

    // Camera Scan Capture Session
    val scanMode = MutableStateFlow(ScanMode.SINGLE)
    val flashEnabled = MutableStateFlow(false)
    val gridEnabled = MutableStateFlow(true)
    val capturedPages = MutableStateFlow<List<CapturedPage>>(emptyList())
    val selectedCapturedPageIndex = MutableStateFlow(0)

    // Currently Opened Document Detail
    val activeDocumentId = MutableStateFlow<String?>(null)
    val activeDocument = MutableStateFlow<DocumentEntity?>(null)
    val activeDocumentPages = MutableStateFlow<List<DocumentPageEntity>>(emptyList())
    val activePageIndex = MutableStateFlow(0)

    // Active Edit / Crop Page
    val editingPage = MutableStateFlow<CapturedPage?>(null)

    // Toast / UI Messages
    private val _uiMessages = MutableSharedFlow<UiMessage>()
    val uiMessages: SharedFlow<UiMessage> = _uiMessages.asSharedFlow()

    // OCR & Translation State
    val isOcrLoading = MutableStateFlow(false)
    val currentOcrResult = MutableStateFlow("")
    val translatedText = MutableStateFlow("")
    val isTranslating = MutableStateFlow(false)

    // Watermark & Password Dialogs
    val showSaveModal = MutableStateFlow(false)
    val showSignatureModal = MutableStateFlow(false)
    val showPasswordModal = MutableStateFlow(false)
    val showTranslateModal = MutableStateFlow(false)

    // Global Sharing State
    val sharingDocument = MutableStateFlow<DocumentEntity?>(null)
    val sharingPageUris = MutableStateFlow<List<String>>(emptyList())

    fun prepareShareDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            sharingDocument.value = doc
            val pages = repository.getPagesListForDocument(doc.id)
            if (pages.isNotEmpty()) {
                sharingPageUris.value = pages.map { it.imageUri }
            } else if (doc.thumbnailUri.isNotBlank()) {
                sharingPageUris.value = listOf(doc.thumbnailUri)
            } else {
                sharingPageUris.value = emptyList()
            }
        }
    }

    fun clearShareState() {
        sharingDocument.value = null
        sharingPageUris.value = emptyList()
    }

    fun setOnboardingCompleted(completed: Boolean) {
        _hasCompletedOnboarding.value = completed
    }

    fun showToast(msg: String, isError: Boolean = false) {
        viewModelScope.launch {
            _uiMessages.emit(UiMessage(msg, isError))
        }
    }

    fun toggleFavorite(document: DocumentEntity) {
        viewModelScope.launch {
            repository.updateDocument(document.copy(isFavorite = !document.isFavorite))
            showToast(if (!document.isFavorite) "Added to Favorites" else "Removed from Favorites")
        }
    }

    // Camera Scan Actions
    fun capturePhoto(context: Context, imageUri: String) {
        val newPage = CapturedPage(
            imageUri = imageUri,
            originalImageUri = imageUri,
            ocrText = "Extracting document content..."
        )
        val updated = capturedPages.value + newPage
        capturedPages.value = updated
        selectedCapturedPageIndex.value = updated.size - 1

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = ScanImageProcessor.loadBitmapFromUri(context, imageUri)
                if (bitmap != null) {
                    val corners = ScanImageProcessor.detectDocumentCorners(bitmap)
                    val enhancedBmp = ScanImageProcessor.applyScanFilter(bitmap, ScanFilter.MAGIC_COLOR)
                    val processedUri = ScanImageProcessor.saveBitmapToCache(context, enhancedBmp, "enhanced_scan")
                    val text = GeminiClient.extractTextFromImage(context, imageUri)

                    withContext(Dispatchers.Main) {
                        val pagesNow = capturedPages.value.map { page ->
                            if (page.id == newPage.id) {
                                page.copy(
                                    imageUri = processedUri,
                                    cornerTL = corners.tl,
                                    cornerTR = corners.tr,
                                    cornerBR = corners.br,
                                    cornerBL = corners.bl,
                                    ocrText = text
                                )
                            } else page
                        }
                        capturedPages.value = pagesNow
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        showToast("Captured Page #${updated.size}")
    }

    fun removeCapturedPage(index: Int) {
        val list = capturedPages.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            capturedPages.value = list
            selectedCapturedPageIndex.value = (list.size - 1).coerceAtLeast(0)
            showToast("Page removed")
        }
    }

    fun clearScanSession() {
        capturedPages.value = emptyList()
        selectedCapturedPageIndex.value = 0
    }

    // Save & Export
    fun saveScanDocument(title: String, folderName: String, format: ExportFormat) {
        viewModelScope.launch {
            val pages = capturedPages.value
            if (pages.isEmpty()) {
                showToast("No pages to save", isError = true)
                return@launch
            }

            val docPages = pages.mapIndexed { idx, page ->
                DocumentPageEntity(
                    pageId = "page_${UUID.randomUUID().toString().take(6)}",
                    documentId = "",
                    pageIndex = idx,
                    imageUri = page.imageUri,
                    rotationAngle = page.rotation,
                    brightness = page.brightness,
                    contrast = page.contrast,
                    ocrText = page.ocrText,
                    filterType = page.filterType.name
                )
            }

            val savedId = repository.saveNewScanDocument(
                title = title.ifBlank { "Scanned Document" },
                folderName = folderName,
                pages = docPages,
                filterType = pages.first().filterType.name,
                isAutoEnhanced = true
            )

            clearScanSession()
            showSaveModal.value = false
            openDocumentDetail(savedId)
            showToast("Document saved as $format successfully!")
        }
    }

    fun openDocumentDetail(docId: String) {
        viewModelScope.launch {
            activeDocumentId.value = docId
            val doc = repository.getDocumentById(docId)
            activeDocument.value = doc
            if (doc != null) {
                currentOcrResult.value = doc.ocrText
                repository.getPagesForDocument(docId).collect { pages ->
                    activeDocumentPages.value = pages
                }
            }
        }
    }

    fun updatePageFilter(context: Context, pageIndex: Int, filter: ScanFilter) {
        val pages = capturedPages.value.toMutableList()
        if (pageIndex !in pages.indices) return

        val targetPage = pages[pageIndex]
        pages[pageIndex] = targetPage.copy(filterType = filter)
        capturedPages.value = pages

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseBitmap = ScanImageProcessor.loadBitmapFromUri(context, targetPage.originalImageUri) ?: return@launch
                val filteredBmp = ScanImageProcessor.applyScanFilter(
                    baseBitmap,
                    filter,
                    targetPage.brightness,
                    targetPage.contrast
                )
                val newUri = ScanImageProcessor.saveBitmapToCache(context, filteredBmp, "filter_${filter.name}")

                withContext(Dispatchers.Main) {
                    val currentPages = capturedPages.value.toMutableList()
                    if (pageIndex in currentPages.indices) {
                        currentPages[pageIndex] = currentPages[pageIndex].copy(
                            imageUri = newUri,
                            filterType = filter
                        )
                        capturedPages.value = currentPages
                        showToast("Filter Applied: ${filter.displayName}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updatePageEnhance(context: Context, pageIndex: Int, brightness: Float, contrast: Float) {
        val pages = capturedPages.value.toMutableList()
        if (pageIndex !in pages.indices) return

        val targetPage = pages[pageIndex]
        pages[pageIndex] = targetPage.copy(brightness = brightness, contrast = contrast)
        capturedPages.value = pages

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseBitmap = ScanImageProcessor.loadBitmapFromUri(context, targetPage.originalImageUri) ?: return@launch
                val enhancedBmp = ScanImageProcessor.applyScanFilter(
                    baseBitmap,
                    targetPage.filterType,
                    brightness,
                    contrast
                )
                val newUri = ScanImageProcessor.saveBitmapToCache(context, enhancedBmp, "enhance")

                withContext(Dispatchers.Main) {
                    val currentPages = capturedPages.value.toMutableList()
                    if (pageIndex in currentPages.indices) {
                        currentPages[pageIndex] = currentPages[pageIndex].copy(
                            imageUri = newUri,
                            brightness = brightness,
                            contrast = contrast
                        )
                        capturedPages.value = currentPages
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun rotatePage(pageIndex: Int) {
        val pages = capturedPages.value.toMutableList()
        if (pageIndex in pages.indices) {
            val currentRot = pages[pageIndex].rotation
            pages[pageIndex] = pages[pageIndex].copy(rotation = (currentRot + 90f) % 360f)
            capturedPages.value = pages
            showToast("Rotated 90°")
        }
    }

    fun cropPage(
        context: Context,
        pageIndex: Int,
        tlNorm: Pair<Float, Float>,
        trNorm: Pair<Float, Float>,
        brNorm: Pair<Float, Float>,
        blNorm: Pair<Float, Float>
    ) {
        val pages = capturedPages.value.toMutableList()
        if (pageIndex !in pages.indices) return

        val targetPage = pages[pageIndex]
        val sourceUri = targetPage.originalImageUri

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawBitmap = ScanImageProcessor.loadBitmapFromUri(context, sourceUri)
                if (rawBitmap == null) {
                    withContext(Dispatchers.Main) {
                        showToast("Could not load image for cropping", isError = true)
                    }
                    return@launch
                }

                val rotatedBitmap = if (targetPage.rotation != 0f) {
                    val matrix = Matrix()
                    matrix.postRotate(targetPage.rotation)
                    Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                } else {
                    rawBitmap
                }

                val croppedBitmap = ScanImageProcessor.perspectiveCrop(
                    rotatedBitmap,
                    tlNorm, trNorm, brNorm, blNorm
                )

                val finalScanBitmap = ScanImageProcessor.applyScanFilter(
                    croppedBitmap,
                    targetPage.filterType,
                    targetPage.brightness,
                    targetPage.contrast
                )

                val newUri = ScanImageProcessor.saveBitmapToCache(context, finalScanBitmap, "cropped_scan")

                withContext(Dispatchers.Main) {
                    val currentPages = capturedPages.value.toMutableList()
                    if (pageIndex in currentPages.indices) {
                        currentPages[pageIndex] = currentPages[pageIndex].copy(
                            imageUri = newUri,
                            originalImageUri = newUri,
                            rotation = 0f,
                            cornerTL = 0.04f to 0.04f,
                            cornerTR = 0.96f to 0.04f,
                            cornerBR = 0.96f to 0.96f,
                            cornerBL = 0.04f to 0.96f
                        )
                        capturedPages.value = currentPages
                        showToast("Document Cropped & Straightened")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showToast("Crop failed: ${e.localizedMessage}", isError = true)
                }
            }
        }
    }

    private fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
        return try {
            val uri = if (uriString.startsWith("/")) {
                Uri.fromFile(File(uriString))
            } else {
                Uri.parse(uriString)
            }
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    fun runOcrOnActiveDocument(context: Context) {
        viewModelScope.launch {
            isOcrLoading.value = true
            val doc = activeDocument.value
            val pages = activeDocumentPages.value
            val firstUri = pages.firstOrNull()?.imageUri ?: doc?.thumbnailUri ?: ""

            val result = GeminiClient.extractTextFromImage(context, firstUri)
            currentOcrResult.value = result
            isOcrLoading.value = false

            if (doc != null) {
                repository.updateDocument(doc.copy(ocrText = result))
            }
            showToast("OCR Text Extracted Successfully")
        }
    }

    fun translateOcrText(targetLang: String) {
        viewModelScope.launch {
            isTranslating.value = true
            val text = currentOcrResult.value
            val translated = GeminiClient.translateText(text, targetLang)
            translatedText.value = translated
            isTranslating.value = false
            showToast("Translated to $targetLang")
        }
    }

    // Multi-select & Batch Actions
    fun toggleSelectDoc(docId: String) {
        val set = selectedDocIds.value.toMutableSet()
        if (set.contains(docId)) {
            set.remove(docId)
        } else {
            set.add(docId)
        }
        selectedDocIds.value = set
        if (set.isEmpty()) {
            isMultiSelectMode.value = false
        }
    }

    fun deleteSelectedDocuments() {
        viewModelScope.launch {
            val docsToDelete = allDocuments.value.filter { selectedDocIds.value.contains(it.id) }
            repository.deleteDocuments(docsToDelete)
            selectedDocIds.value = emptySet()
            isMultiSelectMode.value = false
            showToast("Deleted ${docsToDelete.size} documents")
        }
    }

    fun mergeSelectedDocuments() {
        viewModelScope.launch {
            val docsToMerge = allDocuments.value.filter { selectedDocIds.value.contains(it.id) }
            if (docsToMerge.size < 2) {
                showToast("Select at least 2 documents to merge", isError = true)
                return@launch
            }

            val mergedTitle = "Merged Doc (${docsToMerge.size} files)"
            val allPages = mutableListOf<DocumentPageEntity>()
            docsToMerge.forEach { doc ->
                val pages = repository.getPagesListForDocument(doc.id)
                allPages.addAll(pages)
            }

            val savedId = repository.saveNewScanDocument(
                title = mergedTitle,
                folderName = "My Documents",
                pages = allPages,
                filterType = ScanFilter.MAGIC_COLOR.name,
                isAutoEnhanced = true
            )

            selectedDocIds.value = emptySet()
            isMultiSelectMode.value = false
            openDocumentDetail(savedId)
            showToast("Merged ${docsToMerge.size} documents into $mergedTitle")
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            if (activeDocumentId.value == doc.id) {
                activeDocumentId.value = null
                activeDocument.value = null
            }
            showToast("Document deleted")
        }
    }

    fun addFolder(name: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            repository.createFolder(name, iconName, colorHex)
            showToast("Folder '$name' created")
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        repository.updateSettings(newSettings)
        showToast("Settings updated")
    }
}

class ScanViewModelFactory(private val repository: ScanRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
