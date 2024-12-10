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

    fun insertOrUpdateStopwatch(stopwatchList: List<Stopwatch>, callback: (ids: List<Long>) -> Unit = {}) {
        ensureBackgroundThread {
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

    fun deleteLaps(laps: List<Stopwatch>, callback: () -> Unit = {}) {
        ensureBackgroundThread {
            stopwatchDao.deleteLaps(laps)
            callback.invoke()
        }
    }
}
