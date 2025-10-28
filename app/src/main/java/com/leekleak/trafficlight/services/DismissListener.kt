package com.leekleak.trafficlight.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DismissListener : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        UsageService.stopService()
    }
}