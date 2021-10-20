package com.twobayt.imagemanager
import android.graphics.Bitmap

class RxEvent {
    data class EventImageSelected(val bitmap: Bitmap, val source: Source)
}