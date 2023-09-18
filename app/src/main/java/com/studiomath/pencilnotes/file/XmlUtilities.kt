package com.studiomath.pencilnotes.file

import android.graphics.RectF
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer

// TODO: 24/01/2022 qui inserirò le funzioni che si occupano della lettura e scrittura
//  di alcuni particolari frammenti di testo XML
fun readRectXML(parser: XmlPullParser): RectF {
    return RectF(
        parser.getAttributeValue(null, "left").toFloat(),
        parser.getAttributeValue(null, "top").toFloat(),
        parser.getAttributeValue(null, "right").toFloat(),
        parser.getAttributeValue(null, "bottom").toFloat()
    )
}

fun writeRectXML(serializer: XmlSerializer, rect: RectF){
    serializer.attribute(null, "left", rect.left.toString())
    serializer.attribute(null, "top", rect.top.toString())
    serializer.attribute(null, "right", rect.right.toString())
    serializer.attribute(null, "bottom", rect.bottom.toString())
}