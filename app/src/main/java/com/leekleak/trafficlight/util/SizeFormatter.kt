package com.leekleak.trafficlight.util

import com.leekleak.trafficlight.model.PreferenceRepo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.ceil
import kotlin.math.pow

enum class DataSizeUnit {
    B, KB, MB, GB, TB, // Actual sizes
    PB, EB, ZB, YB  // Mental disorders
}

data class DataSize (
    var value: Float,
    var unit: DataSizeUnit = DataSizeUnit.B,
    val speed: Boolean = false,
    var precision: Int = 1
) {
    val precisionDec: Double
        get() = 10.0.pow(precision)

    init {
        var i = DataSizeUnit.entries.indexOf(unit)
        while (value >= 1000 && i < DataSizeUnit.entries.size) {
            value = if (value < 1024) 1f else value / 1024
            i++
        }
        unit = DataSizeUnit.entries[i]
    }

    private fun applyPrecision(size: Float): String {
        return if (precision == 0 || size.toInt() >= 100) size.toInt().toString()
            else ((size * precisionDec).toInt().toFloat() / precisionDec).toString() // Round down
    }

    fun getComparisonValue(): DataSize {
        if (value < 10) return copy(value = ceil(value), unit = unit, speed = speed, precision = precision)
        if (value < 100) return copy(value = ceil(value / 10f) * 10f, unit = unit, speed = speed, precision = precision)
        return copy(value = ceil(value / 100f) * 100f, unit = unit, speed = speed, precision = precision)
    }

    fun getBitValue(): Long {
        return (value * 1024f.pow(DataSizeUnit.entries.indexOf(unit))).toLong()
    }

    override fun toString(): String {
        val outValue = if (value < 1024 && unit == DataSizeUnit.B) {
            unit = DataSizeUnit.KB
             if (value > 0) "<1" else "0"
        } else applyPrecision(value)
        return "$outValue$unit${if (speed) "/s" else ""}"
    }

    fun toStringParts(): List<String> {
        return listOf(
            value.toInt().toString(),
            (value * precisionDec % precisionDec).toInt().toString(),
            unit.toString()
        )
    }
}

class SizeFormatter (
    private val speed: Boolean = false,
    private val precision: Int = 0
): KoinComponent {
    private val preferenceRepo: PreferenceRepo by inject()
    private val asBits = runBlocking { preferenceRepo.speedBits.first() }

    fun format(size: Number): String {
        val realSize = size.toFloat() * if (asBits && speed) 8f else 1f
        val dataSize = DataSize(realSize, DataSizeUnit.B, speed, precision)
        return "$dataSize".let { if (asBits) it.replace("B", "b") else it }
    }

    fun smartFormat(size: Number): String {
        val realSize = size.toFloat() * if (asBits && speed) 8f else 1f
        val dataSize = DataSize(realSize, DataSizeUnit.B, speed)
        dataSize.precision = if (dataSize.value < 10 && dataSize.unit >= DataSizeUnit.MB) 1 else 0
        return "$dataSize".let { if (asBits) it.replace("B", "b") else it }
    }
}