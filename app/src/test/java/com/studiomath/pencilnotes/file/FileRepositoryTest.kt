package com.studiomath.pencilnotes.file

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class FileRepositoryTest {
    
    private lateinit var database: DrawDatabase
    private lateinit var fileRepository: FileRepository
    
    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DrawDatabase::class.java
        ).allowMainThreadQueries().build()
        
        fileRepository = FileRepository(ApplicationProvider.getApplicationContext())
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun testCreateRootFolder() = runBlocking {
        // Test creating a folder in root
        val success = fileRepository.createFolder("Test Folder", null)
        assertTrue("Should create root folder successfully", success)
        
        // Verify folder was created
        val rootFolders = fileRepository.getRootFolders()
        assertEquals("Should have one root folder", 1, rootFolders.size)
        assertEquals("Folder name should match", "Test Folder", rootFolders[0].name)
        assertNull("Root folder should have null parent", rootFolders[0].parentId)
    }
    
    @Test
    fun testCreateDuplicateFolder() = runBlocking {
        // Create first folder
        fileRepository.createFolder("Duplicate", null)
        
        // Try to create duplicate
        val success = fileRepository.createFolder("Duplicate", null)
        assertFalse("Should not allow duplicate folder names", success)
        
        // Verify only one folder exists
        val rootFolders = fileRepository.getRootFolders()
        assertEquals("Should have only one folder", 1, rootFolders.size)
    }
    
    @Test
    fun testCreateSubFolder() = runBlocking {
        // Create parent folder
        fileRepository.createFolder("Parent", null)
        val parentFolder = fileRepository.getRootFolders()[0]
        
        // Create subfolder
        val success = fileRepository.createFolder("Child", parentFolder.id)
        assertTrue("Should create subfolder successfully", success)
        
        // Verify subfolder was created
        val subFolders = fileRepository.getSubFolders(parentFolder.id)
        assertEquals("Should have one subfolder", 1, subFolders.size)
        assertEquals("Subfolder name should match", "Child", subFolders[0].name)
        assertEquals("Subfolder should have correct parent", parentFolder.id, subFolders[0].parentId)
    }
    
    @Test
    fun testCreateRootDocument() = runBlocking {
        // Test creating a document in root
        val success = fileRepository.createDocument("Test Document", null)
        assertTrue("Should create root document successfully", success)
        
        // Verify document was created
        val rootDocuments = fileRepository.getDocumentsInFolder(null)
        assertEquals("Should have one root document", 1, rootDocuments.size)
        assertEquals("Document name should match", "Test Document", rootDocuments[0].name)
        assertNull("Root document should have null folderId", rootDocuments[0].folderId)
    }
    
    @Test
    fun testCreateDocumentInFolder() = runBlocking {
        // Create folder first
        fileRepository.createFolder("Documents", null)
        val folder = fileRepository.getRootFolders()[0]
        
        // Create document in folder
        val success = fileRepository.createDocument("Test Doc", folder.id)
        assertTrue("Should create document in folder successfully", success)
        
        // Verify document was created
        val documents = fileRepository.getDocumentsInFolder(folder.id)
        assertEquals("Should have one document in folder", 1, documents.size)
        assertEquals("Document name should match", "Test Doc", documents[0].name)
        assertEquals("Document should be in correct folder", folder.id, documents[0].folderId)
    }
    
    @Test
    fun testGetItemsInFolder() = runBlocking {
        // Create mixed content in root
        fileRepository.createFolder("Root Folder", null)
        fileRepository.createDocument("Root Document", null)
        
        // Get items in root
        val items = fileRepository.getItemsInFolder(null)
        assertEquals("Should have 2 items in root", 2, items.size)
        
        // Verify types
        val folder = items.find { it.type == FileRepository.FileType.FOLDER }
        val document = items.find { it.type == FileRepository.FileType.DOCUMENT }
        
        assertNotNull("Should have a folder", folder)
        assertNotNull("Should have a document", document)
        assertEquals("Folder name should match", "Root Folder", folder?.name)
        assertEquals("Document name should match", "Root Document", document?.name)
    }
    
    @Test
    fun testRenameFolder() = runBlocking {
        // Create and rename folder
        fileRepository.createFolder("Old Name", null)
        val folder = fileRepository.getRootFolders()[0]
        
        val success = fileRepository.renameFolder(folder.id, "New Name")
        assertTrue("Should rename folder successfully", success)
        
        // Verify rename
        val updatedFolder = fileRepository.getFolderById(folder.id)
        assertEquals("Folder name should be updated", "New Name", updatedFolder?.name)
    }
    
    @Test
    fun testDeleteFolder() = runBlocking {
        // Create folder
        fileRepository.createFolder("To Delete", null)
        val folder = fileRepository.getRootFolders()[0]
        
        // Delete folder
        val success = fileRepository.deleteFolder(folder.id)
        assertTrue("Should delete folder successfully", success)
        
        // Verify deletion
        val rootFolders = fileRepository.getRootFolders()
        assertEquals("Should have no root folders", 0, rootFolders.size)
    }
    
    @Test
    fun testDeleteFolderWithContents() = runBlocking {
        // Create folder with contents
        fileRepository.createFolder("Parent", null)
        val parent = fileRepository.getRootFolders()[0]
        
        fileRepository.createFolder("Child", parent.id)
        fileRepository.createDocument("Doc", parent.id)
        
        // Delete parent folder
        val success = fileRepository.deleteFolder(parent.id)
        assertTrue("Should delete folder with contents successfully", success)
        
        // Verify all items are deleted
        val rootFolders = fileRepository.getRootFolders()
        val rootDocuments = fileRepository.getDocumentsInFolder(null)
        assertEquals("Should have no root folders", 0, rootFolders.size)
        assertEquals("Should have no root documents", 0, rootDocuments.size)
    }
}