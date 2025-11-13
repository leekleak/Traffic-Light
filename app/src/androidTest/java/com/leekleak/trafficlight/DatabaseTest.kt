package com.leekleak.trafficlight

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.DayUsageDao
import com.leekleak.trafficlight.database.DayUsageDatabase
import com.leekleak.trafficlight.database.HourData
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDate


@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var dayUsageDao: DayUsageDao
    private lateinit var db: DayUsageDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, DayUsageDatabase::class.java).build()
        dayUsageDao = db.dayUsageDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeUserAndReadInList() {
        for (i in 1..10) {
            val hours: MutableMap<Long, HourData> = mutableMapOf()
            for (k in 0..23) {
                hours[k.toLong()] = HourData(k.toLong(), 0)
            }

            val dayUsage = DayUsage(
                LocalDate.now().minusDays(i.toLong()),
                hours
            )
            dayUsageDao.addDayUsage(dayUsage)
        }
    }
}