package com.leekleak.trafficlight.database

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Context.NETWORK_STATS_SERVICE
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.leekleak.trafficlight.util.NetworkType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class DayUsageRepo(context: Context) {
    private val dao = DayUsageDatabase.getInstance(context).dayUsageDao()
    private var networkStatsManager: NetworkStatsManager? = null

    init {
        networkStatsManager = context.getSystemService(NETWORK_STATS_SERVICE) as NetworkStatsManager?
    }

    fun getDBSize(): Flow<Int> = dao.getDBSize()

    fun getDayUsage(date: LocalDate): Flow<DayUsage?> = dao.getDayUsage(date)
    fun getTodayUsage(): Flow<DayUsage?> = dao.getTodayUsage()

    fun updateDayUsage(dayUsage: DayUsage) = dao.updateDayUsage(dayUsage)

    fun dayUsageExists(date: LocalDate) = dao.dayUsageExists(date)

    fun addDayUsage(dayUsage: DayUsage) = dao.addDayUsage(dayUsage)

    fun getAllDayUsagePaged(pageSize: Int = 20): Flow<PagingData<DayUsage>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { dao.getAllDayUsagePaging() }
        ).flow
    }

    fun getMaxCombinedUsage(): Flow<Long> = dao.getMaxCombinedUsage()

    fun clearDB() = dao.clear()

    fun populateDb() {
        val suspiciousDays = mutableListOf<DayUsage>()
        for (i in 1..10000) {
            if (suspiciousDays.size == 31) {
                suspiciousDays.forEach {
                    dao.deleteDayUsage(it)
                }
                return
            }

            val date = LocalDate.now().minusDays(i.toLong()).atStartOfDay()
            if (dao.dayUsageExists(date.toLocalDate())) return

            val dayUsage = calculateDayUsage(date)

            suspiciousDays.add(dayUsage)
            if (dayUsage.totalWifi + dayUsage.totalCellular != 0L) {
                for (day in suspiciousDays) {
                    dao.addDayUsage(day)
                }
                suspiciousDays.clear()
            }
        }
    }

    fun calculateDayUsage(date: LocalDate): DayUsage = calculateDayUsage(date.atStartOfDay())
    fun calculateDayUsage(dateTime: LocalDateTime): DayUsage {
        val timezone = ZoneId.systemDefault().rules.getOffset(Instant.now())
        val dayStamp = dateTime.truncatedTo(ChronoUnit.DAYS).toInstant(timezone).toEpochMilli()
        val hours: MutableMap<Long, HourUsage> = mutableMapOf()

        for (k in 0..23) {
            val globalHour = dayStamp + k * 3_600_000L
            hours[globalHour] = getCurrentHourUsage(globalHour, globalHour + 3_600_000L)
        }

        return DayUsage(dateTime.toLocalDate(), hours).also { it.categorizeUsage() }
    }

    fun getCurrentHourUsage(startTime: Long, endTime: Long): HourUsage {
        val statsWifi = networkStatsManager?.querySummaryForDevice(NetworkType.Wifi.ordinal, null, startTime, endTime)
        val statsMobile = networkStatsManager?.querySummaryForDevice(NetworkType.Cellular.ordinal, null, startTime, endTime)

        val hourUsage = HourUsage()
        statsMobile?.let {
            hourUsage.cellular += it.txBytes + it.rxBytes
            hourUsage.upload += it.txBytes
            hourUsage.download += it.rxBytes
        }

        statsWifi?.let {
            hourUsage.wifi += it.txBytes + it.rxBytes
            hourUsage.upload += it.txBytes
            hourUsage.download += it.rxBytes
        }
        return hourUsage
    }
}