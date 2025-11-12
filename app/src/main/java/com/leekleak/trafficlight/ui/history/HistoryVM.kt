package com.leekleak.trafficlight.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.DayUsageRepo
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate


class HistoryVM() : ViewModel(), KoinComponent {
    private val dayUsageRepo: DayUsageRepo by inject()

    val usageHistoryFlow: Flow<PagingData<DayUsage>> =
        dayUsageRepo.getAllDayUsagePaged().cachedIn(viewModelScope)

    fun getDayUsage(date: LocalDate): Flow<DayUsage?> {
        return dayUsageRepo.getDayUsage(date)
    }
}