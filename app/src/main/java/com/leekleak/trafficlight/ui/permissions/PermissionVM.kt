package com.leekleak.trafficlight.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import org.koin.core.component.KoinComponent

class PermissionVM : ViewModel(), KoinComponent {
    fun allowBackground(activity: Activity) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)/*.apply {
            data = ("package:${activity.packageName}").toUri()
        }*/
        activity.startActivity(intent)
    }

    fun allowNotifications(activity: Activity) {
        activity.requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            1
        )
    }

    fun allowUsage(activity: Activity) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        activity.startActivity(intent)
    }
}