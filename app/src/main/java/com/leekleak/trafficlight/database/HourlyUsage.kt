package com.leekleak.trafficlight.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity
data class HourUsage(
    @PrimaryKey val timestamp: Long,
    val totalWifi: Long,
    val totalCellular: Long
)

@Dao
interface HourlyUsageDao {
    @Query("SELECT * FROM HourUsage WHERE timestamp BETWEEN :startStamp AND :endStamp ORDER BY timestamp ASC")
    fun getUsageFlow(startStamp: Long, endStamp: Long): Flow<List<HourUsage>>

    @Query("SELECT * FROM HourUsage WHERE timestamp BETWEEN :startStamp AND :endStamp ORDER BY timestamp ASC")
    fun getUsage(startStamp: Long, endStamp: Long): List<HourUsage>

    @Query("SELECT EXISTS(SELECT * FROM HourUsage WHERE timestamp = :stamp)")
    fun hourUsageExists(stamp: Long): Boolean

    @Update
    fun updateHourUsage(hourUsage: HourUsage)

    @Delete
    fun deleteHourUsage(hourUsage: HourUsage)

    @Insert
    fun addHourUsage(hourUsage: HourUsage)

    @Query("SELECT COUNT(*) FROM HourUsage")
    fun getDBSize(): Flow<Int>

    @Query("DELETE FROM HourUsage")
    fun clear()

    @Query("SELECT MAX(totalCellular + totalWifi) FROM HourUsage")
    fun getMaxCombinedUsage(): Flow<Long>

    @Query("SELECT * FROM HourUsage ORDER BY timestamp ASC LIMIT 1")
    fun getLastUsage(): Flow<HourUsage?>
}