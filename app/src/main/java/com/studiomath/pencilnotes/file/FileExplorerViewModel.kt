package com.studiomath.pencilnotes.file

import android.util.Xml
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer
import java.io.File

class FileExplorerViewModel(
    filesDir: File, var nomeFile: String // "fileExplorerXml.xml"
) : ViewModel() {
    private var fileManager: FileManager

    /**
     * DATA
     */
    enum class FileType(val value: Int) {
        FILE(0), FOLDER(1)
    }

    data class Files(
        var type: FileType, var name: MutableState<String> = mutableStateOf("")
    )

    data class DirectoryFiles(var directoryPath: String) {
        var filesList = mutableStateListOf<Files>()
    }

    private var filesExplorer: MutableMap<String, DirectoryFiles>

    var directorySequence = mutableStateListOf<String>()
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

    init {
        fileManager = FileManager(filesDir, nomeFile)
        filesExplorer = mutableMapOf()
        readXML()
    }

    fun createFile(type: FileType, name: String): Boolean {
        if (existNameInDirectory(name = name)) {
            return false
        }
        filesExplorer[currentDirectoryPath.value]!!.filesList.add(
            0, Files(type = type, name = mutableStateOf(name))
        )
        if (type == FileType.FOLDER) {
            filesExplorer["${currentDirectoryPath.value}$name/"] =
                DirectoryFiles("${currentDirectoryPath.value}$name/")
        }
        writeXML()
        return true
    }

    fun enterFolder(name: String) {
        directorySequence.add(name)
    }

    fun backFolder(): String? {
        return directorySequence.removeLastOrNull()
    }

    fun fileLocation(fileName: String, directoryPath: String = currentDirectoryPath.value): String {
        return "/documenti/${directoryPath}${fileName}.json"
    }

    fun existNameInDirectory(directoryPath: String = currentDirectoryPath.value, name: String): Boolean {
        for (element in filesExplorer[directoryPath]!!.filesList) {
            if (element.name.value == name) {
                return true
            }
        }
        return false
    }

    fun renameFile(oldName: String, newName: String, directoryPath: String = currentDirectoryPath.value): Boolean {
        if (existNameInDirectory(directoryPath = directoryPath, name = newName)) {
            return false
        }

        val from = File(fileLocation(oldName, directoryPath))

        if (from.exists()){
            val to = File(fileLocation(newName, directoryPath))
            from.renameTo(to)
        }

        for (element in filesExplorer[directoryPath]!!.filesList) {
            if (element.name.value == oldName) {
                element.name.value = newName
                if (element.type == FileType.FOLDER) {
                    filesExplorer["${directoryPath}${newName}/"]!!.filesList = filesExplorer["${directoryPath}${oldName}/"]!!.filesList
                    filesExplorer["${directoryPath}${newName}/"]!!.directoryPath = "${directoryPath}${newName}/"
                }
            }

        }
        writeXML()
        return true

    }

    fun deleteFile(name: String, directoryPath: String = currentDirectoryPath.value): Boolean {
        val fileToDelete = File(fileLocation(name, directoryPath))

        if (fileToDelete.exists()){
            if (fileToDelete.isDirectory) {
                fun deleteDirectory(directory: File) {
                    for (element in directory.listFiles()!!){
                        if (element.isDirectory) {
                            deleteDirectory(element)
                        }
                        element.delete()
                    }
                }
                deleteDirectory(fileToDelete)

            }
            fileToDelete.delete()
        }

        for (index in filesExplorer[directoryPath]!!.filesList.indices) {
            if (filesExplorer[directoryPath]!!.filesList[index].name.value == name) {

                if (filesExplorer[directoryPath]!!.filesList[index].type == FileType.FOLDER) {
                    filesExplorer.remove("${directoryPath}${name}/")
                }
                filesExplorer[directoryPath]!!.filesList.removeAt(index)
                break
            }

        }
        writeXML()
        return true
    }

    fun moveFile(name: String, newDirectoryPath: String, oldDirectoryPath: String = currentDirectoryPath.value): Boolean {
        TODO("Not yet implemented")
//        if (existNameInDirectory(directoryPath = newDirectoryPath, name = name)) {
//            return false
//        }
//        val fileToMove = File(fileLocation(name, oldDirectoryPath))
//        val newDirectory = File(fileLocation(name, newDirectoryPath))

        writeXML()
        return true
    }



    /**
     * Funzione per la lettura dei file XML
     */
    private fun readXML() {

        if (fileManager.file.length() == 0L) {
            writeXML()
        }

        val inputStream = fileManager.file.inputStream()

        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        dataReader(parser)
    }

    /**
     * Ogni volta che in una funzione raggiungo un start_tag
     * e quindi richiamo un'altra funzione, mi assicuro che nella funzione
     * chiamata raggiungo sempre l'end_tag corrispettivo
     */
    private fun dataReader(parser: XmlPullParser) {
        while (!(parser.depth == 1 && parser.eventType == XmlPullParser.END_TAG)) {
            // start_tag data e tag successivi
            parser.nextTag()

            fun funReader(parser: XmlPullParser, root: String) {
                val startDepht = parser.depth

                while (!(parser.depth == startDepht && parser.eventType == XmlPullParser.END_TAG)) {
                    parser.nextTag()

                    if (parser.name == "file" && parser.eventType == XmlPullParser.START_TAG) {
                        filesExplorer[root]!!.filesList.add(
                            Files(
                                type = FileType.FILE,
                                name = mutableStateOf(parser.getAttributeValue(null, "nome"))
                            )
                        )

                    } else if (parser.name == "folder" && parser.eventType == XmlPullParser.START_TAG) {
                        filesExplorer[root]!!.filesList.add(
                            Files(
                                type = FileType.FOLDER,
                                name = mutableStateOf(parser.getAttributeValue(null, "nome"))
                            )
                        )

                        filesExplorer["$root${parser.getAttributeValue(null, "nome")}/"] =
                            DirectoryFiles("$root${parser.getAttributeValue(null, "nome")}/")
                        funReader(parser, "$root${parser.getAttributeValue(null, "nome")}/")

                    }
                }
            }

            val root = "/"
            filesExplorer[root] = DirectoryFiles(root)
            funReader(parser, root)

        }
    }


    /**
     * Funzione per la scrittura dei file XML
     */
    private fun writeXML() {
        val outputStreamWriter = fileManager.file.writer()

        val serializer = Xml.newSerializer()
        serializer.setOutput(outputStreamWriter)

        //Start Document
        serializer.startDocument("UTF-8", true)
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
        serializer.startTag("", "data")

        fun funWriter(serializer: XmlSerializer, root: String) {

            for (element in filesExplorer[root]!!.filesList) {
                if (element.type == FileType.FILE) {
                    serializer.startTag("", "file")
                    serializer.attribute(null, "nome", element.name.value)
                    serializer.endTag("", "file")

                } else if (element.type == FileType.FOLDER) {
                    serializer.startTag("", "folder")
                    serializer.attribute(null, "nome", element.name.value)

                    funWriter(serializer, "$root${element.name.value}/")

                    serializer.endTag("", "folder")

                }

            }
        }

        if (filesExplorer.isNotEmpty()) {
            funWriter(serializer, "/")
        }

        //End tag <file>
        serializer.endTag("", "data")
        serializer.endDocument()
    }

}