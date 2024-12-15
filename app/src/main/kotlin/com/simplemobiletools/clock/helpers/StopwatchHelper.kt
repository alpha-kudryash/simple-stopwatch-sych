package com.simplemobiletools.clock.helpers

import android.content.Context
import com.simplemobiletools.clock.extensions.stopwatchDb
import com.simplemobiletools.clock.models.Stopwatch
import com.simplemobiletools.commons.helpers.ensureBackgroundThread

class StopwatchHelper(val context: Context) {
    private val stopwatchDao = context.stopwatchDb

    fun getLaps(callback: (laps: List<Stopwatch>) -> Unit) {
        ensureBackgroundThread {
            callback.invoke(stopwatchDao.getLaps())
        }
    }

    fun getMaxSetIdStopwatch(callback: (num: Int) -> Unit) {
        ensureBackgroundThread {
            val num = stopwatchDao.getMaxSetNum() ?: 0
            callback.invoke(num)
        }
    }

    fun getLastSetIdStopwatch(callback: (num: Int) -> Unit) {
        ensureBackgroundThread {
            val lastSetNum = stopwatchDao.getLastSetNum() ?: 0
            callback.invoke(lastSetNum)
        }
    }

    fun insertOrUpdateStopwatch(stopwatchList: List<Stopwatch>, callback: (ids: List<Long>) -> Unit = {}) {
        ensureBackgroundThread {
            stopwatchDao.deleteSet(stopwatchList[0].stopwatchSetNum)
            val ids = stopwatchDao.insertOrUpdateStopwatch(stopwatchList)
            callback.invoke(ids)
        }
    }

    fun deleteLap(id: Int, callback: () -> Unit = {}) {
        ensureBackgroundThread {
            stopwatchDao.deleteLap(id)
            callback.invoke()
        }
    }

    fun deleteSet(setId: Int, callback: () -> Unit = {}) {
        ensureBackgroundThread {
            stopwatchDao.deleteSet(setId)
            callback.invoke()
        }
    }

    fun deleteLaps(laps: List<Stopwatch>, callback: () -> Unit = {}) {
        ensureBackgroundThread {
            stopwatchDao.deleteLaps(laps)
            callback.invoke()
        }
    }

    fun deleteAllLaps(callback: () -> Unit = {}) {
        ensureBackgroundThread {
            stopwatchDao.deleteAllLaps()
            callback.invoke()
        }
    }
}
