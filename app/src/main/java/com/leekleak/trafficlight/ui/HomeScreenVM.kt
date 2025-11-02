package com.leekleak.trafficlight.ui

import android.app.Activity
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.DayUsageRepository
import com.leekleak.trafficlight.services.UsageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    fun batteryOptimizationDisabled(context: Context): Boolean {
        val packageName: String? = context.packageName
        val pm = context.getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    fun disableBatteryOptimization(context: Context) {
        val intent = Intent()
        val packageName: String? = context.packageName
        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        intent.data = ("package:$packageName").toUri()
        context.startActivity(intent)
    }

    fun pop() {
        viewModelScope.launch(Dispatchers.IO) {
            dayUsageRepository.populateDb()
        }
    }
}