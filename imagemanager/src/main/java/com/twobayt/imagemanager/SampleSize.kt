package com.twobayt.imagemanager

import java.io.Serializable

enum class SampleSize(val value: Int) : Serializable {
    SMALL(500),
    NORMAL(700),
    BIG(1100),
    EXTRABIG(1400),
}