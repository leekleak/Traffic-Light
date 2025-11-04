package com.leekleak.trafficlight.ui.overview

import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.DayUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent.inject
import kotlin.getValue

class OverviewVM : ViewModel(), KoinComponent {
    private val dayUsageRepository: DayUsageRepository by inject(DayUsageRepository::class.java)
    val todayUsage: Flow<DayUsage> = dayUsageRepository.getTodayUsage().map { it ?: DayUsage() }
}