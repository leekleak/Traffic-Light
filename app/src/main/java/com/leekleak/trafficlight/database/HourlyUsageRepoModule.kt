package com.leekleak.trafficlight.database

import android.content.Context
import androidx.room.Room
import org.koin.dsl.module

val hourlyUsageRepoModule = module {single { HourlyUsageRepo(get()) }}
val databaseModule = module {
    single {
        Room.databaseBuilder(
            get<Context>().applicationContext,
            HourlyUsageDatabase::class.java,
            "hourly_usage_database"
        ).build()
    }
    single { get<HourlyUsageDatabase>().dao() }
}
