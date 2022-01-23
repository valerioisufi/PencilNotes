package com.example.pencil.document.page

import android.graphics.*
import android.text.TextUtils
import com.example.pencil.document.path.stringToPath

var widthPagePredefinito = 210f
var heightPagePredefinito = 297f
var risoluzionePxInchPagePredefinito = 160

class GestionePagina(
    var widthMm: Float = widthPagePredefinito,
    var heightMm: Float = heightPagePredefinito,
    var index: Int = 0
) {
    class Tracciato(var type: TypeTracciato = TypeTracciato.PENNA) {
        enum class TypeTracciato {
            PENNA, EVIDENZIATORE
        }

        var pathString = ""
        var paintString = ""
        var rectString = ""

        var pathObject: Path? = null
        var paintObject: Paint? = null
        var rectObject: RectF? = null

        fun stringToObject() {
            if (pathObject == null) {
                pathObject = stringToPath(pathString)
            }
            if (paintObject == null) {
                paintObject = stringToPaint(paintString)
            }
            if (rectObject == null) {
                rectObject = stringToRect(rectString)
            }
        }

        fun objectToString() {
            if (paintObject != null) {
                paintString = paintToString(paintObject!!)
            }
            if (rectObject != null) {
                rectString = rectToString(rectObject!!)
            }
        }

        companion object {
            fun stringToPaint(paintS: String): Paint {
                val paintList = TextUtils.split(paintS, "#").toList()
                val paintTemp = Paint().apply {
                    color = paintList[0].toInt()
                    // Smooths out edges of what is drawn without affecting shape.
                    isAntiAlias = true
                    // Dithering affects how colors with higher-precision than the device are down-sampled.
                    isDither = true
                    style = when (paintList[1]) {
                        "STROKE" -> Paint.Style.STROKE
                        "FILL" -> Paint.Style.FILL
                        else -> Paint.Style.FILL_AND_STROKE
                    }
                    strokeJoin = Paint.Join.ROUND // default: MITER
                    strokeCap = Paint.Cap.ROUND // default: BUTT
                    strokeWidth = paintList[2].toFloat()
                }
                return paintTemp
            }

            fun stringToRect(rectS: String): RectF {
                val rectList = TextUtils.split(rectS, "#").toList()
                val rectTemp = RectF().apply {
                    left = rectList[0].toFloat()
                    top = rectList[1].toFloat()
                    right = rectList[2].toFloat()
                    bottom = rectList[3].toFloat()
                }
                return rectTemp
            }

            fun paintToString(paint: Paint): String {
                val paintS =
                    paint.color.toString() + "#" +
                            when (paint.style) {
                                Paint.Style.STROKE -> "STROKE"
                                Paint.Style.FILL -> "FILL"
                                Paint.Style.FILL_AND_STROKE -> "FILL_AND_STROKE"
                            } + "#" +
                            paint.strokeWidth.toString()
                return paintS
            }

            fun rectToString(rect: RectF): String {
                val rectS =
                    rect.left.toString() + "#" + rect.top.toString() + "#" + rect.right.toString() + "#" + rect.bottom.toString()
                return rectS
            }
        }
    }

    class Image(var type: TypeImage = TypeImage.PDF) {
        enum class TypeImage {
            PDF, PNG, JPG
        }

        var id = ""

        /**
         * rectVisualizzazione per determinare la posizione dell'immagine
         */
        var rectVisualizzazione = RectF()
        var rectRitaglio = RectF()

        /**
         * solo x Pdf
         */
        var index = 0

    }


    /**
     * Variabili che memorizzano le varie informazioni
     * presenti nella pagina
     */
    var dimensioni = Dimensioni(widthMm, heightMm)

    var background: Image? = null
    var tracciati = mutableListOf<Tracciato>()
    var images = mutableListOf<Image>()

    fun tracciatiStringToObject() {
        for (tracciato in tracciati) {
            tracciato.stringToObject()
        }
    }
    fun tracciatiObjectToString() {
        for (tracciato in tracciati) {
            tracciato.objectToString()
        }
    }

    /**
     * Variabili che memorizzano lo stato della
     * pagina nel drawView
     */
    var rectPage: RectF? = null
    var matrixPage: Matrix? = null

    /**
     * bitmapPage e canvasPage servono solo come cache da
     * utlizzare per esempio durante lo scaling o lo scorrimento
     * tra le pagine
     */
    var bitmapPage: Bitmap = Bitmap.createBitmap(
        dimensioni.calcWidthFromRisoluzionePxInch(risoluzionePxInchPagePredefinito).toInt(),
        dimensioni.calcHeightFromRisoluzionePxInch(risoluzionePxInchPagePredefinito).toInt(),
        Bitmap.Config.ARGB_8888
    )
    var canvasPage: Canvas = Canvas(bitmapPage)

}