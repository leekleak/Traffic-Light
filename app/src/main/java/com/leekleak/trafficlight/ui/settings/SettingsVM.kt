package com.leekleak.trafficlight.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.DayUsageRepo
import com.leekleak.trafficlight.model.PreferenceRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsVM : ViewModel(), KoinComponent {
    private val preferenceRepo: PreferenceRepo by inject()

    private val dayUsageRepo: DayUsageRepo by inject()

    val modeAOD = preferenceRepo.modeAOD.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun setModeAOD(enabled: Boolean) = preferenceRepo.setModeAOD(enabled)

    val bigIcon = preferenceRepo.bigIcon.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun setBigIcon(enabled: Boolean) = preferenceRepo.setBigIcon(enabled)

    val dbSize = dayUsageRepo.getDBSize().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)
    fun repopulateDB() {
        viewModelScope.launch(Dispatchers.IO) {
            dayUsageRepo.clearDB()
            dayUsageRepo.populateDb()
        }
    }
}