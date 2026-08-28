package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentPageEntity
import com.example.data.model.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Query("SELECT * FROM documents ORDER BY modifiedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE folderName = :folderName ORDER BY modifiedAt DESC")
    fun getDocumentsByFolder(folderName: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :docId")
    suspend fun getDocumentById(docId: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :docId")
    suspend fun deleteDocumentById(docId: String)

    @Query("DELETE FROM documents WHERE id IN (:docIds)")
    suspend fun deleteDocumentsByIds(docIds: List<String>)

    @Query("SELECT * FROM document_pages WHERE documentId = :docId ORDER BY pageIndex ASC")
    fun getPagesForDocument(docId: String): Flow<List<DocumentPageEntity>>

    @Query("SELECT * FROM document_pages WHERE documentId = :docId ORDER BY pageIndex ASC")
    suspend fun getPagesListForDocument(docId: String): List<DocumentPageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: DocumentPageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<DocumentPageEntity>)

    @Update
    suspend fun updatePage(page: DocumentPageEntity)

    @Query("DELETE FROM document_pages WHERE pageId = :pageId")
    suspend fun deletePageById(pageId: String)

    @Query("DELETE FROM document_pages WHERE documentId = :docId")
    suspend fun deletePagesByDocId(docId: String)

    @Query("SELECT * FROM folders ORDER BY isDefault DESC, name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Query("UPDATE folders SET docCount = docCount + 1 WHERE name = :folderName")
    suspend fun incrementFolderDocCount(folderName: String)

    @Query("UPDATE folders SET docCount = CASE WHEN docCount > 0 THEN docCount - 1 ELSE 0 END WHERE name = :folderName")
    suspend fun decrementFolderDocCount(folderName: String)
}
