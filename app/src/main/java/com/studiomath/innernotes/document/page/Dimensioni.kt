package com.studiomath.innernotes.document.page

import android.graphics.RectF

/**
 * Classe per le funzioni che controllano le dimensioni della pagina
 */
class Dimensioni(widthTemp: Float, heightTemp: Float) {
    class Misura(grandezza: Float, type: Unit){
        enum class Unit{
            PIXEL, INCH, DOT, CM, MM
        }
        var inch = 0f
        var pt = 0f

        var cm = 0f
        var mm = 0f

        init {
            when(type){
                Unit.PIXEL -> {}
                Unit.INCH -> {}
                Unit.DOT -> {}
                Unit.CM -> {}
                Unit.MM -> {
                    mm = grandezza
                    cm = grandezza/10
                    inch = (grandezza/25.4).toFloat()
                    pt = (grandezza * 2.83465).toFloat()
                }
            }
        }
    }

    /**
     * Formati Pagina prestabiliti
     */
    companion object{
        enum class Orientamento{
            VERTICALE, ORIZZONTALE
        }

        fun A3(orientamento: Orientamento = Orientamento.VERTICALE): Dimensioni{
            return if (orientamento == Orientamento.VERTICALE)
                Dimensioni(297f, 420f)
            else
                Dimensioni(420f, 297f)
        }

        fun A4(orientamento: Orientamento = Orientamento.VERTICALE): Dimensioni{
            return if (orientamento == Orientamento.VERTICALE)
                Dimensioni(210f, 297f)
            else
                Dimensioni(297f, 210f)
        }

        fun A5(orientamento: Orientamento = Orientamento.VERTICALE): Dimensioni{
            return if (orientamento == Orientamento.VERTICALE)
                Dimensioni(148f, 210f)
            else
                Dimensioni(210f, 148f)
        }
    }

    var width = Misura(widthTemp, Misura.Unit.MM)
    var height = Misura(heightTemp, Misura.Unit.MM)
    //var rapportoDimensioni = width.mm / height.mm

    //var risoluzionePxInch = risoluzioneTemp
    /*var widthPx = risoluzionePxInch * width.inch
    var heightPx = risoluzionePxInch * height.inch*/

    /**
     * Funzioni per il calcolo delle dimensioni,
     * indipendenti o meno dalla risoluzione della pagina
     */
    fun calcHeightFromWidthPx(widthPx: Int): Float {
        val heightPx = (height.mm * widthPx) / width.mm
        return heightPx
    }
    fun calcWidthFromHeightPx(heightPx: Int): Float {
        val widthPx = (width.mm * heightPx) / height.mm
        return widthPx
    }

    // TODO: 15/11/2021 Considerare la possibiltà di utilizzare i valori in px come Float
    fun calcHeightFromRisoluzionePxInch(risoluzionePxInch: Int): Float {
        val heightPx = height.inch * risoluzionePxInch
        return heightPx
    }
    fun calcWidthFromRisoluzionePxInch(risoluzionePxInch: Int): Float {
        val widthPx = width.inch * risoluzionePxInch
        return widthPx
    }

    /**
     * Funzioni per il calcolo della dimensione del tratto
     * basate sulla dimensione in pt (dots) e su un fattore di scala
     */
    fun calcPxFromPt(dimPt: Float, widthPx: Int): Float {
        return (dimPt / width.pt) * widthPx
    }
    fun calcPxFromMm(dimMm: Float, widthPx: Int): Float {
        return (dimMm / width.mm) * widthPx
    }


    /**
     * Conversione da px (gradezza dipendente dallo schermo)
     * a pt (dots, grandezza indipendente dal dispositivo)
     */
    fun calcPtFromPx(dimPx: Float, widthPx: Int): Float {
        return width.pt * dimPx / widthPx
    }

    fun calcXPt(xPx: Float, rectPage: RectF): Float{
        return (xPx - rectPage.left) * width.pt / rectPage.width()
    }
    fun calcYPt(yPx: Float, rectPage: RectF): Float{
        return (yPx - rectPage.top) * height.pt / rectPage.height()
    }

    fun calcXPx(xPt: Float, rectPage: RectF): Float{
        return (xPt * rectPage.width() / width.pt) + rectPage.left
    }
    fun calcYPx(yPt: Float, rectPage: RectF): Float{
        return (yPt * rectPage.height() / height.pt) + rectPage.top
    }
}