package com.leekleak.trafficlight.database

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Context.NETWORK_STATS_SERVICE
import android.util.Log
import com.leekleak.trafficlight.util.NetworkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HourlyUsageRepo(context: Context) {
    private val dao = HourlyUsageDatabase.getInstance(context).hourlyUsageDao()
    private var networkStatsManager: NetworkStatsManager? = null

    init {
        networkStatsManager = context.getSystemService(NETWORK_STATS_SERVICE) as NetworkStatsManager?
    }

    fun getDBSize(): Flow<Int> = dao.getDBSize()

    fun getUsage(startStamp: Long, endStamp: Long): Flow<List<HourUsage>> =
        dao.getUsage(startStamp, endStamp)

    fun getLastDayWithData(): Flow<LocalDate> = dao.getLastUsage().map { hourUsage ->
        Instant.ofEpochMilli(hourUsage?.timestamp ?: 0L)
            .atZone(ZoneId.systemDefault().rules.getOffset(Instant.now()))
            .toLocalDate()
    }

    fun getMaxCombinedUsage(): Flow<Long> = dao.getMaxCombinedUsage()

    fun clearDB() = dao.clear()

    fun populateDb() {
        val suspiciousHours = mutableListOf<HourUsage>()
        val timezone = ZoneId.systemDefault().rules.getOffset(Instant.now())
        val date = LocalDate.now().atStartOfDay()
        val dayStamp = date.truncatedTo(ChronoUnit.DAYS).toInstant(timezone).toEpochMilli()

        for (i in 1..10000) {
            if (suspiciousHours.size == 31 * 24) {
                suspiciousHours.forEach {
                    dao.deleteHourUsage(it)
                }
                Log.e("leekleak", "Reached maximum amount of empty shit")
                return
            }

            val hour = 3_600_000L
            val currentStamp = dayStamp - (i * hour)
            val hourUsage = getCurrentHourUsage(currentStamp, currentStamp + hour)

            if (dao.hourUsageExists(currentStamp)) return
            suspiciousHours.add(HourUsage(currentStamp,hourUsage.wifi, hourUsage.cellular))
            if (hourUsage.total != 0L) {
                for (hour in suspiciousHours) {
                    dao.addHourUsage(hour)
                }
                suspiciousHours.clear()
            }
        }
    }

    fun calculateDayUsage(date: LocalDate): DayUsage = calculateDayUsage(date.atStartOfDay())
    fun calculateDayUsage(dateTime: LocalDateTime): DayUsage {
        val timezone = ZoneId.systemDefault().rules.getOffset(Instant.now())
        val dayStamp = dateTime.truncatedTo(ChronoUnit.DAYS).toInstant(timezone).toEpochMilli()
        val hours: MutableMap<Long, HourData> = mutableMapOf()

        for (k in 0..23) {
            val globalHour = dayStamp + k * 3_600_000L
            hours[globalHour] = getCurrentHourUsage(globalHour, globalHour + 3_600_000L)
        }

        return DayUsage(dateTime.toLocalDate(), hours).also { it.categorizeUsage() }
    }

    fun getCurrentHourUsage(startTime: Long, endTime: Long): HourData {
        val statsWifi = networkStatsManager?.querySummaryForDevice(NetworkType.Wifi.ordinal, null, startTime, endTime)
        val statsMobile = networkStatsManager?.querySummaryForDevice(NetworkType.Cellular.ordinal, null, startTime, endTime)

        val hourData = HourData()
        statsMobile?.let {
            hourData.cellular += it.txBytes + it.rxBytes
            hourData.upload += it.txBytes
            hourData.download += it.rxBytes
        }

        statsWifi?.let {
            hourData.wifi += it.txBytes + it.rxBytes
            hourData.upload += it.txBytes
            hourData.download += it.rxBytes
        }
        return hourData
    }
}