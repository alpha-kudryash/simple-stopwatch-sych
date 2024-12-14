package com.simplemobiletools.clock.helpers

import android.os.SystemClock
import com.simplemobiletools.clock.models.Lap
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CopyOnWriteArraySet

private const val UPDATE_INTERVAL = 10L

object CurrentStopwatch {

    private var updateTimer = Timer()
    private var uptimeAtStart = 0L
    private var totalTicks = 0L
    private var lapTicks = 0L
    private var currentLap = 1
    var currentSetId = 0
    val laps = ArrayList<Lap>()
    var state = State.RESETED
        private set(value) {
            field = value
            for (listener in updateListeners) {
                listener.onStateChanged(value)
            }
        }
    private var updateListeners = CopyOnWriteArraySet<UpdateListener>()

    fun reset() {
        updateTimer.cancel()
        state = State.RESETED
        currentLap = 1
        totalTicks = 0
        lapTicks = 0
        laps.clear()
    }

    fun lap() {
        if (laps.isEmpty()) {
            val lap = Lap(currentLap++, lapTicks, totalTicks, "")
            laps.add(0, lap)
            lapTicks = 0
        } else {
            laps.first().apply {
                lapTime = lapTicks
                totalTime = totalTicks
            }
        }
        val lap = Lap(currentLap++, totalTicks, totalTicks, "")
        laps.add(0, lap)
        lapTicks = 0
    }

    fun toggle(setUptimeAtStart: Boolean) {
        when (state) {
            State.RESETED -> {
                state = State.RUNNING
                updateTimer = buildUpdateTimer()
                if (setUptimeAtStart) {
                    uptimeAtStart = SystemClock.uptimeMillis()
                }
            }
            State.PAUSED -> {
                state = State.RUNNING
                updateTimer = buildUpdateTimer()
                if (setUptimeAtStart) {
                    uptimeAtStart = SystemClock.uptimeMillis()
                }
            }
            else -> {}
        }
    }

    fun pause() {
        state = State.PAUSED
        val totalDuration = SystemClock.uptimeMillis() - uptimeAtStart + totalTicks
        updateTimer.cancel()
        for (listener in updateListeners) {
            listener.onUpdate(totalDuration, -1)
        }
    }

    /**
     * Add a update listener to the stopwatch. The listener gets the current state
     * immediately after adding. To avoid memory leaks the listener should be removed
     * from the stopwatch.
     * @param updateListener the listener
     */
    fun addUpdateListener(updateListener: UpdateListener) {
        updateListeners.add(updateListener)
        updateListener.onUpdate(
            totalTicks,
            lapTicks,
        )
        updateListener.onStateChanged(state)
    }

    /**
     * Remove the listener from the stopwatch
     * @param updateListener the listener
     */
    fun removeUpdateListener(updateListener: UpdateListener) {
        updateListeners.remove(updateListener)
    }

    private fun buildUpdateTimer(): Timer {
        return Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    if (state == State.RUNNING) {
                        for (listener in updateListeners) {
                            listener.onUpdate(
                                totalTicks,
                                lapTicks
                            )
                        }
                        totalTicks += UPDATE_INTERVAL
                        lapTicks += UPDATE_INTERVAL
                    }
                }
            }, 0, UPDATE_INTERVAL)
        }
    }

    enum class State {
        RUNNING,
        PAUSED,
        RESETED
    }

    interface UpdateListener {
        fun onUpdate(totalTime: Long, lapTime: Long)
        fun onStateChanged(state: State)
    }
}
