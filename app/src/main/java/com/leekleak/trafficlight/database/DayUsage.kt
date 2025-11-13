package com.leekleak.trafficlight.database

import android.net.TrafficStats
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Entity
data class DayUsage(
    @PrimaryKey val date: LocalDate = LocalDate.now(),
    val hours: MutableMap<Long, HourData> = mutableMapOf(),
    var totalWifi: Long = 0,
    var totalCellular: Long = 0,
) {
    fun categorizeUsage() {
        totalWifi = hours.map { it.value.wifi }.sum()
        totalCellular = hours.map { it.value.cellular }.sum()
    }
}

@Dao
interface DayUsageDao {
    @Query("Select * From DayUsage Where date = :date")
    fun getDayUsage(date: LocalDate): Flow<DayUsage?>

    @Query("Select * From DayUsage Where date = date('now', 'localtime')")
    fun getTodayUsage(): Flow<DayUsage?>

    @Query("SELECT EXISTS(SELECT * FROM DayUsage WHERE date = :date)")
    fun dayUsageExists(date: LocalDate): Boolean

    @Insert
    fun addDayUsage(dayUsage: DayUsage)

    @Update
    fun updateDayUsage(dayUsage: DayUsage)

    @Delete
    fun deleteDayUsage(dayUsage: DayUsage)

    @Query("SELECT COUNT(*) FROM DayUsage")
    fun getDBSize(): Flow<Int>

    @Query("DELETE FROM DayUsage")
    fun clear()

    @Query("SELECT MAX(totalWifi + totalCellular) FROM DayUsage")
    fun getMaxCombinedUsage(): Flow<Long>
}

@Serializable
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

    private fun setLastAsCurrent() {
        currentDown = lastDown
        currentUp = lastUp
        currentMobile = lastMobile
        currentWifi = lastWifi
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

    fun toHourUsage(): HourData {
        return HourData(
            upSpeed,
            downSpeed,
            wifiSpeed,
            mobileSpeed
        )
    }

    fun closeEnough(other: TrafficSnapshot?): Boolean {
        return other?.let {
            totalSpeed == it.totalSpeed ||
            (totalSpeed  in 1..1023 && it.totalSpeed in 1..1023)
        } ?: false
    }

    object Converters {
        @TypeConverter
        fun fromLocalDate(date: LocalDate?): String? {
            return date?.toString()
        }

        @TypeConverter
        fun toLocalDate(value: String?): LocalDate? {
            return if (value == null) null else LocalDate.parse(value)
        }

        @TypeConverter
        fun fromHoursMap(hours: MutableMap<Long, HourData>): String {
            return Json.encodeToString(hours)
        }

        @TypeConverter
        fun toHoursMap(hoursString: String): MutableMap<Long, HourData> {
            return Json.decodeFromString(hoursString)
        }
    }
}
