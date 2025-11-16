package com.leekleak.trafficlight.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HourUsage::class], version = 1, exportSchema = false)
abstract class HourlyUsageDatabase : RoomDatabase() {
    abstract fun dao(): HourlyUsageDao
}