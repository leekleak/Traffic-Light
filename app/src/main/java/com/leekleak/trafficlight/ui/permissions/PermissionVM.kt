package com.leekleak.trafficlight.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.services.UsageService
import org.koin.core.component.KoinComponent

class PermissionVM : ViewModel(), KoinComponent {
    fun disableBatteryOptimization(activity: Activity) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = ("package:${activity.packageName}").toUri()
        }
        activity.startActivity(intent)
    }

    fun allowNotifications(activity: Activity) {
        activity.requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            1
        )
    }
}