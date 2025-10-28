package com.leekleak.trafficlight.charts.model

data class BarData(
    val x: String,
    val y1: Double,
    val y2: Double
) {
    operator fun plus(other: BarData): BarData {
        return this.copy(
            y1 = this.y1 + other.y1,
            y2 = this.y2 + other.y2
        )
    }
}