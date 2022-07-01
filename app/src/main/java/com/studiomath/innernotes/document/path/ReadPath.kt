package com.studiomath.innernotes.document.path

import android.graphics.Path
import com.studiomath.innernotes.document.DrawView


var digits = setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

// TODO: 01/12/2021 Si poteva anche semplicemente sostituire tutti i caratteri che non sono numeri o lettere
//  (quindi virgola, prima del meno, però problema con il punto) con uno spazio, e scomporre poi la stringa in una lista
fun stringToPath(pathString: String, /*drawView: DrawView? = null*/): Path {
    val realPath = Path()
    var pos = 0

    fun searchNumber(isX: Boolean = false, isY: Boolean = false): Float{
        var number = ""
        var firstPunto = true

        pos++
        while (true){
            if(pos > pathString.lastIndex){
                break
            }
            if (pathString[pos] in digits){
                number += pathString[pos]
                pos++
            } else if(pathString[pos] == '.'){
                if(firstPunto){
                    number += "."
                    pos++
                    firstPunto = false
                } else{
                    break
                }
            } else if(pathString[pos] == ' '){
                if(number == ""){
                    pos++
                } else{
                    break
                }
            } else if(pathString[pos] == '-'){
                if(number == ""){
                    number += "-"
                    pos++
                } else{
                    break
                }
            } else{
                break
            }
        }

        if (number == ""){
            return 0f
        }

//        if (drawView != null){
//            if (isX) {
//                return drawView.drawFile.body[drawView.pageAttuale].dimensioni.calcXPx(
//                    number.toFloat(),
//                    drawView.redrawPageRect
//                )
//            } else if (isY){
//                return drawView.drawFile.body[drawView.pageAttuale].dimensioni.calcYPx(
//                    number.toFloat(),
//                    drawView.redrawPageRect
//                )
//            }
//
//        }
        return number.toFloat()
    }



    var istruzioneProvvisoria = 'L'
    var x = 0f
    var y = 0f

    while(pos <= pathString.lastIndex){
        var istruzione = pathString[pos]
        if (istruzione in digits){
            istruzione = istruzioneProvvisoria
            pos--
        }

        when(istruzione){
            'M' -> {
                x = searchNumber(isX = true)
                y = searchNumber(isY = true)
                realPath.moveTo(x, y)

                istruzioneProvvisoria = 'L'
            }
            'm' -> {
                x = searchNumber(isX = true)
                y = searchNumber(isY = true)
                realPath.rMoveTo(x, y)

                istruzioneProvvisoria = 'l'
            }
            'L' -> {
                x = searchNumber(isX = true)
                y = searchNumber(isY = true)
                realPath.lineTo(x, y)

                istruzioneProvvisoria = 'L'
            }
            'l' -> {
                x = searchNumber(isX = true)
                y = searchNumber(isY = true)
                realPath.rLineTo(x, y)

                istruzioneProvvisoria = 'l'
            }
            'H' -> {
                x = searchNumber(isX = true)
                realPath.lineTo(x, y)

                istruzioneProvvisoria = 'H'
            }
            'h' -> {
                x = searchNumber(isX = true)
                realPath.rLineTo(x, y)

                istruzioneProvvisoria = 'h'
            }
            'V' -> {
                y = searchNumber(isY = true)
                realPath.lineTo(x, y)

                istruzioneProvvisoria = 'V'
            }
            'v' -> {
                y = searchNumber(isY = true)
                realPath.rLineTo(x, y)

                istruzioneProvvisoria = 'v'
            }
            'Q' -> {
                val x1 = searchNumber(isX = true)
                val y1 = searchNumber(isY = true)
                x = searchNumber(isX = true)
                y = searchNumber(isY = true)
                realPath.quadTo(x1, y1, x, y)

                istruzioneProvvisoria = 'Q'
            }
            'q' -> {
                val x1 = searchNumber(isX = true)
                val y1 = searchNumber(isY = true)
                x = searchNumber(isX = true)
                y = searchNumber(isY = true)
                realPath.rQuadTo(x1, y1, x, y)

                istruzioneProvvisoria = 'q'
            }
            'C' -> {
                val x1 = searchNumber(isX = true)
                val y1 = searchNumber(isY = true)
                val x2 = searchNumber(isX = true)
                val y2 = searchNumber(isY = true)
                x = searchNumber(isX = true)
                y = searchNumber(isY = true)
                realPath.cubicTo(x1, y1, x2, y2, x, y)

                istruzioneProvvisoria = 'C'
            }
            'c' -> {
                val x1 = searchNumber(isX = true)
                val y1 = searchNumber(isY = true)
                val x2 = searchNumber(isX = true)
                val y2 = searchNumber(isY = true)
                x = searchNumber(isX = true)
                y = searchNumber(isY = true)
                realPath.rCubicTo(x1, y1, x2, y2, x, y)

                istruzioneProvvisoria = 'c'
            }
            'Z', 'z' -> {
                realPath.close()
                //istruzioneProvvisoria = 'L'
            }

            else -> pos++
        }
    }

    return realPath
}



/**
 * Funzione che trasforma una stringa contenente un tracciato
 * in una lista Kotlin
 */
