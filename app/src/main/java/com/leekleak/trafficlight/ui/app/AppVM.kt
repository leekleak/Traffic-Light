package com.leekleak.trafficlight.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.DayUsageRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class AppVM : ViewModel(), KoinComponent {
    private val dayUsageRepo: DayUsageRepo by inject()
    fun updateDB() = viewModelScope.launch (Dispatchers.IO) { dayUsageRepo.populateDb() }
}