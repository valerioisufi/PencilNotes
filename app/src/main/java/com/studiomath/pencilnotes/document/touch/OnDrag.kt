package com.studiomath.pencilnotes.document.touch

import android.content.ClipData
import android.graphics.RectF
import android.util.Log
import android.view.DragEvent
import android.widget.Toast
import androidx.core.app.ActivityCompat.requestDragAndDropPermissions
import com.studiomath.pencilnotes.file.DrawViewModel

class OnDrag(
    private var drawViewModel: DrawViewModel
) {
//    /**
//     * gestione del Drag and Drop
//     */
//    fun onDragEvent(event: DragEvent): Boolean {
//        when (event.action) {
//            DragEvent.ACTION_DRAG_STARTED -> {
//                // Determines if this View can accept the dragged data.
//
//                // Returns true to indicate that the View can accept the dragged data.
//                // Returns false to indicate that, during the current drag and drop operation,
//                // this View will not receive events again until ACTION_DRAG_ENDED is sent.
//                return event.clipDescription.hasMimeType("image/jpeg") || event.clipDescription.hasMimeType(
//                    "image/png"
//                )
//            }
//
//            DragEvent.ACTION_DRAG_ENTERED -> {
//                drawViewModel.draw(dragAndDrop = true)
//
//                // Returns true; the value is ignored.
//                return true
//            }
//
//            DragEvent.ACTION_DRAG_LOCATION -> {
//                drawViewModel.draw(dragAndDrop = true)
//
//                // Ignore the event.
//                return true
//            }
//
//            DragEvent.ACTION_DRAG_EXITED -> {
//                drawViewModel.draw()
//
//                // Returns true; the value is ignored.
//                return true
//            }
//
//            DragEvent.ACTION_DROP -> {
//                val imageItem: ClipData.Item = event.clipData.getItemAt(0)
//                val uri = imageItem.uri
//
//                // Request permission to access the image data being dragged into
//                // the target activity's ImageView element.
//                val dropPermissions = requestDragAndDropPermissions(context as Activity, event)
//
//                var estensione =
//                    if (event.clipDescription.hasMimeType("image/jpeg")) "jpg" else
//                        if (event.clipDescription.hasMimeType("image/png")) "png" else ""
//                val id = addRisorsa(cartellaFile, ".$estensione")
//                val inputStream = context.contentResolver.openInputStream(uri)
//                val outputFile = FileManager(context, "$id.$estensione", cartellaFile)
//                val outputStream = outputFile.file.outputStream()
//
//                val buffer = ByteArray(1024)
//                var n = 0
//                if (inputStream != null) {
//                    while (inputStream.read(buffer)
//                            .also { n = it } != -1
//                    ) outputStream.write(buffer, 0, n)
//                }
//
//                inputStream?.close()
//                outputStream.close()
//
//                drawFile.body[pageAttuale].images.add(
//                    GestionePagina.Image(
//                        GestionePagina.Image.TypeImage.valueOf(estensione.uppercase())
//                    ).apply {
//                        this.id = id
//                        this.rectPage = redrawPageRect
//                        this.rectVisualizzazione = RectF().apply {
//                            left = event.x - 200
//                            top = event.y - 200
//                            right = event.x + 200
//                            bottom = event.y + 200
//                        }
//                    }
//                )
//
//                // Release the permission immediately afterwards because it's
//                // no longer needed.
//                dropPermissions!!.release()
//
//                draw(redraw = true)
//                drawFile.writeXML()
//
//                // Returns true. DragEvent.getResult() will return true.
//                return true
//            }
//
//            DragEvent.ACTION_DRAG_ENDED -> {
//                // Does a getResult(), and displays what happened.
//                when (event.result) {
//                    true ->
//                        Toast.makeText(context, "The drop was handled.", Toast.LENGTH_LONG)
//
//                    else ->
//                        Toast.makeText(context, "The drop didn't work.", Toast.LENGTH_LONG)
//                }.show()
//
//                draw()
//                // Returns true; the value is ignored.
//                return true
//            }
//
//            else -> {
//                // An unknown action type was received.
//                Log.e("DragDrop Example", "Unknown action type received by View.OnDragListener.")
//                return false
//            }
//        }
//    }
}