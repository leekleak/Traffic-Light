package com.leekleak.trafficlight.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.leekleak.trafficlight.util.hasAllPermissions

class AutoStarter : BroadcastReceiver()
{
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context?.let {
                if (hasAllPermissions(context)) {
                    UsageService.startService(it)
                }
            }
        }
    }
}