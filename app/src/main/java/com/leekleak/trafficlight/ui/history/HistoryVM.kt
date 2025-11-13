package com.leekleak.trafficlight.ui.history

import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.database.DayUsageRepo
import com.leekleak.trafficlight.database.HourUsage
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate


class HistoryVM() : ViewModel(), KoinComponent {
    private val dayUsageRepo: DayUsageRepo by inject()

    val lastDayFlow: Flow<LocalDate> = dayUsageRepo.getLastDayWithData()
    fun dayUsage(startStamp: Long, endStamp: Long): Flow<List<HourUsage>> =
        dayUsageRepo.getUsage(startStamp, endStamp)

    val getMaxCombinedUsage: Flow<Long> = dayUsageRepo.getMaxCombinedUsage()
}