package com.leekleak.trafficlight.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class NetworkType {
    Cellular,
    Wifi
}

fun String.clipAndPad(length: Int): String {
    return if (this.length >= length) {
        this.substring(0, length)
    } else {
        this.padEnd(length, ' ')
    }
}

inline val Dp.px: Float
    @Composable get() = with(LocalDensity.current) { this@px.toPx() }

fun padHour(time: Int): String {
    if (time % 6 == 0) return time.toString().padStart(2, '0')
    return ""
}

@Composable
fun Int.toDp(): Dp {
    return (this / LocalDensity.current.density).dp
}