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

    fun createFile(type: FileType, name: String) {
        filesExplorer[currentDirectoryPath.value]!!.filesList.add(
            0, Files(type = type, name = mutableStateOf(name))
        )
        if (type == FileType.FOLDER) {
            filesExplorer["${currentDirectoryPath.value}$name/"] =
                DirectoryFiles("${currentDirectoryPath.value}$name/")
        }
        writeXML()
    }

    fun enterFolder(name: String) {
        directorySequence.add(name)
    }

    fun backFolder(): String? {
        return directorySequence.removeLastOrNull()
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