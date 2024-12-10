package com.simplemobiletools.clock.interfaces

import androidx.room.*
import com.simplemobiletools.clock.models.Stopwatch

@Dao
interface StopwatchDao {

    @Query("SELECT * FROM laps ORDER BY createdAt ASC")
    fun getLaps(): List<Stopwatch>

    @Query("SELECT * FROM laps WHERE id=:id")
    fun getLap(id: Int): Stopwatch?

    //@Query("SELECT * FROM laps WHERE seconds=:seconds AND label=:label")
    //fun findLaps(seconds: Int, label: String): List<Stopwatch>

    @Query("SELECT * FROM laps WHERE milliseconds=:milliseconds AND label=:label")
    fun findLaps(milliseconds: Long, label: String): List<Stopwatch>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateStopwatch(stopwatchList: List<Stopwatch>): List<Long>

    @Query("DELETE FROM laps WHERE id=:id")
    fun deleteLap(id: Int)

    @Delete
    fun deleteLaps(list: List<Stopwatch>)
}
