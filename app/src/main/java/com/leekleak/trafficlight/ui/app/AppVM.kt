package com.leekleak.trafficlight.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.HourlyUsageRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class AppVM : ViewModel(), KoinComponent {
    private val hourlyUsageRepo: HourlyUsageRepo by inject()
    fun updateDB() = viewModelScope.launch (Dispatchers.IO) { hourlyUsageRepo.populateDb() }
}