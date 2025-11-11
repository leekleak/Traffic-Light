package com.leekleak.trafficlight.util

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.AppOpsManager
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.pm.PackageManager
import android.os.PowerManager
import android.os.Process.myUid
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED


fun hasAllPermissions(context: Context): Boolean {
    return  hasBackgroundPermission(context) &&
            hasUsageStatsPermission(context) &&
            hasNotificationPermission(context)
}
fun hasBackgroundPermission(context: Context): Boolean {
    val packageName: String? = context.packageName
    val pm = context.getSystemService(POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(packageName)
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOpsManager.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun hasNotificationPermission(context: Context): Boolean {
    return context.checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}