package com.studiomath.pencilnotes.file

import android.content.Context
import androidx.room.*

@Database(
    entities = [Folder::class, Document::class, Page::class, Resource::class],
    version = 2
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
                ).fallbackToDestructiveMigration() // For development, we can use destructive migration
                .build()
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
    val folderId: Int? = null // A quale cartella appartiene - null per documenti root
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
    suspend fun insert(folder: Folder): Long

    @Update
    suspend fun update(folder: Folder)

    @Delete
    suspend fun delete(folder: Folder)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteById(folderId: Int)

    @Query("SELECT * FROM folders WHERE parentId IS NULL")
    suspend fun getRootFolders(): List<Folder>

    @Query("SELECT * FROM folders WHERE parentId = :parentId")
    suspend fun getSubFolders(parentId: Int): List<Folder>

    @Query("SELECT * FROM folders WHERE id = :folderId")
    suspend fun getFolderById(folderId: Int): Folder?

    @Query("SELECT * FROM folders WHERE name = :name AND parentId = :parentId")
    suspend fun getFolderByNameAndParent(name: String, parentId: Int?): Folder?

    @Query("UPDATE folders SET name = :newName WHERE id = :folderId")
    suspend fun renameFolder(folderId: Int, newName: String)
}

@Dao
interface DocumentDao {
    @Insert
    suspend fun insert(document: Document): Long

    @Update
    suspend fun update(document: Document)

    @Delete
    suspend fun delete(document: Document)

    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun deleteById(documentId: Int)

    @Query("SELECT * FROM documents WHERE folderId = :folderId")
    suspend fun getDocumentsInFolder(folderId: Int): List<Document>

    @Query("SELECT * FROM documents WHERE folderId IS NULL")
    suspend fun getRootDocuments(): List<Document>

    @Query("SELECT * FROM documents WHERE id = :documentId")
    suspend fun getDocumentById(documentId: Int): Document?

    @Query("SELECT * FROM documents WHERE name = :name AND folderId = :folderId")
    suspend fun getDocumentByNameAndFolder(name: String, folderId: Int): Document?

    @Query("SELECT * FROM documents WHERE name = :name AND folderId IS NULL")
    suspend fun getRootDocumentByName(name: String): Document?

    @Query("UPDATE documents SET name = :newName WHERE id = :documentId")
    suspend fun renameDocument(documentId: Int, newName: String)

    @Query("UPDATE documents SET folderId = :newFolderId WHERE id = :documentId")
    suspend fun moveDocument(documentId: Int, newFolderId: Int?)
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