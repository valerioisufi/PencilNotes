package com.studiomath.pencilnotes.file

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FileRepository(context: Context) {
    private val database = DrawDatabase.getInstance(context)
    private val folderDao = database.folderDao()
    private val documentDao = database.documentDao()

    // Folder operations
    suspend fun createFolder(name: String, parentId: Int?): Boolean {
        // Check if folder already exists
        if (folderDao.getFolderByNameAndParent(name, parentId) != null) {
            return false
        }
        
        val folder = Folder(name = name, parentId = parentId)
        folderDao.insert(folder)
        return true
    }

    suspend fun getRootFolders(): List<Folder> {
        return folderDao.getRootFolders()
    }

    suspend fun getSubFolders(parentId: Int): List<Folder> {
        return folderDao.getSubFolders(parentId)
    }

    suspend fun renameFolder(folderId: Int, newName: String): Boolean {
        val folder = folderDao.getFolderById(folderId) ?: return false
        
        // Check if new name already exists in the same parent
        if (folderDao.getFolderByNameAndParent(newName, folder.parentId) != null) {
            return false
        }
        
        folderDao.renameFolder(folderId, newName)
        return true
    }

    suspend fun deleteFolder(folderId: Int): Boolean {
        val folder = folderDao.getFolderById(folderId) ?: return false
        
        // Delete all subfolders and documents recursively
        val subFolders = folderDao.getSubFolders(folderId)
        for (subFolder in subFolders) {
            deleteFolder(subFolder.id)
        }
        
        // Delete all documents in this folder
        val documents = documentDao.getDocumentsInFolder(folderId)
        for (document in documents) {
            deleteDocument(document.id)
        }
        
        // Delete the folder itself
        folderDao.deleteById(folderId)
        return true
    }

    // Document operations
    suspend fun createDocument(name: String, folderId: Int?): Boolean {
        // Check if document already exists
        val existingDocument = if (folderId == null) {
            documentDao.getRootDocumentByName(name)
        } else {
            documentDao.getDocumentByNameAndFolder(name, folderId)
        }
        
        if (existingDocument != null) {
            return false
        }
        
        val document = Document(name = name, folderId = folderId)
        documentDao.insert(document)
        return true
    }

    suspend fun getDocumentsInFolder(folderId: Int?): List<Document> {
        return if (folderId == null) {
            documentDao.getRootDocuments()
        } else {
            documentDao.getDocumentsInFolder(folderId)
        }
    }

    suspend fun renameDocument(documentId: Int, newName: String): Boolean {
        val document = documentDao.getDocumentById(documentId) ?: return false
        
        // Check if new name already exists in the same folder
        val existingDocument = if (document.folderId == null) {
            documentDao.getRootDocumentByName(newName)
        } else {
            documentDao.getDocumentByNameAndFolder(newName, document.folderId)
        }
        
        if (existingDocument != null) {
            return false
        }
        
        documentDao.renameDocument(documentId, newName)
        return true
    }

    suspend fun deleteDocument(documentId: Int): Boolean {
        documentDao.deleteById(documentId)
        return true
    }

    suspend fun moveDocument(documentId: Int, newFolderId: Int?): Boolean {
        val document = documentDao.getDocumentById(documentId) ?: return false
        
        // Check if document with same name already exists in target folder
        val existingDocument = if (newFolderId == null) {
            documentDao.getRootDocumentByName(document.name)
        } else {
            documentDao.getDocumentByNameAndFolder(document.name, newFolderId)
        }
        
        if (existingDocument != null) {
            return false
        }
        
        documentDao.moveDocument(documentId, newFolderId)
        return true
    }

    // Combined operations for UI
    data class FileItem(
        val id: Int,
        val name: String,
        val type: FileType,
        val parentId: Int?
    )

    enum class FileType {
        FOLDER, DOCUMENT
    }

    suspend fun getItemsInFolder(parentId: Int?): List<FileItem> {
        val items = mutableListOf<FileItem>()
        
        // Add folders
        val folders = if (parentId == null) {
            folderDao.getRootFolders()
        } else {
            folderDao.getSubFolders(parentId)
        }
        
        folders.forEach { folder ->
            items.add(FileItem(folder.id, folder.name, FileType.FOLDER, folder.parentId))
        }
        
        // Add documents
        val documents = getDocumentsInFolder(parentId)
        documents.forEach { document ->
            items.add(FileItem(document.id, document.name, FileType.DOCUMENT, parentId))
        }
        
        return items
    }

    suspend fun getFolderById(folderId: Int): Folder? {
        return folderDao.getFolderById(folderId)
    }

    suspend fun getDocumentById(documentId: Int): Document? {
        return documentDao.getDocumentById(documentId)
    }
}