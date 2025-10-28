package com.leekleak.trafficlight

import android.app.Application
import com.leekleak.trafficlight.database.dayUsageModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class TrafficLightApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TrafficLightApplication)
            modules(dayUsageModule)
        }
    }
}