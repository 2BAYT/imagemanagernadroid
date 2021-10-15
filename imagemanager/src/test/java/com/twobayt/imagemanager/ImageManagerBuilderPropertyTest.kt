package com.twobayt.imagemanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageManagerBuilderPropertyTest {

    @Test
    fun property_isCorrect() {

        val context:Context = ApplicationProvider.getApplicationContext()

        val crop = true
        val height = 600
        val width = 600

        val builder = ImageManager.Builder(context)
            .crop(crop)
            .targetHeight(height)
            .targetWidth(width)


        assertEquals(builder.debugLogEnabled, false)
        assertEquals(builder.fixExif, false)
        assertEquals(builder.sampleSize, SampleSize.NORMAL)

        assertEquals(builder.isCrop, crop)
        assertEquals(builder.targetHeight, height)
        assertEquals(builder.targetWidth, width)
        assertEquals(builder.targetWidth, width)

        builder.fixExif()
        assertEquals(builder.fixExif, true)

        builder.debugLogEnabled()
        assertEquals(builder.debugLogEnabled, true)

        builder.sampleSize(SampleSize.EXTRABIG)
        assertEquals(builder.sampleSize, SampleSize.EXTRABIG)

        builder.build()

    }

}