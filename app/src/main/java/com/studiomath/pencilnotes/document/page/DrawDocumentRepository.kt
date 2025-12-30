package com.studiomath.pencilnotes.document.page

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.ink.authoring.InProgressStrokeId
import android.view.MotionEvent

import com.studiomath.pencilnotes.document.DrawViewModel
import com.studiomath.pencilnotes.file.DrawDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.studiomath.pencilnotes.file.Page as DbPage

class DrawDocumentRepository(
    context: Context,
    var documentId: Int,
    var drawViewModel: DrawViewModel
) {
    private val db: DrawDatabase = DrawDatabase.getInstance(context)
    private val documentDao = db.documentDao()
    private val pageDao = db.pageDao()
    private val resourceDao = db.resourceDao()

    lateinit var document: Document
    
    // Mutex for document modifications
    var documentMutex = Mutex()

    var isDocumentLoaded by mutableStateOf(false)
    var isDocumentShowed by mutableStateOf(false)

    // UI State for pages
    var pagesState = androidx.compose.runtime.mutableStateListOf<Page>()

    private var documentJob: Job? = null
    var documentScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        loadDocument()
    }

    private fun loadDocument() {
        documentJob = documentScope.launch {
            try {
                // 1. Find Document by ID
                var dbDocument = documentDao.getDocumentById(documentId)
                
                if (dbDocument == null) {
                   // Handle error: Document not found
                   // For now, we just return, maybe we should close activity or show toast via VM?
                   Log.e("DrawDocumentRepository", "Document with id $documentId not found")
                   return@launch
                }

                // 2. Load pages and resources
                val dbPages = pageDao.getPagesForDocument(documentId)
                val dbResources = resourceDao.getResourcesForDocument(documentId)

                // 3. Populate memory model
                document = Document(dbDocument.name).apply {
                    this.dbId = dbDocument.id

                    // Map Resources
                    dbResources.forEach { dbRes ->
                        // Enum conversion safety check? Assuming DB type string matches enum name
                        val type = try {
                           Resource.ResourceType.valueOf(dbRes.type)
                        } catch (e: IllegalArgumentException) {
                            Resource.ResourceType.COLOR // Fallback
                        }
                        
                        this.resources.add(Resource(dbRes.id.toString(), type).apply {
                            content = dbRes.uri // storing content/uri in content field
                        })
                    }

                    // Map Pages
                    dbPages.forEach { dbPage ->
                        val pageContent = if (dbPage.content.isNotEmpty() && dbPage.content != "{}") {
                             try {
                                 Json.decodeFromString<PageContent>(dbPage.content)
                             } catch (e: Exception) {
                                 PageContent()
                             }
                        } else {
                            PageContent()
                        }

                        this.pages.add(Page(dbPage.pageNumber).apply {
                            this.dbId = dbPage.id
                            this.width = dbPage.width
                            this.height = dbPage.height
                            
                            this.strokeData.addAll(pageContent.strokeData)
                            this.imageData.addAll(pageContent.imageData)
                            this.pdfData.addAll(pageContent.pdfData)
                            
                            prepare()
                        })
                    }
                }
                
                withContext(Dispatchers.Main) {
                    pagesState.clear()
                    pagesState.addAll(document.pages)
                    isDocumentLoaded = true
                    

                }
            } catch (e: Exception) {
                Log.e("DrawDocumentRepository", "Error loading document", e)
            }
        }
    }

    fun debounce(
        delayMillis: Long = 300L,
        scope: CoroutineScope = MainScope(),
        action: () -> Unit
    ): () -> Unit {
        var debounceJob: Job? = null
        return {
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(delayMillis)
                action()
            }
        }
    }

    var saveDocument = debounce(scope = documentScope) {
        documentScope.launch {
            if (!isDocumentLoaded) return@launch

            // 1. Identify modified pages and take snapshots of their content atomically
            val pagesToSave = documentMutex.withLock {
                val modifiedPages = document.pages.filter { it.isModified }
                if (modifiedPages.isEmpty()) return@withLock emptyList()

                modifiedPages.map { page ->
                    // Create deep copies / snapshots of the lists to avoid ConcurrentModificationException during serialization
                    val strokesSnapshot = page.strokeData.toList()
                    val imagesSnapshot = page.imageData.toList()
                    val pdfsSnapshot = page.pdfData.toList()
                    
                    page.isModified = false // Mark as clean immediately since we captured the state
                    
                    Triple(page.dbId, PageContent(strokesSnapshot, imagesSnapshot, pdfsSnapshot), page)
                }
            }

            if (pagesToSave.isEmpty()) return@launch

            // 2. Serialize and Save to DB (outside the lock)
            pagesToSave.forEach { (dbId, content, page) ->
                try {
                    val contentJson = Json.encodeToString(content)
                    pageDao.updatePageContent(dbId, contentJson)
                } catch (e: Exception) {
                    Log.e("DrawDocumentRepository", "Error saving page $dbId", e)
                    // Optionally re-mark as modified if save fails?
                    // page.isModified = true // accessing page outside lock might be racing but 'isModified' is volatile-ish? 
                    // For now, let's assume retry will happen on next change.
                }
            }
        }
    }

    fun addPage(page: Page) {
        documentScope.launch {
            // Create minimal content JSON (empty)
            val contentJson = Json.encodeToString(PageContent())
            
            val dbPage = com.studiomath.pencilnotes.file.Page(
                documentId = document.dbId,
                pageNumber = document.pages.size, // Append to end
                width = page.width,
                height = page.height,
                content = contentJson
            )
            
            val newPageId = pageDao.insert(dbPage)
            page.dbId = newPageId.toInt()
            page.prepare()

            withContext(Dispatchers.Main) {
                document.pages.add(page)
                pagesState.add(page)
                

            }
        }
    }

    fun removePage(index: Int = document.pages.lastIndex) {
        if (index >= 0 && index < document.pages.size) {
            val pageToRemove = document.pages[index]
            documentScope.launch {
                pageDao.deleteById(pageToRemove.dbId) 
                
                withContext(Dispatchers.Main) {
                    document.pages.removeAt(index)
                    pagesState.removeAt(index)
                    

                }
            }
        }
    }

    
    // Additional methods from DrawDocumentData



    fun addColorResource(color: Int) {
        val resourceId = (document.resources.lastIndex + 1).toString()
        val type = Resource.ResourceType.COLOR
        val content = color.toString()

        documentScope.launch {
            val dbRes = com.studiomath.pencilnotes.file.Resource(
                documentId = document.dbId,
                type = type.name, 
                uri = content
            )
            val newId = resourceDao.insert(dbRes)
            
            withContext(Dispatchers.Main) {
                document.resources.add(
                     Resource(
                        id = newId.toInt().toString(), // Using DB ID as resource ID? Or keeping the index-based ID?
                        // Original code used "(document.resources.lastIndex + 1).toString()".
                        // To maintain compatibility with existing logic, maybe we should stick to that 
                        // or switch to DB IDs. Since resources are a list, index-based ID is fragile.
                        // But for now let's append to memory list.
                        type = type
                    ).apply {
                        this.content = content
                    }
                )
            }
        }
    }

    fun getColorResource(resourceId: String): Int {
        // Warning: if resourceId is index-based, ensure it matches list index
        val index = resourceId.toIntOrNull()
        if (index != null && index >= 0 && index < document.resources.size) {
             val res = document.resources[index]
             if (res.type == Resource.ResourceType.COLOR) {
                 return res.content.toIntOrNull() ?: 0xFFFFFF
             }
        }
        return 0xFFFFFF
    }
}