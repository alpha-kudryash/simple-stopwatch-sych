package com.simplemobiletools.clock.activities

import android.app.Application
import android.content.DialogInterface
import android.os.Looper
import android.view.KeyEvent
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.clock.R
import com.simplemobiletools.clock.adapters.StopwatchAdapter
import com.simplemobiletools.clock.helpers.CurrentStopwatch
import com.simplemobiletools.clock.helpers.StopwatchEngine
import com.simplemobiletools.commons.views.MyRecyclerView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class StopwatchControlsTest {
    @Test fun volumeKeysResumeTheTimerLapsFreezeImmediatelyAndResetRequiresConfirmation() {
        CurrentStopwatch.reset()
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        val activity = controller.get()
        shadowOf(Looper.getMainLooper()).idle()
        try {
            val list = activity.findViewById<MyRecyclerView>(R.id.stopwatch_list)
            val adapter = list.adapter as StopwatchAdapter
            fun volumeUp() = activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP))
            fun volumeDown() = activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN))

            volumeUp()
            assertEquals(StopwatchEngine.State.RUNNING, CurrentStopwatch.state)
            assertEquals(0, adapter.itemCount)
            ShadowSystemClock.advanceBy(Duration.ofMillis(1234))
            volumeUp()
            assertEquals(1, adapter.itemCount)
            val recorded = adapter.laps.single().copy()
            assertTrue(recorded.totalTime in 1234L..1240L)
            assertEquals(recorded.totalTime, recorded.lapTime)
            ShadowSystemClock.advanceBy(Duration.ofMillis(2000))
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(recorded, adapter.laps.single())

            volumeDown()
            assertEquals(StopwatchEngine.State.PAUSED, CurrentStopwatch.state)
            ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
            volumeUp()
            assertEquals(StopwatchEngine.State.RUNNING, CurrentStopwatch.state)
            assertEquals(1, adapter.itemCount)
            volumeDown()
            val beforeReset = CurrentStopwatch.laps
            val reset = activity.findViewById<ImageView>(R.id.stopwatch_pause_reset)
            reset.performClick()
            assertEquals(beforeReset, CurrentStopwatch.laps)
            val cancelled = ShadowDialog.getLatestDialog() as AlertDialog
            cancelled.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(StopwatchEngine.State.PAUSED, CurrentStopwatch.state)
            assertEquals(beforeReset, CurrentStopwatch.laps)

            reset.performClick()
            val confirmed = ShadowDialog.getLatestDialog() as AlertDialog
            confirmed.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(StopwatchEngine.State.RESETED, CurrentStopwatch.state)
            assertTrue(CurrentStopwatch.laps.isEmpty())
            assertEquals(0, adapter.itemCount)
        } finally {
            controller.pause().stop().destroy()
            CurrentStopwatch.reset()
        }
    }
}
