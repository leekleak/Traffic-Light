package com.leekleak.trafficlight.ui.overview

import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.DayUsageRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OverviewVM : ViewModel(), KoinComponent {
    private val dayUsageRepo: DayUsageRepo by inject()
    val todayUsage: Flow<DayUsage> = dayUsageRepo.getTodayUsage().map { it ?: DayUsage() }
}