fun stringToList(pathString: String): MutableList<MutableMap<String, String>> {
    val pathList = mutableListOf<MutableMap<String,String>>()
    var pos = 0

    fun searchNumber(): Float{
        var number = ""
        var firstPunto = true

        pos++
        while (true){
            if(pos > pathString.lastIndex){
                break
            }
            if (pathString[pos] in digits){
                number += pathString[pos]
                pos++
            } else if(pathString[pos] == '.'){
                if(firstPunto){
                    number += "."
                    pos++
                    firstPunto = false
                } else{
                    break
                }
            } else if(pathString[pos] == ' '){
                if(number == ""){
                    pos++
                } else{
                    break
                }
            } else if(pathString[pos] == '-'){
                if(number == ""){
                    number += "-"
                    pos++
                } else{
                    break
                }
            } else{
                break
            }
        }

        if (number == ""){
            return 0f
        }

        //Log.d(TAG, "searchNumber: " + number)
        return number.toFloat()
    }



    var istruzioneProvvisoria = 'L'
    var x = 0f
    var y = 0f

    while(pos <= pathString.lastIndex){
        var istruzione = pathString[pos]
        if (istruzione in digits){
            istruzione = istruzioneProvvisoria
            pos--
        }

        when(istruzione){
            'M' -> {
                x = searchNumber()
                y = searchNumber()

                pathList.add(
                    mutableMapOf(
                        Pair("type", "M"),
                        Pair("x", x.toString()),
                        Pair("y", y.toString())
                    )
                )

                istruzioneProvvisoria = 'L'
            }
            'm' -> {
                x = searchNumber()
                y = searchNumber()

                pathList.add(
                    mutableMapOf(
                        Pair("type", "m"),
                        Pair("x", x.toString()),
                        Pair("y", y.toString())
                    )
                )

                istruzioneProvvisoria = 'l'
            }
            'L' -> {
                x = searchNumber()
                y = searchNumber()

                pathList.add(
                    mutableMapOf(
                        Pair("type", "L"),
                        Pair("x", x.toString()),
                        Pair("y", y.toString())
                    )
                )

                istruzioneProvvisoria = 'L'
            }
            'l' -> {
                x = searchNumber()
                y = searchNumber()

                pathList.add(
                    mutableMapOf(
                        Pair("type", "l"),
                        Pair("x", x.toString()),
                        Pair("y", y.toString())
                    )
                )

                istruzioneProvvisoria = 'l'
            }
            'H' -> {
                x = searchNumber()

                pathList.add(
                    mutableMapOf(
                        Pair("type", "H"),
                        Pair("x", x.toString()),
                        Pair("y", y.toString())
                    )
                )

                istruzioneProvvisoria = 'H'
            }
            'h' -> {
                x = searchNumber()

                pathList.add(
                    mutableMapOf(
                        Pair("type", "h"),
                        Pair("x", x.toString()),
                        Pair("y", y.toString())
                    )
                )

                istruzioneProvvisoria = 'h'
            }
            'V' -> {
                y = searchNumber()

                pathList.add(
                    mutableMapOf(
                        Pair("type", "V"),
                        Pair("x", x.toString()),
                        Pair("y", y.toString())
                    )
                )

                istruzioneProvvisoria = 'V'
            }
            'v' -> {
                y = searchNumber()

                pathList.add(
                    mutableMapOf(
                        Pair("type", "v"),
                        Pair("x", x.toString()),
                        Pair("y", y.toString())
                    )
                )

                istruzioneProvvisoria = 'v'
            }
            'Q' -> {
                val x1 = searchNumber()
                val y1 = searchNumber()
                x = searchNumber()
                y = searchNumber()

                pathList.add(
                    mutableMapOf(
                        Pair("type", "Q"),
                        Pair("x", x.toString()),
                        Pair("y", y.toString()),
                        Pair("x1", x1.toString()),
                        Pair("y1", y1.toString())
                    )
                )

                istruzioneProvvisoria = 'Q'
            }
            'q' -> {
                val x1 = searchNumber()
                val y1 = searchNumber()
                x = searchNumber()
                y = searchNumber()

                pathList.add(
                    mutableMapOf(
                        Pair("type", "q"),
                        Pair("x", x.toString()),
                        Pair("y", y.toString()),
                        Pair("x1", x1.toString()),
                        Pair("y1", y1.toString())
                    )
                )

                istruzioneProvvisoria = 'q'
            }
            'C' -> {
                val x1 = searchNumber()
                val y1 = searchNumber()
                val x2 = searchNumber()
                val y2 = searchNumber()
                x = searchNumber()
                y = searchNumber()

                pathList.add(
                    mutableMapOf(
                        Pair("type", "C"),
                        Pair("x", x.toString()),
                        Pair("y", y.toString()),
                        Pair("x1", x1.toString()),
                        Pair("y1", y1.toString()),
                        Pair("x2", x2.toString()),
                        Pair("y2", y2.toString())
                    )
                )

                istruzioneProvvisoria = 'C'
            }
            'c' -> {
                val x1 = searchNumber()
                val y1 = searchNumber()
                val x2 = searchNumber()
                val y2 = searchNumber()
                x = searchNumber()
                y = searchNumber()

                pathList.add(
                    mutableMapOf(
                        Pair("type", "c"),
                        Pair("x", x.toString()),
                        Pair("y", y.toString()),
                        Pair("x1", x1.toString()),
                        Pair("y1", y1.toString()),
                        Pair("x2", x2.toString()),
                        Pair("y2", y2.toString())
                    )
                )

                istruzioneProvvisoria = 'c'
            }
            'Z', 'z' -> {
                pathList.add(
                    mutableMapOf(
                        Pair("type", "Z")
                    )
                )

                //istruzioneProvvisoria = 'L'
            }

            else -> pos++
        }
    }

    return pathList
}