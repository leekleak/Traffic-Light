package com.leekleak.trafficlight.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.DayUsageRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate


class HistoryVM() : ViewModel(), KoinComponent {
    private val dayUsageRepo: DayUsageRepo by inject()

    @OptIn(FlowPreview::class)
    fun usageHistory(now: LocalDate): Flow<List<DayUsage?>> =
        combine(
            (1..28).map {
                getDayUsage(now.minusDays(it.toLong()))
            }
        ) {it.toList()}

    fun getDayUsage(date: LocalDate): Flow<DayUsage?> {
        return dayUsageRepo.getDayUsage(date)
    }

    fun pop() {
        viewModelScope.launch(Dispatchers.IO) {
            dayUsageRepo.populateDb()
        }
    }
}