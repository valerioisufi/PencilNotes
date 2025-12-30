package com.studiomath.pencilnotes.file

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File

class FileExplorerViewModel(
    context: Context
) : ViewModel() {
    private val fileRepository = FileRepository(context)

    fun openFile(id: Int) {
        viewModelScope.launch {
            fileRepository.updateLastOpened(id)
            loadRecentFiles() // Refresh recents
        }
    }

    /**
     * DATA
     */
    enum class FileType(val value: Int) {
        FILE(0), FOLDER(1)
    }

    data class Files(
        var type: FileType, 
        var name: MutableState<String> = mutableStateOf(""),
        var id: Int = 0, // Added to track database IDs
        val createdAt: Long = 0,
        val modifiedAt: Long = 0,
        val lastOpenedAt: Long? = null
    )

    enum class SortOption {
        NAME, DATE_CREATED, DATE_MODIFIED, LAST_OPENED
    }

    var sortOption = mutableStateOf(SortOption.NAME)
    var recentFiles = mutableStateListOf<Files>()


    data class DirectoryFiles(var directoryPath: String) {
        var filesList = mutableStateListOf<Files>()
    }

    private var filesExplorer: MutableMap<String, DirectoryFiles> = mutableMapOf()

    var directorySequence = mutableStateListOf<String>()
    private var directoryIdSequence = mutableStateListOf<Int?>() // Track folder IDs for database
    
    val currentDirectoryPath: MutableState<String>
        get() {
            var path = "/"
            for (item in directorySequence) {
                path += "$item/"
            }
            return mutableStateOf(path)
        }

    val currentDirectoryFiles: DirectoryFiles
        get() {
            return filesExplorer[currentDirectoryPath.value]!!
        }

    private val currentFolderId: Int?
        get() = directoryIdSequence.lastOrNull()

    init {
        loadCurrentDirectory()
        loadRecentFiles()
    }

    private fun loadRecentFiles() {
        viewModelScope.launch {
            recentFiles.clear()
            val recents = fileRepository.getRecentDocuments(10)
            recents.forEach { doc ->
                recentFiles.add(
                    Files(
                        type = FileType.FILE,
                        name = mutableStateOf(doc.name),
                        id = doc.id,
                        createdAt = doc.createdAt,
                        modifiedAt = doc.modifiedAt,
                        lastOpenedAt = doc.lastOpenedAt
                    )
                )
            }
        }
    }

    fun setSortOption(option: SortOption) {
        sortOption.value = option
        loadCurrentDirectory()
    }

    private fun loadCurrentDirectory() {
        viewModelScope.launch {
            val currentPath = currentDirectoryPath.value
            
            if (filesExplorer[currentPath] == null) {
                filesExplorer[currentPath] = DirectoryFiles(currentPath)
            }
            
            // Clear current items
            filesExplorer[currentPath]!!.filesList.clear()
            
            // Load items from database
            val items = fileRepository.getItemsInFolder(currentFolderId)
            
            for (item in items) {
                val fileType = when (item.type) {
                    FileRepository.FileType.FOLDER -> FileType.FOLDER
                    FileRepository.FileType.DOCUMENT -> FileType.FILE
                }
                
                val file = Files(
                        type = fileType,
                        name = mutableStateOf(item.name),
                        id = item.id,
                        createdAt = item.createdAt,
                        modifiedAt = item.modifiedAt,
                        lastOpenedAt = item.lastOpenedAt
                    )
                filesExplorer[currentPath]!!.filesList.add(file)
            }

            // Sort the list
            val sortedList = filesExplorer[currentPath]!!.filesList.sortedWith(
                when (sortOption.value) {
                    SortOption.NAME -> compareBy { it.name.value.lowercase() }
                    SortOption.DATE_CREATED -> compareByDescending { it.createdAt }
                    SortOption.DATE_MODIFIED -> compareByDescending { it.modifiedAt }
                    SortOption.LAST_OPENED -> compareByDescending { it.lastOpenedAt ?: 0L }
                }
            )
            
            // Re-populate with sorted items (keeping folders on top logic if desired, but for now simple sort)
            // Usually folders on top is preferred. Let's add that.
            val folders = sortedList.filter { it.type == FileType.FOLDER }
            val files = sortedList.filter { it.type == FileType.FILE }
            
            filesExplorer[currentPath]!!.filesList.clear()
            filesExplorer[currentPath]!!.filesList.addAll(folders)
            filesExplorer[currentPath]!!.filesList.addAll(files)

        }
    }

    fun validateFileName(name: String): String? {
        if (name.isBlank()) return "Name cannot be empty"
        if (name.contains("/") || name.contains("\\")) return "Name cannot contain slashes"
        if (existNameInDirectory(name = name)) return "Name already exists"
        return null
    }

    fun createFile(type: FileType, name: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}): Boolean {
        if (existNameInDirectory(name = name)) {
            onError("Name already exists")
            return false
        }
        
        val validationError = validateFileName(name)
        if (validationError != null) {
            onError(validationError)
            return false
        }
        
        viewModelScope.launch {
            val success = when (type) {
                FileType.FOLDER -> fileRepository.createFolder(name, currentFolderId)
                FileType.FILE -> fileRepository.createDocument(name, currentFolderId)
            }
            
            if (success) {
                loadCurrentDirectory() // Refresh the view
                onSuccess()
            } else {
                onError("Failed to create file")
            }
        }
        return true // Return true optimistically for UI responsiveness
    }

    fun enterFolder(name: String) {
        // Find the folder ID
        val folder = filesExplorer[currentDirectoryPath.value]?.filesList?.find { 
            it.name.value == name && it.type == FileType.FOLDER 
        }
        
        if (folder != null) {
            directorySequence.add(name)
            directoryIdSequence.add(folder.id)
            loadCurrentDirectory()
        }
    }

    suspend fun getSubFolders(parentId: Int?): List<FileRepository.FileItem> {
        val folders = fileRepository.getSubFolders(parentId ?: 0) // Assuming root is 0 or handled by repo if null
        // Fix: Repo getSubFolders expects Int (non-nullable) looking at previous definition? 
        // Let's check FileRepository.kt again. 
        // FileRepository.getSubFolders(parentId: Int) -> List<Folder>.
        // Root folders are getRootFolders().
        
        val folderList = if (parentId == null) {
            fileRepository.getRootFolders()
        } else {
            fileRepository.getSubFolders(parentId)
        }
        
        return folderList.map { folder ->
            FileRepository.FileItem(
                id = folder.id,
                name = folder.name,
                type = FileRepository.FileType.FOLDER,
                parentId = folder.parentId,
                createdAt = folder.createdAt,
                modifiedAt = folder.modifiedAt,
                lastOpenedAt = null
            )
        }
    }

    fun backFolder(): String? {
        val removed = directorySequence.removeLastOrNull()
        if (removed != null) {
            directoryIdSequence.removeLastOrNull()
            loadCurrentDirectory()
        }
        return removed
    }

    fun fileLocation(fileName: String, directoryPath: String = currentDirectoryPath.value): String {
        // For database-based system, we construct the path for compatibility
        return "/documenti/${directoryPath}${fileName}.json"
    }

    fun existNameInDirectory(directoryPath: String = currentDirectoryPath.value, name: String): Boolean {
        for (element in filesExplorer[directoryPath]?.filesList ?: emptyList()) {
            if (element.name.value == name) {
                return true
            }
        }
        return false
    }

    fun renameFile(oldName: String, newName: String, directoryPath: String = currentDirectoryPath.value): Boolean {
        val fileItem = filesExplorer[directoryPath]?.filesList?.find { 
            it.name.value == oldName 
        } ?: return false

        viewModelScope.launch {
            val success = when (fileItem.type) {
                FileType.FOLDER -> fileRepository.renameFolder(fileItem.id, newName)
                FileType.FILE -> fileRepository.renameDocument(fileItem.id, newName)
            }
            
            if (success) {
                fileItem.name.value = newName
                // Also handle any physical file renaming for compatibility
                val from = File(fileLocation(oldName, directoryPath))
                if (from.exists()) {
                    val to = File(fileLocation(newName, directoryPath))
                    from.renameTo(to)
                }
            }
        }
        return true // Return true optimistically for UI responsiveness
    }

    fun deleteFile(name: String, directoryPath: String = currentDirectoryPath.value): Boolean {
        val fileItem = filesExplorer[directoryPath]?.filesList?.find { 
            it.name.value == name 
        } ?: return false

        viewModelScope.launch {
            val success = when (fileItem.type) {
                FileType.FOLDER -> fileRepository.deleteFolder(fileItem.id)
                FileType.FILE -> fileRepository.deleteDocument(fileItem.id)
            }
            
            if (success) {
                // Remove from UI
                filesExplorer[directoryPath]?.filesList?.removeIf { it.name.value == name }
                
                // Also handle any physical file deletion for compatibility
                val fileToDelete = File(fileLocation(name, directoryPath))
                if (fileToDelete.exists()) {
                    if (fileToDelete.isDirectory) {
                        fileToDelete.deleteRecursively()
                    } else {
                        fileToDelete.delete()
                    }
                }
            }
        }
        return true // Return true optimistically for UI responsiveness
    }

    /**
     * MOVE & SELECTION
     */
    var selectionMode = mutableStateOf(false)
    var selectedItems = mutableStateListOf<Files>()

    fun toggleSelection(file: Files) {
        if (selectedItems.contains(file)) {
            selectedItems.remove(file)
            if (selectedItems.isEmpty()) {
                selectionMode.value = false
            }
        } else {
            selectedItems.add(file)
            selectionMode.value = true
        }
    }

    fun clearSelection() {
        selectedItems.clear()
        selectionMode.value = false
    }

    fun selectAll() {
        val currentList = currentDirectoryFiles.filesList
        selectedItems.clear()
        selectedItems.addAll(currentList)
        selectionMode.value = true
    }
    
    fun deleteSelected() {
        val itemsToDelete = selectedItems.toList() // Copy to avoid concurrent modification issues
        // Use a coroutine to delete all
        viewModelScope.launch {
             itemsToDelete.forEach { fileItem ->
                 val success = when (fileItem.type) {
                    FileType.FOLDER -> fileRepository.deleteFolder(fileItem.id)
                    FileType.FILE -> fileRepository.deleteDocument(fileItem.id)
                }
                if (success) {
                    filesExplorer[currentDirectoryPath.value]?.filesList?.remove(fileItem)
                }
             }
             clearSelection()
        }
    }

    fun moveFile(file: Files, targetFolderId: Int?) {
         viewModelScope.launch {
             val success = when (file.type) {
                 FileType.FOLDER -> fileRepository.moveFolder(file.id, targetFolderId)
                 FileType.FILE -> fileRepository.moveDocument(file.id, targetFolderId)
             }
             
             if (success) {
                 loadCurrentDirectory() // Reload to remove moved item
             }
         }
    }

    fun moveSelected(targetFolderId: Int?) {
        val itemsToMove = selectedItems.toList()
        viewModelScope.launch {
            itemsToMove.forEach { file ->
                val success = when (file.type) {
                    FileType.FOLDER -> fileRepository.moveFolder(file.id, targetFolderId)
                    FileType.FILE -> fileRepository.moveDocument(file.id, targetFolderId)
                }
            }
            clearSelection()
            loadCurrentDirectory()
        }
    }

    // Helper to check if a move is valid (e.g. not moving folder into itself)
    fun isValidMove(targetFolderId: Int?): Boolean {
        // Simple check: if we are moving selected folders, target cannot be one of them
        // A more complex check would be needed to ensure we don't move a folder into its own child
        // But for now, just checking "is target in selected items" is a good start.
        // Also check if target is current folder (pointless move)
        
        if (targetFolderId == currentFolderId) return false
        
        // Cannot move a folder into itself
        selectedItems.forEach { 
             if (it.type == FileType.FOLDER && it.id == targetFolderId) return false
        }
        
        return true
    }
}