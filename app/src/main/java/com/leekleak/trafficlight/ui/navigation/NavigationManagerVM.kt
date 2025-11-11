package com.leekleak.trafficlight.ui.navigation

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.services.UsageService

class NavigationManagerVM : ViewModel() {
    fun runService(activity: Activity?) {
        activity?.let {
            UsageService.startService(it)
        }
    }
}