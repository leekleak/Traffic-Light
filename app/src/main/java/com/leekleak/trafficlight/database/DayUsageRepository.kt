package com.leekleak.trafficlight.database

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import kotlin.random.Random

class DayUsageRepository(context: Context) {
    private val dao = DayUsageDatabase.getInstance(context).dayUsageDao()

    fun getDayUsage(date: LocalDate): Flow<DayUsage?> = dao.getDayUsage(date)
    fun getTodayUsage(): Flow<DayUsage?> = dao.getTodayUsage()

    fun updateDayUsage(dayUsage: DayUsage) = dao.updateDayUsage(dayUsage)

    fun dayUsageExists(date: LocalDate) = dao.dayUsageExists(date)

    fun addDayUsage(dayUsage: DayUsage) = dao.addDayUsage(dayUsage)

    fun populateDb() {
        val seeds = listOf(Random(1), Random(2), Random(3), Random(4))
        for (i in 0..28) {
            val hours: MutableMap<Long, HourUsage> = mutableMapOf()
            val date = LocalDate.now().minusDays(i.toLong()).atStartOfDay()
            for (k in 0..23) {
                hours[k * 3_600_000L] = HourUsage(
                    seeds[0].nextLong(1_000_000_000)/(i + 1),
                    seeds[1].nextLong(1_000_000_000)/(i + 1),
                    seeds[2].nextLong(1_000_000_000)/(i + 1),
                    seeds[3].nextLong(1_000_000_000)/(i + 1) / 2,
                )
            }

            val dayUsage = DayUsage(
                date.toLocalDate(),
                hours
            )

            if (dao.dayUsageExists(dayUsage.date)) {
                dao.updateDayUsage(dayUsage)
            } else {
                dao.addDayUsage(dayUsage)
            }
        }
    }
}