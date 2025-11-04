package com.leekleak.trafficlight.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.services.UsageService
import org.koin.core.component.KoinComponent

class NavigationVM : ViewModel(), KoinComponent {
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
}