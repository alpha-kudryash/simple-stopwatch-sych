package com.simplemobiletools.clock.helpers

import com.simplemobiletools.clock.models.Lap

/** Durations come from elapsed realtime, never from the number of display callbacks. */
class StopwatchEngine(private val elapsedRealtime: () -> Long) {
    enum class State { RUNNING, PAUSED, RESETED }

    data class Reading(val totalTime: Long, val lapTime: Long)
    data class Snapshot(
        val state: State,
        val totalTime: Long,
        val savedAtElapsedRealtime: Long,
        val lapStartTime: Long,
        val nextLapId: Int,
        val currentSetId: Int,
        val laps: List<Lap>,
    )

    var state = State.RESETED
        private set
    var currentSetId = 0
    private var accumulatedTime = 0L
    private var runningSince = 0L
    private var lapStartTime = 0L
    private var nextLapId = 1
    private val lapItems = ArrayList<Lap>()

    fun reading(): Reading {
        val total = totalTimeAt(elapsedRealtime())
        return Reading(total, total - lapStartTime)
    }

    private fun totalTimeAt(now: Long) = accumulatedTime + if (state == State.RUNNING) {
        (now - runningSince).coerceAtLeast(0L)
    } else 0L

    /** The first row is the live lap; completed laps follow it in reverse order. */
    fun laps(): ArrayList<Lap> = copyLaps(reading())

    private fun copyLaps(time: Reading) = ArrayList(lapItems.mapIndexed { index, lap ->
        if (index == 0) lap.copy(lapTime = time.lapTime, totalTime = time.totalTime) else lap.copy()
    })

    fun start() {
        if (state == State.RUNNING) return
        if (state == State.RESETED) lapItems.add(Lap(nextLapId++, 0L, 0L, ""))
        runningSince = elapsedRealtime()
        state = State.RUNNING
    }

    fun pause() {
        if (state != State.RUNNING) return
        accumulatedTime = reading().totalTime
        state = State.PAUSED
    }

    fun lap() {
        if (state != State.RUNNING) return
        val time = reading()
        lapItems.first().apply {
            lapTime = time.lapTime
            totalTime = time.totalTime
        }
        lapStartTime = time.totalTime
        lapItems.add(0, Lap(nextLapId++, 0L, time.totalTime, ""))
    }

    fun updateLapText(id: Int, text: String) {
        lapItems.firstOrNull { it.id == id }?.textTime = text
    }

    fun reset() {
        state = State.RESETED
        accumulatedTime = 0L
        runningSince = 0L
        lapStartTime = 0L
        nextLapId = 1
        currentSetId = 0
        lapItems.clear()
    }

    fun snapshot(): Snapshot {
        val now = elapsedRealtime()
        val total = totalTimeAt(now)
        return Snapshot(state, total, now, lapStartTime, nextLapId, currentSetId, copyLaps(Reading(total, total - lapStartTime)))
    }

    /** After a reboot preserve the last known duration, without mixing clocks from two boots. */
    fun restore(snapshot: Snapshot, sameBoot: Boolean) {
        require(snapshot.totalTime >= 0L && snapshot.lapStartTime in 0L..snapshot.totalTime)
        require(snapshot.nextLapId > (snapshot.laps.maxOfOrNull { it.id } ?: 0))
        require(snapshot.state == State.RESETED || snapshot.laps.isNotEmpty())
        state = if (snapshot.state == State.RUNNING && (!sameBoot || elapsedRealtime() < snapshot.savedAtElapsedRealtime)) {
            State.PAUSED
        } else snapshot.state
        accumulatedTime = snapshot.totalTime
        runningSince = snapshot.savedAtElapsedRealtime
        lapStartTime = snapshot.lapStartTime
        nextLapId = snapshot.nextLapId
        currentSetId = snapshot.currentSetId
        lapItems.clear()
        lapItems.addAll(snapshot.laps.map { it.copy() })
    }
}
