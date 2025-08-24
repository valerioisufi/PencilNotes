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
    private val context: Context, var nomeFile: String // "fileExplorerXml.xml" - kept for compatibility but not used
) : ViewModel() {
    private val fileRepository = FileRepository(context)
    
    // Migration utility
    private val migration = XMLToRoomMigration(context, context.filesDir)

    /**
     * DATA
     */
    enum class FileType(val value: Int) {
        FILE(0), FOLDER(1)
    }

    data class Files(
        var type: FileType, 
        var name: MutableState<String> = mutableStateOf(""),
        var id: Int = 0 // Added to track database IDs
    )

    data class DirectoryFiles(var directoryPath: String) {
        var filesList = mutableStateListOf<Files>()
    }

    private var filesExplorer: MutableMap<String, DirectoryFiles>

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
        filesExplorer = mutableMapOf()
        
        // Perform migration if needed, then load current directory
        viewModelScope.launch {
            migration.migrateIfNeeded(nomeFile)
            loadCurrentDirectory()
        }
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
                
                filesExplorer[currentPath]!!.filesList.add(
                    Files(
                        type = fileType,
                        name = mutableStateOf(item.name),
                        id = item.id
                    )
                )
            }
        }
    }

    fun createFile(type: FileType, name: String): Boolean {
        if (existNameInDirectory(name = name)) {
            return false
        }
        
        viewModelScope.launch {
            val success = when (type) {
                FileType.FOLDER -> fileRepository.createFolder(name, currentFolderId)
                FileType.FILE -> fileRepository.createDocument(name, currentFolderId)
            }
            
            if (success) {
                loadCurrentDirectory() // Refresh the view
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

    fun moveFile(name: String, newDirectoryPath: String, oldDirectoryPath: String = currentDirectoryPath.value): Boolean {
        // TODO: Implement move functionality for database
        return false
    }
}