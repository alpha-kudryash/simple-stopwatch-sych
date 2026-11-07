package com.simplemobiletools.clock.helpers

import android.app.Application
import android.content.Context
import android.provider.Settings
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class StopwatchSnapshotStoreTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before fun clearState() {
        context.getSharedPreferences("current_stopwatch", Context.MODE_PRIVATE).edit().clear().commit()
        Settings.Global.putInt(context.contentResolver, "boot_count", 12)
    }

    @Test fun snapshotRoundTripKeepsTimesTextAndSetNumber() {
        var now = 100L
        val stopwatch = StopwatchEngine { now }
        stopwatch.start()
        now += 123
        stopwatch.updateLapText(1, "Этап 👟")
        stopwatch.lap()
        stopwatch.currentSetId = 4
        val saved = stopwatch.snapshot()
        StopwatchSnapshotStore(context).save(saved)
        val restored = StopwatchSnapshotStore(context).load()!!
        assertEquals(saved, restored.snapshot)
        assertTrue(restored.sameBoot)
    }

    @Test fun rebootIsDetectedEvenWhenUptimeHasCaughtUpWithThePreviousBoot() {
        val stopwatch = StopwatchEngine { 1_000L }
        stopwatch.start()
        StopwatchSnapshotStore(context).save(stopwatch.snapshot())
        Settings.Global.putInt(context.contentResolver, "boot_count", 13)
        assertFalse(StopwatchSnapshotStore(context).load()!!.sameBoot)
    }

    @Test fun resetRemovesThePreviousMeasurement() {
        val stopwatch = StopwatchEngine { 100L }
        stopwatch.start()
        val store = StopwatchSnapshotStore(context)
        store.save(stopwatch.snapshot())
        stopwatch.reset()
        store.save(stopwatch.snapshot())
        assertNull(StopwatchSnapshotStore(context).load())
    }

    @Test fun corruptSnapshotDoesNotPreventLaunchingTheApp() {
        context.getSharedPreferences("current_stopwatch", Context.MODE_PRIVATE)
            .edit().putString("snapshot", "not valid json").commit()
        assertNull(StopwatchSnapshotStore(context).load())
    }
}
