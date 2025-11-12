package com.leekleak.trafficlight.ui.overview

import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.services.UsageService
import kotlinx.coroutines.flow.Flow

class OverviewVM : ViewModel() {
    val todayUsage: Flow<DayUsage> = UsageService.todayUsageFlow
}