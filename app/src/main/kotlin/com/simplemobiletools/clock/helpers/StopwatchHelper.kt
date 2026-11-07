package com.simplemobiletools.clock.helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.simplemobiletools.clock.extensions.stopwatchDb
import com.simplemobiletools.clock.models.Stopwatch
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.extensions.showErrorToast

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

    fun insertOrUpdateStopwatch(
        stopwatchList: List<Stopwatch>,
        onError: (Exception) -> Unit = { error ->
            Handler(Looper.getMainLooper()).post { context.showErrorToast(error) }
        },
        callback: (ids: List<Long>, setId: Int) -> Unit = { _, _ -> },
    ) {
        ensureBackgroundThread {
            try {
                val (setId, ids) = stopwatchDao.saveSet(stopwatchList)
                callback.invoke(ids, setId)
            } catch (error: Exception) {
                onError(error)
            }
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
