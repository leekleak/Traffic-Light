package com.leekleak.trafficlight.ui.history

import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.database.HourUsage
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate


class HistoryVM: ViewModel(), KoinComponent {
    private val hourlyUsageRepo: HourlyUsageRepo by inject()

    val lastDayFlow: Flow<LocalDate> = hourlyUsageRepo.getLastDayWithData()
    fun dayUsage(startStamp: Long, endStamp: Long): Flow<List<HourUsage>> =
        hourlyUsageRepo.getUsage(startStamp, endStamp)

    val getMaxCombinedUsage: Flow<Long> = hourlyUsageRepo.getMaxCombinedUsage()
}