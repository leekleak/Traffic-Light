package com.leekleak.trafficlight.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.leekleak.trafficlight.database.TrafficSnapshot.Converters

@Database(entities = [DayUsage::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class DayUsageDatabase : RoomDatabase() {
    abstract fun dayUsageDao(): DayUsageDao
    companion object {
        private var _instance: DayUsageDatabase? = null
        fun getInstance(context: Context): DayUsageDatabase {
            if (_instance == null) {
                _instance = Room.databaseBuilder(
                    context.applicationContext,
                    DayUsageDatabase::class.java,
                    "day_usage_database").build()
            }
            return _instance!!
        }
    }
}