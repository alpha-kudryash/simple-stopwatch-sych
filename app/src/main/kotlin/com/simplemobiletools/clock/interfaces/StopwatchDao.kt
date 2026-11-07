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

    /*@Query("""
        SELECT 
            id, 
            stopwatchSetNum, 
            milliseconds,
            ROW_NUMBER() OVER (PARTITION BY stopwatchSetNum ORDER BY milliseconds) AS row_num
        FROM laps
    """)
        fun getSeqnumber(id: Int)*/

    @Query("SELECT * FROM laps WHERE milliseconds=:milliseconds AND label=:label")
    fun findLaps(milliseconds: Long, label: String): List<Stopwatch>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateStopwatch(stopwatchList: List<Stopwatch>): List<Long>

    /** Allocate the set number and replace its rows atomically, keeping an existing set on failure. */
    @Transaction
    fun saveSet(stopwatchList: List<Stopwatch>): Pair<Int, List<Long>> {
        val first = stopwatchList.firstOrNull() ?: return 0 to emptyList()
        val setId = first.stopwatchSetNum.takeIf { it > 0 } ?: ((getMaxSetNum() ?: 0) + 1)
        val rows = stopwatchList.map {
            it.copy(stopwatchSetNum = setId, label = it.label.ifBlank { setId.toString() })
        }
        deleteSet(setId)
        return setId to insertOrUpdateStopwatch(rows)
    }

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
