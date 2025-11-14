package com.leekleak.trafficlight.ui.overview

import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.services.UsageService
import com.leekleak.trafficlight.util.padHour
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class OverviewVM : ViewModel(), KoinComponent {
    private val hourlyUsageRepo: HourlyUsageRepo by inject()
    val todayUsage: Flow<DayUsage> = UsageService.todayUsageFlow

    fun weekUsage(weekStartStamp: Long, todayStamp: Long, todayUsage: DayUsage): Flow<List<BarData>> =
        hourlyUsageRepo.getUsage(weekStartStamp, todayStamp).map { usageList ->
            val data = MutableList(7) { i ->
                val x = DayOfWeek.entries[i].getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())
                BarData(x, 0.0, 0.0)
            }
            val todayWeekDay = LocalDate.now().dayOfWeek.value - 1
            data[todayWeekDay] += BarData("",todayUsage.totalCellular.toDouble(),todayUsage.totalWifi.toDouble())

            val zone = ZoneId.systemDefault()

            for (usage in usageList) {
                val day = Instant.ofEpochMilli(usage.timestamp)
                    .atZone(zone)
                    .toLocalDate()

                val idx = day.dayOfWeek.value - 1

                data[idx] += BarData(
                    "",
                    usage.totalCellular.toDouble(),
                    usage.totalWifi.toDouble()
                )
            }
            data
        }
}