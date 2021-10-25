package com.twobayt.imagemanager

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageManagerTest_calculateAccordingToTargets {

    @Test
    fun checkTargetFunction() {
        val targetHeight = 600.0f
        val targetWidth = 700.0f

        //check height width is lower than target
        val testResult1 = BitmapUtils.calculateAccordingToTargets(width = 500.0f, height = 1000.0f, targetWidth, targetHeight)
        assertEquals(testResult1[1], targetHeight)


        //check width,  height is lower than target
        val testResult2 = BitmapUtils.calculateAccordingToTargets(1000.0f, 400.0f, targetWidth, targetHeight)
        assertEquals(testResult2[0], targetWidth)


        val testResult3 = BitmapUtils.calculateAccordingToTargets(2000.0f, 2000.0f, targetWidth, targetHeight)
        assertEquals(testResult3[0]== targetWidth || testResult3[1]== targetHeight, true)

        //too small dimens nothing changed
        val testResult4 = BitmapUtils.calculateAccordingToTargets(100.0f, 100.0f, targetWidth, targetHeight)
        assertEquals(testResult4[0]==100.0f && testResult4[1]==100.0f , true)

    }
}