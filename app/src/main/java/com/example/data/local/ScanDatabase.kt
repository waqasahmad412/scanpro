package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentPageEntity
import com.example.data.model.FolderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [DocumentEntity::class, DocumentPageEntity::class, FolderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ScanDatabase : RoomDatabase() {

    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile
        private var INSTANCE: ScanDatabase? = null

        fun getDatabase(context: Context): ScanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScanDatabase::class.java,
                    "scanpro_db"
                )
                .addCallback(DatabaseCallback(context.applicationContext))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.scanDao())
                    }
                }
            }

            private suspend fun populateInitialData(dao: ScanDao) {
                // Default Folders
                val folders = listOf(
                    FolderEntity("My Documents", "description", "#3B5BFE", docCount = 2, isDefault = true),
                    FolderEntity("Receipts", "receipt", "#FF6B35", docCount = 1, isDefault = false),
                    FolderEntity("ID Cards", "badge", "#10B981", docCount = 1, isDefault = false),
                    FolderEntity("Business Cards", "contact_page", "#8B5CF6", docCount = 1, isDefault = false),
                    FolderEntity("Notes", "notes", "#F59E0B", docCount = 1, isDefault = false)
                )
                dao.insertFolders(folders)

                val now = System.currentTimeMillis()
                val hour = 3600_000L

                // Sample Documents
                val doc1 = DocumentEntity(
                    id = "doc_1",
                    title = "Artisan Coffee Receipt",
                    folderName = "Receipts",
                    pageCount = 1,
                    createdAt = now - hour * 2,
                    modifiedAt = now - hour * 2,
                    isFavorite = true,
                    thumbnailUri = "android.resource://" + context.packageName + "/drawable/img_sample_receipt_1786037765469",
                    ocrText = """ARTISAN ROASTERS COFFEE
123 Main Street, Suite 400
Date: 08/06/2026   Time: 09:42 AM
Receipt #: 88492

1x Oat Milk Latte ........ $5.50
1x Avocado Toast ......... $12.00
1x Espresso Double ....... $3.80

Subtotal:                 $21.30
Tax (8.25%):              $1.76
TOTAL:                    $23.06

Paid via Apple Pay (**** 4912)
Thank you for visiting Artisan Roasters!""",
                    fileSizeKb = 840
                )

                val doc2 = DocumentEntity(
                    id = "doc_2",
                    title = "Driver License & ID",
                    folderName = "ID Cards",
                    pageCount = 1,
                    createdAt = now - hour * 24,
                    modifiedAt = now - hour * 24,
                    isFavorite = true,
                    thumbnailUri = "android.resource://" + context.packageName + "/drawable/img_sample_id_card_1786037776617",
                    ocrText = """STATE DRIVER LICENSE
DL No: D9842109
Name: ALEX M. MORGAN
DOB: 04/18/1992
Sex: M   Height: 5'-11"
EXP: 04/18/2028   ISS: 04/18/2022
Address: 742 Evergreen Terrace, Springfield, CA 90210
CLASS C - RESTRICTIONS: NONE""",
                    fileSizeKb = 1450
                )

                val doc3 = DocumentEntity(
                    id = "doc_3",
                    title = "Q3 Executive Proposal",
                    folderName = "My Documents",
                    pageCount = 3,
                    createdAt = now - hour * 48,
                    modifiedAt = now - hour * 5,
                    isFavorite = false,
                    thumbnailUri = "android.resource://" + context.packageName + "/drawable/img_sample_document_1786037785507",
                    ocrText = """EXECUTIVE SUMMARY & PROPOSAL
Q3 Strategic Growth Plan

1. Overview
This proposal outlines the deployment strategy for scaling document workflows across engineering, product design, and operations.

2. Key Performance Indicators
- Processing time reduction: 45%
- OCR accuracy target: 99.2%
- Cloud synchronization latency: < 300ms

3. Implementation Timeline
Milestone A: Architecture & Prototype - Month 1
Milestone B: Beta Testing & User Feedback - Month 2
Milestone C: Global Rollout - Month 3""",
                    fileSizeKb = 3120
                )

                val doc4 = DocumentEntity(
                    id = "doc_4",
                    title = "Tech Conf Business Card",
                    folderName = "Business Cards",
                    pageCount = 1,
                    createdAt = now - hour * 72,
                    modifiedAt = now - hour * 72,
                    thumbnailUri = "android.resource://" + context.packageName + "/drawable/img_sample_id_card_1786037776617",
                    ocrText = """SARA CHEN
Head of Product Architecture
Nexus Technologies Inc.
Email: sara.chen@nexustech.io
Phone: +1 (555) 019-2834
Web: www.nexustech.io
San Francisco, CA""",
                    fileSizeKb = 620
                )

                val doc5 = DocumentEntity(
                    id = "doc_5",
                    title = "Design Brainstorm Notes",
                    folderName = "Notes",
                    pageCount = 2,
                    createdAt = now - hour * 96,
                    modifiedAt = now - hour * 96,
                    thumbnailUri = "android.resource://" + context.packageName + "/drawable/img_sample_document_1786037785507",
                    ocrText = """DESIGN BRAINSTORMING SESSION
Topic: ScanPro Mobile UX

- Camera overlay with dynamic corner brackets
- Instant perspective warp & edge detection
- Single page & Batch multi-page capture mode
- Clean dark/light theme options with 16dp rounded cards
- Smart Gemini AI OCR extraction & multi-language translation""",
                    fileSizeKb = 1890
                )

                dao.insertDocument(doc1)
                dao.insertDocument(doc2)
                dao.insertDocument(doc3)
                dao.insertDocument(doc4)
                dao.insertDocument(doc5)

                // Pages for Doc 1
                dao.insertPage(
                    DocumentPageEntity(
                        pageId = "page_1_1",
                        documentId = "doc_1",
                        pageIndex = 0,
                        imageUri = doc1.thumbnailUri,
                        ocrText = doc1.ocrText
                    )
                )

                // Pages for Doc 2
                dao.insertPage(
                    DocumentPageEntity(
                        pageId = "page_2_1",
                        documentId = "doc_2",
                        pageIndex = 0,
                        imageUri = doc2.thumbnailUri,
                        ocrText = doc2.ocrText
                    )
                )

                // Pages for Doc 3 (3 pages)
                dao.insertPages(
                    listOf(
                        DocumentPageEntity("page_3_1", "doc_3", 0, doc3.thumbnailUri, ocrText = "Page 1: Executive Summary & Overview"),
                        DocumentPageEntity("page_3_2", "doc_3", 1, doc3.thumbnailUri, ocrText = "Page 2: Key Performance Indicators & Architecture"),
                        DocumentPageEntity("page_3_3", "doc_3", 2, doc3.thumbnailUri, ocrText = "Page 3: Implementation Timeline & Sign-off")
                    )
                )

                // Pages for Doc 4
                dao.insertPage(
                    DocumentPageEntity("page_4_1", "doc_4", 0, doc4.thumbnailUri, ocrText = doc4.ocrText)
                )

                // Pages for Doc 5
                dao.insertPages(
                    listOf(
                        DocumentPageEntity("page_5_1", "doc_5", 0, doc5.thumbnailUri, ocrText = doc5.ocrText),
                        DocumentPageEntity("page_5_2", "doc_5", 1, doc5.thumbnailUri, ocrText = "Page 2: Action Items & Follow-up Tasks")
                    )
                )
            }
        }
    }
}
