package com.studiomath.pencilnotes.file

import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File

/**
 * Migration utility to convert existing XML-based file structure to Room database
 */
class XMLToRoomMigration(private val context: Context, private val filesDir: File) {
    private val fileRepository = FileRepository(context)

    suspend fun migrateIfNeeded(xmlFileName: String = "fileExplorerXml.xml") {
        val xmlFile = File(filesDir, xmlFileName)
        
        // Check if XML file exists and database is empty
        if (xmlFile.exists() && xmlFile.length() > 0L) {
            // Check if database is empty (no root folders)
            val rootFolders = fileRepository.getRootFolders()
            if (rootFolders.isEmpty()) {
                migrateFromXML(xmlFile)
            }
        }
    }

    private suspend fun migrateFromXML(xmlFile: File) {
        try {
            val inputStream = xmlFile.inputStream()
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            // Start parsing
            parser.nextTag()
            if (parser.name == "data") {
                migrateFolderStructure(parser, null)
            }
            
            inputStream.close()
            
            // Optionally backup the XML file
            val backupFile = File(xmlFile.parent, "${xmlFile.name}.backup")
            xmlFile.copyTo(backupFile, overwrite = true)
            
        } catch (e: Exception) {
            // Log error but don't crash the app
            e.printStackTrace()
        }
    }

    private suspend fun migrateFolderStructure(parser: XmlPullParser, parentId: Int?) {
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "folder" -> {
                        val folderName = parser.getAttributeValue(null, "nome")
                        if (folderName != null) {
                            // Create folder in database
                            val success = fileRepository.createFolder(folderName, parentId)
                            if (success) {
                                // Get the created folder to get its ID
                                val folders = if (parentId == null) {
                                    fileRepository.getRootFolders()
                                } else {
                                    fileRepository.getSubFolders(parentId)
                                }
                                val createdFolder = folders.find { it.name == folderName }
                                createdFolder?.let { folder ->
                                    // Recursively migrate subfolders and documents
                                    migrateFolderStructure(parser, folder.id)
                                }
                            }
                        }
                    }
                    "file" -> {
                        val fileName = parser.getAttributeValue(null, "nome")
                        if (fileName != null) {
                            // Create document in database
                            fileRepository.createDocument(fileName, parentId)
                        }
                    }
                }
            } else if (parser.eventType == XmlPullParser.END_TAG) {
                if (parser.name == "folder" || parser.name == "data") {
                    return // Exit this level of recursion
                }
            }
        }
    }
}