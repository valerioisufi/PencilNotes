package com.studiomath.pencilnotes

import android.graphics.Color
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.studiomath.pencilnotes.document.page.Page
import com.studiomath.pencilnotes.document.page.PageMaker
import com.studiomath.pencilnotes.document.page.Stroke
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawLogicTest {

    @Test
    fun verifyStrokeRendering() = runBlocking {
        // Context of the app under test.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val displayMetrics = context.resources.displayMetrics
        
        // 1. Setup PageMaker and Page
        val pageMaker = PageMaker(displayMetrics)
        
        // Create an A5-ish page (100mm x 100mm for simplicity)
        // Index 0
        val page = Page(index = 0).apply {
            width = 100f // mm
            height = 100f // mm
        }

        // 2. Add a diagonal stroke
        // We'll draw a thick red line from (10,10) to (90,90) in mm
        // We use PRESSURE_PEN for a solid stroke
        val stroke = Stroke(zIndex = 0).apply {
            color = Color.RED
            size = 5f // 5mm thick (very thick to be sure we hit it)
            brush = Stroke.BrushFamily.PRESSURE_PEN
            
            // Add inputs (diagonal)
            val start = 10f
            val end = 90f
            val count = 20
            
            for (i in 0..count) {
                val t = i / count.toFloat()
                val pos = start + (end - start) * t
                inputs.add(Stroke.StrokeInput(x = pos, y = pos).apply {
                    timeMillis = i * 10f
                    pressure = 1f // Full pressure
                })
            }
        }
        
        // Add stroke to page
        page.strokeData.add(stroke)
        
        // 3. Render Page
        // This calculates the Dimension and Bitmap size, and converts Stroke inputs to Ink Stroke
        page.prepare()
        
        val cachedBitmap = page.bitmapPage
        assertNotNull("Page bitmap should be created after prepare()", cachedBitmap)
        
        // Render using PageMaker to ensure the stroke is drawn onto the bitmap
        // In the app, PageMaker.makePage is used to render content.
        // We pass the cachedBitmap as both source and target implied by usage in PageComposable
        val resultBitmap = pageMaker.makePage(
            bitmapRect = Rect(0, 0, cachedBitmap!!.width, cachedBitmap.height),
            bitmapSource = cachedBitmap,
            page = page
        )
        
        // 4. Verify Pixels
        // Check center of the line. The line goes from (10,10) to (90,90) mm on a 100x100 mm page.
        // So (50,50) mm should be exactly on the line.
        // Map 50mm to pixels.
        val widthPx = resultBitmap.width
        val heightPx = resultBitmap.height
        
        // 50mm / 100mm = 0.5
        val centerX = (widthPx * 0.5).toInt()
        val centerY = (heightPx * 0.5).toInt()
        
        val centerPixel = resultBitmap.getPixel(centerX, centerY)
        
        val alpha = Color.alpha(centerPixel)
        val red = Color.red(centerPixel)
        val green = Color.green(centerPixel)
        val blue = Color.blue(centerPixel)

        // Debug output
        println("Center Pixel at ($centerX, $centerY): A=$alpha R=$red G=$green B=$blue")

        // Assertions
        // Expecting Red color. Alpha should be non-zero (solid pen).
        assertTrue("Center pixel should have alpha > 0", alpha > 0)
        assertTrue("Center pixel should be predominantly red", red > 200)
        assertTrue("Center pixel should have low green", green < 100)
        assertTrue("Center pixel should have low blue", blue < 100)
        
        // Check a corner (0,0) which should be empty (Transparent)
        // The stroke starts at 10mm, so 0,0 should be clear.
        val cornerPixel = resultBitmap.getPixel(0, 0)
        assertEquals("Corner pixel should be transparent", 0, cornerPixel)
    }
}
