package com.simplemobiletools.clock.interfaces

import androidx.room.*
import com.simplemobiletools.clock.models.Stopwatch

@Dao
interface StopwatchDao {
    @Insert
    suspend fun insertStopwatches(laps: List<Stopwatch>)

    @Query("SELECT MAX(stopwatchSetNum) FROM laps")
    fun getMaxSetNum(): Int?

    @Query("SELECT stopwatchSetNum FROM laps LIMIT 1")
    fun getLastSetNum(): Int?

    @Query("SELECT * FROM laps ORDER BY stopwatchSetNum ASC, milliseconds ASC")
    fun getLaps(): List<Stopwatch>

    @Query("SELECT * FROM laps WHERE id=:id")
    fun getLap(id: Int): Stopwatch?

    @Query("SELECT * FROM laps WHERE milliseconds=:milliseconds AND label=:label")
    fun findLaps(milliseconds: Long, label: String): List<Stopwatch>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateStopwatch(stopwatchList: List<Stopwatch>): List<Long>

    @Query("DELETE FROM laps WHERE id=:id")
    fun deleteLap(id: Int)

    @Query("DELETE FROM laps WHERE stopwatchSetNum=:setId")
    fun deleteSet(setId: Int)

    @Query("DELETE FROM laps WHERE stopwatchSetNum = :number")
    suspend fun deleteBySetNum(number: Int)

    @Delete
    fun deleteLaps(list: List<Stopwatch>)

    @Query("DELETE FROM laps")
    fun deleteAllLaps()
}
