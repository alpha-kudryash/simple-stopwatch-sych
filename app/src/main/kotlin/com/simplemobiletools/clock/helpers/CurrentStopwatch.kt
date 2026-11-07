package com.simplemobiletools.clock.helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.MainThread
import com.simplemobiletools.clock.helpers.StopwatchEngine.State
import com.simplemobiletools.clock.models.Lap

/** Commands and display callbacks run on the main thread, avoiding timer/UI data races. */
@MainThread
object CurrentStopwatch {
    private const val UPDATE_INTERVAL = 10L
    private val engine = StopwatchEngine(SystemClock::elapsedRealtime)
    private val handler = Handler(Looper.getMainLooper())
    private val updateListeners = LinkedHashSet<UpdateListener>()
    private var store: StopwatchSnapshotStore? = null

    val state: State get() = engine.state
    val laps: ArrayList<Lap> get() = engine.laps()
    var currentSetId: Int
        get() = engine.currentSetId
        set(value) {
            engine.currentSetId = value
            saveState()
        }

    fun initialize(context: Context) {
        if (store != null) return
        store = StopwatchSnapshotStore(context.applicationContext)
        store?.load()?.let { restored ->
            runCatching { engine.restore(restored.snapshot, restored.sameBoot) }
                .onFailure { engine.reset() }
        }
    }

    fun start() = change { engine.start() }
    fun pause() = change { engine.pause() }
    fun lap() = change { engine.lap() }
    fun reset() = change { engine.reset() }

    fun updateLapText(id: Int, text: String) {
        engine.updateLapText(id, text)
        saveState()
    }

    fun saveState() {
        store?.save(engine.snapshot())
    }

    private fun change(action: () -> Unit) {
        val oldState = state
        action()
        saveState()
        notifyListeners(oldState != state)
        scheduleUpdate()
    }

    fun addUpdateListener(listener: UpdateListener) {
        updateListeners.add(listener)
        listener.onStateChanged(state)
        val time = engine.reading()
        listener.onUpdate(time.totalTime, time.lapTime)
        scheduleUpdate()
    }

    fun removeUpdateListener(listener: UpdateListener) {
        updateListeners.remove(listener)
        scheduleUpdate()
    }

    private fun notifyListeners(stateChanged: Boolean = false) {
        val time = engine.reading()
        updateListeners.toList().forEach { listener ->
            if (stateChanged) listener.onStateChanged(state)
            listener.onUpdate(time.totalTime, time.lapTime)
        }
    }

    private val update = object : Runnable {
        override fun run() {
            notifyListeners()
            scheduleUpdate()
        }
    }

    private fun scheduleUpdate() {
        handler.removeCallbacks(update)
        if (state == State.RUNNING && updateListeners.isNotEmpty()) handler.postDelayed(update, UPDATE_INTERVAL)
    }

    interface UpdateListener {
        fun onUpdate(totalTime: Long, lapTime: Long)
        fun onStateChanged(state: State)
    }
}
