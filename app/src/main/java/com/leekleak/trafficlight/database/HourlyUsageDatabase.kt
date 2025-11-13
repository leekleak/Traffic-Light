package com.leekleak.trafficlight.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.leekleak.trafficlight.database.TrafficSnapshot.Converters

@Database(entities = [HourUsage::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class HourlyUsageDatabase : RoomDatabase() {
    abstract fun hourlyUsageDao(): HourlyUsageDao
    companion object {
        private var _instance: HourlyUsageDatabase? = null
        fun getInstance(context: Context): HourlyUsageDatabase {
            if (_instance == null) {
                _instance = Room.databaseBuilder(
                    context.applicationContext,
                    HourlyUsageDatabase::class.java,
                    "hourly_usage_database").build()
            }
            return _instance!!
        }
    }
}