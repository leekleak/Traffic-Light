package com.leekleak.trafficlight.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.DayUsageRepository
import com.leekleak.trafficlight.services.UsageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDate


class HomeScreenVM() : ViewModel(), KoinComponent {
    private val dayUsageRepository: DayUsageRepository by inject(DayUsageRepository::class.java)
    val todayUsage: Flow<DayUsage> = dayUsageRepository.getTodayUsage().map { it ?: DayUsage() }

    @OptIn(FlowPreview::class)
    fun usageHistory(now: LocalDate): Flow<List<DayUsage?>> =
        combine(
            (1..28).map {
                getDayUsage(now.minusDays(it.toLong()))
            }
        ) {it.toList()}//.debounce { 500 }

    fun getDayUsage(date: LocalDate): Flow<DayUsage?> {
        return dayUsageRepository.getDayUsage(date)
    }

    fun runService(activity: Activity?) {
        activity?.let {
            UsageService.startService(it)
        }
    }

    fun pop() {
        viewModelScope.launch(Dispatchers.IO) {
            dayUsageRepository.populateDb()
        }
    }
}