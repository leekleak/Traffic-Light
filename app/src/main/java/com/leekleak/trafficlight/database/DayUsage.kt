package com.leekleak.trafficlight.database

import android.net.TrafficStats
import java.time.LocalDate

data class DayUsage(
    val date: LocalDate = LocalDate.now(),
    val hours: MutableMap<Long, HourData> = mutableMapOf(),
    var totalWifi: Long = 0,
    var totalCellular: Long = 0,
) {
    fun categorizeUsage() {
        totalWifi = hours.map { it.value.wifi }.sum()
        totalCellular = hours.map { it.value.cellular }.sum()
    }
}

data class HourData(
    var upload: Long = 0,
    var download: Long = 0,
    var wifi: Long = 0,
    var cellular: Long = 0
) {
    val total: Long
        get() = upload + download

    fun toHourUsage(): HourUsage {
        return HourUsage(0,
            wifi,
            cellular
        )
    }

    operator fun plus(other: HourData): HourData {
        return HourData(
            upload + other.upload,
            download + other.download,
            wifi + other.wifi,
            cellular + other.cellular
        )
    }
}

data class TrafficSnapshot (
    var lastDown: Long = 0,
    var lastUp: Long = 0,
    var lastMobile: Long = 0,
    var lastWifi: Long = 0,

    var currentDown: Long = 0,
    var currentUp: Long = 0,
    var currentMobile: Long = 0,
    var currentWifi: Long = 0,
) {
    val totalSpeed: Long
        get() = downSpeed + upSpeed

    val downSpeed: Long
        get() = currentDown - lastDown

    val upSpeed: Long
        get() = currentUp - lastUp

    val mobileSpeed: Long
        get() = currentMobile - lastMobile

    val wifiSpeed: Long
        get() = currentWifi - lastWifi

    private fun setCurrentAsLast() {
        lastDown = currentDown
        lastUp = currentUp
        lastMobile = currentMobile
        lastWifi = currentWifi
    }

    fun updateSnapshot() {
        setCurrentAsLast()
        currentDown = TrafficStats.getTotalRxBytes()
        currentUp = TrafficStats.getTotalTxBytes()
        currentMobile = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes()
        currentWifi = currentUp + currentDown - currentMobile

        // When switching networks the api sometimes fucks up the values as per
        // https://issuetracker.google.com/issues/37009612
        // Yes. That bug report is from 2014.
        // Yes. It still happens on my Android 16 device.

        // I think ignoring data until it fixes itself up is fine

        if (currentMobile < lastMobile || currentWifi < lastWifi) {
            setCurrentAsLast()
        }
    }

    fun closeEnough(other: TrafficSnapshot?): Boolean {
        return other?.let {
            totalSpeed == it.totalSpeed ||
            (totalSpeed  in 1..1023 && it.totalSpeed in 1..1023)
        } ?: false
    }
}
