package com.simplemobiletools.clock.activities

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import com.simplemobiletools.clock.R
import com.simplemobiletools.clock.dialogs.EditTimerDialog
import com.simplemobiletools.clock.extensions.*
import com.simplemobiletools.clock.helpers.*
import com.simplemobiletools.clock.models.*
import com.simplemobiletools.commons.dialogs.PermissionRequiredDialog
import com.simplemobiletools.commons.extensions.getDefaultAlarmSound
import com.simplemobiletools.commons.extensions.getFilenameFromUri
import com.simplemobiletools.commons.extensions.openNotificationSettings
import com.simplemobiletools.commons.helpers.SILENT
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.AlarmSound
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class IntentHandlerActivity : SimpleActivity() {
    companion object {
        @SuppressLint("InlinedApi")
        val HANDLED_ACTIONS = listOf(
            AlarmClock.ACTION_SET_TIMER,
            AlarmClock.ACTION_DISMISS_TIMER
        )

        private const val URI_SCHEME = "id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intentToHandle: Intent) {
        intentToHandle.apply {
            when (action) {
                AlarmClock.ACTION_SET_TIMER -> setNewTimer()
                AlarmClock.ACTION_DISMISS_TIMER -> dismissTimer()
                else -> finish()
            }
        }
    }

    private fun Intent.setNewLaps() {
        //stopwatchHelper.insertOrUpdateStopwatch()
    }

    private fun Intent.setNewTimer() {
        val length = getIntExtra(AlarmClock.EXTRA_LENGTH, -1)
        val message = getStringExtra(AlarmClock.EXTRA_MESSAGE)
        val skipUi = getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false)

        fun createAndStartNewTimer() {
            val newTimer = createNewTimer()
            if (message != null) {
                newTimer.label = message
            }

            if (length < 0 || !skipUi) {
                newTimer.id = -1
                openEditTimer(newTimer)
            } else {
                newTimer.seconds = length
                newTimer.oneShot = true

            }
        }
    }

    private fun Intent.dismissTimer() {
        val uri = data
        if (uri == null) {
            return
        } else if (uri.scheme == URI_SCHEME) {
            val id = uri.schemeSpecificPart.toIntOrNull()
            if (id != null) {
                return
            }
        }
        finish()
    }

    private fun openEditTimer(timer: Timer) {
        EditTimerDialog(this, timer) {
            timer.id = it.toInt()
            startTimer(timer)
        }
    }

    private fun startTimer(timer: Timer) {
        handleNotificationPermission { granted ->
            val newState = TimerState.Running(timer.seconds.secondsToMillis, timer.seconds.secondsToMillis)
            val newTimer = timer.copy(state = newState)
            fun notifyAndStartTimer() {
                EventBus.getDefault().post(TimerEvent.Start(newTimer.id!!, newTimer.seconds.secondsToMillis))
                EventBus.getDefault().post(TimerEvent.Refresh)
            }

            if (granted) {
            } else {
                PermissionRequiredDialog(
                    this,
                    com.simplemobiletools.commons.R.string.allow_notifications_reminders,
                    positiveActionCallback = {
                        openNotificationSettings()
                    },
                    negativeActionCallback = {
                        finish()
                    })
            }
        }
    }
}
