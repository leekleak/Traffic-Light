package com.leekleak.trafficlight.database

import android.net.TrafficStats
import androidx.room.Dao
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
    val hours: MutableMap<Long, HourUsage> = mutableMapOf()
) {
    fun totalWifi(): Long = hours.values.sumOf { it.wifi }
    fun totalCellular(): Long = hours.values.sumOf { it.cellular }
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
}

@Serializable
data class HourUsage(
    var upload: Long = 0,
    var download: Long = 0,
    var wifi: Long = 0,
    var cellular: Long = 0
) {
    val total: Long
        get() = upload + download

    operator fun plus(other: HourUsage): HourUsage {
        return HourUsage(
            upload + other.upload,
            download + other.download,
            wifi + other.wifi,
            cellular + other.cellular)
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
        currentWifi = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes() - currentMobile

        // When switching networks the api sometimes fucks up the values as per
        // https://issuetracker.google.com/issues/37009612
        // Yes. That bug report is from 2014.
        // Yes. It still happens on my Android 16 device.

        // I think ignoring data until it fixes itself up is fine

        if (currentDown < lastDown || currentUp < lastUp || currentMobile < lastMobile || currentWifi < lastWifi) {
            setLastAsCurrent()
        }
    }

    fun toHourUsage(): HourUsage {
        return HourUsage(
            upSpeed,
            downSpeed,
            currentWifi - lastWifi,
            currentMobile - lastMobile
        )
    }

    fun equals(other: TrafficSnapshot?): Boolean {
        return other?.let {
            currentDown == other.currentDown &&
            currentUp == other.currentUp &&
            currentMobile == other.currentMobile &&
            currentWifi == other.currentWifi
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
        fun fromHoursMap(hours: MutableMap<Long, HourUsage>): String {
            return Json.encodeToString(hours)
        }

        @TypeConverter
        fun toHoursMap(hoursString: String): MutableMap<Long, HourUsage> {
            return Json.decodeFromString(hoursString)
        }
    }
}
