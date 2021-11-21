package com.example.pencil

/**
 * Classe per le funzioni che controllano le dimensioni della pagina
 */
class Dimensioni(widthTemp: Float, heightTemp: Float, risoluzioneTemp: Float) {
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
    fun calcSpessore(dimPt: Float, widthPx: Int): Int {
        val dimPx = (dimPt / width.pt) * widthPx
        return dimPx.toInt()

    }
}