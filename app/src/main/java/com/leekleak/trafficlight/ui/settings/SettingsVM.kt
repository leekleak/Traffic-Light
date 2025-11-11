package com.leekleak.trafficlight.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsVM : ViewModel(), KoinComponent {
    val preferenceRepo: PreferenceRepo by inject()

    val modeAOD = preferenceRepo.modeAOD.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    fun setModeAOD(enabled: Boolean) = preferenceRepo.setModeAOD(enabled)
}