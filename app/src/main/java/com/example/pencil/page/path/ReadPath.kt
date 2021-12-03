package com.example.pencil.page.path

import android.graphics.Path


var digits = setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

// TODO: 01/12/2021 Si poteva anche semplicemente sostituire tutti i caratteri che non sono numeri o lettere
//  (quindi virgola, prima del meno, però problema con il punto) con uno spazio, e scomporre poi la stringa in una lista
fun stringToPath(pathString: String): Path {
    val realPath = Path()
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
                realPath.moveTo(x, y)

                istruzioneProvvisoria = 'L'
            }
            'm' -> {
                x = searchNumber()
                y = searchNumber()
                realPath.rMoveTo(x, y)

                istruzioneProvvisoria = 'l'
            }
            'L' -> {
                x = searchNumber()
                y = searchNumber()
                realPath.lineTo(x, y)

                istruzioneProvvisoria = 'L'
            }
            'l' -> {
                x = searchNumber()
                y = searchNumber()
                realPath.rLineTo(x, y)

                istruzioneProvvisoria = 'l'
            }
            'H' -> {
                x = searchNumber()
                realPath.lineTo(x, y)

                istruzioneProvvisoria = 'H'
            }
            'h' -> {
                x = searchNumber()
                realPath.rLineTo(x, y)

                istruzioneProvvisoria = 'h'
            }
            'V' -> {
                y = searchNumber()
                realPath.lineTo(x, y)

                istruzioneProvvisoria = 'V'
            }
            'v' -> {
                y = searchNumber()
                realPath.rLineTo(x, y)

                istruzioneProvvisoria = 'v'
            }
            'Q' -> {
                val x1 = searchNumber()
                val y1 = searchNumber()
                x = searchNumber()
                y = searchNumber()
                realPath.quadTo(x1, y1, x, y)

                istruzioneProvvisoria = 'Q'
            }
            'q' -> {
                val x1 = searchNumber()
                val y1 = searchNumber()
                x = searchNumber()
                y = searchNumber()
                realPath.rQuadTo(x1, y1, x, y)

                istruzioneProvvisoria = 'q'
            }
            'C' -> {
                val x1 = searchNumber()
                val y1 = searchNumber()
                val x2 = searchNumber()
                val y2 = searchNumber()
                x = searchNumber()
                y = searchNumber()
                realPath.cubicTo(x1, y1, x2, y2, x, y)

                istruzioneProvvisoria = 'C'
            }
            'c' -> {
                val x1 = searchNumber()
                val y1 = searchNumber()
                val x2 = searchNumber()
                val y2 = searchNumber()
                x = searchNumber()
                y = searchNumber()
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