package com.studiomath.pencilnotes.file

import android.content.Context
import androidx.room.*

@Database(
    entities = [Folder::class, Document::class, Page::class, Resource::class],
    version = 1
)
abstract class DrawDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun documentDao(): DocumentDao
    abstract fun pageDao(): PageDao
    abstract fun resourceDao(): ResourceDao

    companion object {
        @Volatile
        private var INSTANCE: DrawDatabase? = null

        fun getInstance(context: Context): DrawDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DrawDatabase::class.java,
                    "draw_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}


/**
 * Entity
 */
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val parentId: Int? // Se null, è una cartella di primo livello
)

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val folderId: Int // A quale cartella appartiene
)

@Entity(
    tableName = "pages",
    indices = [Index(value = ["documentId", "pageNumber"])]
)
data class Page(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentId: Int, // A quale documento appartiene
    val pageNumber: Int, // Numero della pagina
    val content: String // Può essere un riferimento a una risorsa o testo
)

@Entity(tableName = "resources")
data class Resource(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentId: Int, // A quale documento appartiene
    val type: String, // "image", "pdf"
    val uri: String // Percorso del file
)


/**
 * Dao
 */
@Dao
interface FolderDao {
    @Insert
    suspend fun insert(folder: Folder)

    @Query("SELECT * FROM folders WHERE parentId IS NULL")
    suspend fun getRootFolders(): List<Folder>

    @Query("SELECT * FROM folders WHERE parentId = :parentId")
    suspend fun getSubFolders(parentId: Int): List<Folder>
}

@Dao
interface DocumentDao {
    @Insert
    suspend fun insert(document: Document)

    @Query("SELECT * FROM documents WHERE folderId = :folderId")
    suspend fun getDocumentsInFolder(folderId: Int): List<Document>
}

@Dao
interface PageDao {
    @Insert
    suspend fun insert(page: Page)

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageNumber")
    fun getPagesForDocument(documentId: Int): List<Page>

    @Query("UPDATE pages SET content = :content WHERE id = :pageId")
    suspend fun updatePageContent(pageId: Int, content: String)
}

@Dao
interface ResourceDao {
    @Insert
    suspend fun insert(resource: Resource)

    @Query("SELECT * FROM resources WHERE documentId = :documentId")
    suspend fun getResourcesForDocument(documentId: Int): List<Resource>
}