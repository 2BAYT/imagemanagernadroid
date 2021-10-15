package com.twobayt.imagemanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageManagerTest_calculateAccordingToTargets {

    @Test
    fun checkTargetFunction() {
        val context: Context = ApplicationProvider.getApplicationContext()

        val height = 600
        val width = 700

        val manager = ImageManager.Builder(context)
            .crop(true)
            .targetHeight(height)
            .targetWidth(width)
            .build()


        //check height width is lower than target
        val testResult1 = manager.calculateAccordingToTargets(500.0f, 1000.0f)
        assertEquals(testResult1[1], height.toFloat())


        //check width,  height is lower than target
        val testResult2 = manager.calculateAccordingToTargets(1000.0f, 400.0f)
        assertEquals(testResult2[0], width.toFloat())


        val testResult3 = manager.calculateAccordingToTargets(2000.0f, 2000.0f)
        assertEquals(testResult3[0]==width.toFloat()  || testResult3[1]==height.toFloat() , true)

        //too small dimens nothing changed
        val testResult4 = manager.calculateAccordingToTargets(100.0f, 100.0f)
        assertEquals(testResult4[0]==100.0f && testResult4[1]==100.0f , true)

    }
}