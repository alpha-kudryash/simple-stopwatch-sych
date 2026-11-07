package com.simplemobiletools.clock.helpers

import org.junit.Assert.*
import org.junit.Test

class StopwatchEngineTest {
    private var now = 50_000L
    private val stopwatch = StopwatchEngine { now }

    @Test fun pauseDoesNotDoubleElapsedTimeAndIgnoresPausedTime() {
        stopwatch.start()
        now += 10_000
        stopwatch.pause()
        assertEquals(10_000L, stopwatch.reading().totalTime)
        now += 60_000
        stopwatch.pause()
        assertEquals(10_000L, stopwatch.reading().totalTime)
        stopwatch.start()
        now += 2_345
        stopwatch.pause()
        assertEquals(12_345L, stopwatch.reading().totalTime)
        assertEquals(12_345L, stopwatch.reading().lapTime)
    }

    @Test fun sleepAndDelayedDisplayCallbacksDoNotLoseTime() {
        stopwatch.start()
        now += 6 * 60 * 60 * 1_000L
        assertEquals(21_600_000L, stopwatch.reading().totalTime)
        stopwatch.lap()
        assertEquals(21_600_000L, stopwatch.laps()[1].totalTime)
    }

    @Test fun repeatedStartDoesNotRestartTheMeasurement() {
        stopwatch.start()
        now += 1_000
        stopwatch.start()
        now += 500
        assertEquals(1_500L, stopwatch.reading().totalTime)
        assertEquals(1, stopwatch.laps().size)
    }

    @Test fun firstLapIsMeasuredFromStartAndKeepsItsText() {
        stopwatch.start()
        stopwatch.updateLapText(1, "Первый этап")
        now += 1_234
        stopwatch.lap()
        assertEquals(1_234L, stopwatch.laps()[1].lapTime)
        assertEquals(1_234L, stopwatch.laps()[1].totalTime)
        assertEquals("Первый этап", stopwatch.laps()[1].textTime)
        assertEquals(0L, stopwatch.laps()[0].lapTime)
        now += 765
        stopwatch.lap()
        assertEquals(765L, stopwatch.laps()[1].lapTime)
        assertEquals(1_999L, stopwatch.laps()[1].totalTime)
        assertEquals("Первый этап", stopwatch.laps()[2].textTime)
    }

    @Test fun lapDurationExcludesPausesAndUsesTheExactCaptureTime() {
        stopwatch.start()
        now += 1_000
        stopwatch.lap()
        now += 200
        stopwatch.pause()
        now += 5_000
        stopwatch.start()
        now += 300
        stopwatch.lap()
        assertEquals(500L, stopwatch.laps()[1].lapTime)
        assertEquals(1_500L, stopwatch.laps()[1].totalTime)
    }

    @Test fun runningMeasurementCanBeRestoredWithoutATickingService() {
        stopwatch.start()
        now += 2_000
        stopwatch.lap()
        stopwatch.updateLapText(2, "Продолжение")
        stopwatch.currentSetId = 7
        now += 500
        val saved = stopwatch.snapshot()
        now += 30_000
        val restored = StopwatchEngine { now }
        restored.restore(saved, sameBoot = true)
        assertEquals(StopwatchEngine.State.RUNNING, restored.state)
        assertEquals(32_500L, restored.reading().totalTime)
        assertEquals(30_500L, restored.reading().lapTime)
        assertEquals("Продолжение", restored.laps()[0].textTime)
        assertEquals(7, restored.currentSetId)
        restored.lap()
        assertEquals(3, restored.laps()[0].id)
    }

    @Test fun pausedMeasurementStaysPausedAfterRestore() {
        stopwatch.start()
        now += 900
        stopwatch.pause()
        val saved = stopwatch.snapshot()
        now += 20_000
        val restored = StopwatchEngine { now }
        restored.restore(saved, sameBoot = true)
        assertEquals(StopwatchEngine.State.PAUSED, restored.state)
        assertEquals(900L, restored.reading().totalTime)
    }

    @Test fun rebootPreservesTheLastCheckpointAndPausesTheMeasurement() {
        stopwatch.start()
        now += 900
        val saved = stopwatch.snapshot()
        // Even if the next boot has a larger uptime, its time must not be added.
        now += 100_000
        val restored = StopwatchEngine { now }
        restored.restore(saved, sameBoot = false)
        assertEquals(StopwatchEngine.State.PAUSED, restored.state)
        assertEquals(900L, restored.reading().totalTime)
        assertEquals(1, restored.laps().size)
    }

    @Test fun backwardsClockIsAlsoTreatedAsAReboot() {
        stopwatch.start()
        now += 900
        val saved = stopwatch.snapshot()
        now = 100
        val restored = StopwatchEngine { now }
        restored.restore(saved, sameBoot = true)
        assertEquals(StopwatchEngine.State.PAUSED, restored.state)
        assertEquals(900L, restored.reading().totalTime)
    }

    @Test fun resetClearsTimesLapsAndTheSavedSetIdentity() {
        stopwatch.start()
        now += 100
        stopwatch.lap()
        stopwatch.currentSetId = 4
        stopwatch.reset()
        assertEquals(StopwatchEngine.State.RESETED, stopwatch.state)
        assertEquals(0L, stopwatch.reading().totalTime)
        assertEquals(0L, stopwatch.reading().lapTime)
        assertTrue(stopwatch.laps().isEmpty())
        assertEquals(0, stopwatch.currentSetId)
        stopwatch.start()
        assertEquals(1, stopwatch.laps().single().id)
    }

    @Test fun callersCannotMutateEngineStateThroughASnapshot() {
        stopwatch.start()
        stopwatch.laps().single().textTime = "External mutation"
        stopwatch.snapshot().laps.single().totalTime = 999
        assertEquals("", stopwatch.laps().single().textTime)
        assertEquals(0L, stopwatch.laps().single().totalTime)
    }

    @Test fun lapIsIgnoredUnlessTheStopwatchIsRunning() {
        stopwatch.lap()
        assertTrue(stopwatch.laps().isEmpty())
        stopwatch.start()
        stopwatch.pause()
        stopwatch.lap()
        assertEquals(1, stopwatch.laps().size)
    }
}
