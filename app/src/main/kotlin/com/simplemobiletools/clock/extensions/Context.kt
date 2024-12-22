package com.simplemobiletools.clock.extensions

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager.STREAM_ALARM
import android.media.RingtoneManager
import android.net.Uri
import android.os.PowerManager
import android.text.SpannableString
import android.text.format.DateFormat
import android.text.style.RelativeSizeSpan
import androidx.core.app.NotificationCompat
import com.simplemobiletools.clock.R
import com.simplemobiletools.clock.activities.ReminderActivity
import com.simplemobiletools.clock.activities.SplashActivity
import com.simplemobiletools.clock.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.clock.databases.AppDatabase
import com.simplemobiletools.clock.helpers.*
import com.simplemobiletools.clock.interfaces.TimerDao
import com.simplemobiletools.clock.interfaces.StopwatchDao
import com.simplemobiletools.clock.models.*
import com.simplemobiletools.clock.receivers.*
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import java.util.Calendar

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.timerDb: TimerDao get() = AppDatabase.getInstance(applicationContext).TimerDao()

val Context.stopwatchDb: StopwatchDao get() = AppDatabase.getInstance(applicationContext).StopwatchDao()
val Context.stopwatchHelper: StopwatchHelper get() = StopwatchHelper(this)

fun Context.getFormattedDate(calendar: Calendar): String {
    val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7    // make sure index 0 means monday
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH)

    val dayString = resources.getStringArray(com.simplemobiletools.commons.R.array.week_days_short)[dayOfWeek]
    val monthString = resources.getStringArray(com.simplemobiletools.commons.R.array.months)[month]
    return "$dayString, $dayOfMonth $monthString"
}

fun Context.getEditedTimeZonesMap(): HashMap<Int, String> {
    val editedTimeZoneTitles = config.editedTimeZoneTitles
    val editedTitlesMap = HashMap<Int, String>()
    editedTimeZoneTitles.forEach {
        val parts = it.split(EDITED_TIME_ZONE_SEPARATOR.toRegex(), 2)
        editedTitlesMap[parts[0].toInt()] = parts[1]
    }
    return editedTitlesMap
}

fun Context.getAllTimeZonesModified(): ArrayList<MyTimeZone> {
    val timeZones = getAllTimeZones()
    val editedTitlesMap = getEditedTimeZonesMap()
    timeZones.forEach {
        if (editedTitlesMap.keys.contains(it.id)) {
            it.title = editedTitlesMap[it.id]!!
        } else {
            it.title = it.title.substring(it.title.indexOf(' ')).trim()
        }
    }
    return timeZones
}

fun Context.getModifiedTimeZoneTitle(id: Int) = getAllTimeZonesModified().firstOrNull { it.id == id }?.title ?: getDefaultTimeZoneTitle(id)

fun Context.createNewAlarm(timeInMinutes: Int, weekDays: Int): Alarm {
    val defaultAlarmSound = getDefaultAlarmSound(RingtoneManager.TYPE_ALARM)
    return Alarm(0, timeInMinutes, weekDays, false, false, defaultAlarmSound.title, defaultAlarmSound.uri, "")
}

fun Context.createNewTimer(): Timer {
    return Timer(
        null,
        config.timerSeconds,
        TimerState.Idle,
        config.timerVibrate,
        config.timerSoundUri,
        config.timerSoundTitle,
        config.timerLabel ?: "",
        System.currentTimeMillis(),
        config.timerChannelId,
    )
}

fun Context.createNewListLap(laps: ArrayList<Lap>, id: Int): ArrayList<Stopwatch> {
    val stopwatches = ArrayList<Stopwatch>()
    //catch (e: Exception) {0}
    var size = laps.size - 1
    laps.drop(1).forEachIndexed { index, lap ->
        val stopwatch = Stopwatch(
            0,
            id,
            lap.totalTime,
            lap.lapTime,
            lap.textTime,
            config.stopwatchLabel ?: "",
            System.currentTimeMillis(),
            config.stopwatchChannelId,
            size - index
        )
        stopwatches.add(stopwatch)
    }
    return stopwatches
}

fun Context.getOpenStopwatchTabIntent(): PendingIntent {
    val intent = getLaunchIntent() ?: Intent(this, SplashActivity::class.java)
    intent.putExtra(OPEN_TAB, TAB_STOPWATCH)
    return PendingIntent.getActivity(this, OPEN_STOPWATCH_TAB_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.hideNotification(id: Int) {
    val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.cancel(id)
}

fun Context.deleteNotificationChannel(channelId: String) {
    if (isOreoPlus()) {
        try {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.deleteNotificationChannel(channelId)
        } catch (_: Throwable) {
        }
    }
}

fun Context.hideTimerNotification(timerId: Int) = hideNotification(timerId)

fun Context.getFormattedTime(passedSeconds: Int, showSeconds: Boolean, makeAmPmSmaller: Boolean): SpannableString {
    val use24HourFormat = DateFormat.is24HourFormat(this)
    val hours = (passedSeconds / 3600) % 24
    val minutes = (passedSeconds / 60) % 60
    val seconds = passedSeconds % 60

    return if (use24HourFormat) {
        val formattedTime = formatTime(showSeconds, use24HourFormat, hours, minutes, seconds)
        SpannableString(formattedTime)
    } else {
        val formattedTime = formatTo12HourFormat(showSeconds, hours, minutes, seconds)
        val spannableTime = SpannableString(formattedTime)
        val amPmMultiplier = if (makeAmPmSmaller) 0.4f else 1f
        spannableTime.setSpan(RelativeSizeSpan(amPmMultiplier), spannableTime.length - 3, spannableTime.length, 0)
        spannableTime
    }
}

fun Context.formatTo12HourFormat(showSeconds: Boolean, hours: Int, minutes: Int, seconds: Int): String {
    val appendable = getString(if (hours >= 12) com.simplemobiletools.commons.R.string.p_m else com.simplemobiletools.commons.R.string.a_m)
    val newHours = if (hours == 0 || hours == 12) 12 else hours % 12
    return "${formatTime(showSeconds, false, newHours, minutes, seconds)} $appendable"
}

fun Context.isScreenOn() = (getSystemService(Context.POWER_SERVICE) as PowerManager).isScreenOn

fun Context.getTimerNotification(timer: Timer, pendingIntent: PendingIntent, addDeleteIntent: Boolean): Notification {
    var soundUri = timer.soundUri
    if (soundUri == SILENT) {
        soundUri = ""
    } else {
        grantReadUriPermission(soundUri)
    }

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = timer.channelId ?: "simple_timer_channel_${soundUri}_${System.currentTimeMillis()}"

    if (isOreoPlus()) {
        try {
            notificationManager.deleteNotificationChannel(channelId)
        } catch (e: Exception) {
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setLegacyStreamType(STREAM_ALARM)
            .build()

        val name = getString(R.string.timer)
        val importance = NotificationManager.IMPORTANCE_HIGH
        NotificationChannel(channelId, name, importance).apply {
            setBypassDnd(true)
            enableLights(true)
            lightColor = getProperPrimaryColor()
            setSound(Uri.parse(soundUri), audioAttributes)

            if (!timer.vibrate) {
                vibrationPattern = longArrayOf(0L)
            }

            enableVibration(timer.vibrate)
            notificationManager.createNotificationChannel(this)
        }
    }

    val title = if (timer.label.isEmpty()) {
        getString(R.string.timer)
    } else {
        timer.label
    }

    val reminderActivityIntent = getReminderActivityIntent()
    val builder = NotificationCompat.Builder(this)
        .setContentTitle(title)
        .setContentText(getString(R.string.time_expired))
        .setSmallIcon(R.drawable.ic_hourglass_vector)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setDefaults(Notification.DEFAULT_LIGHTS)
        .setCategory(Notification.CATEGORY_EVENT)
        .setAutoCancel(true)
        .setSound(Uri.parse(soundUri), STREAM_ALARM)
        .setChannelId(channelId)
        .addAction(
            com.simplemobiletools.commons.R.drawable.ic_cross_vector,
            getString(com.simplemobiletools.commons.R.string.dismiss),
            if (addDeleteIntent) {
                reminderActivityIntent
            } else {
                getHideTimerPendingIntent(timer.id!!)
            }
        )

    if (addDeleteIntent) {
        builder.setDeleteIntent(reminderActivityIntent)
    }

    builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    if (timer.vibrate) {
        val vibrateArray = LongArray(2) { 500 }
        builder.setVibrate(vibrateArray)
    }

    val notification = builder.build()
    notification.flags = notification.flags or Notification.FLAG_INSISTENT
    return notification
}

fun Context.getHideTimerPendingIntent(timerId: Int): PendingIntent {
    val intent = Intent(this, HideTimerReceiver::class.java)
    intent.putExtra(TIMER_ID, timerId)
    return PendingIntent.getBroadcast(this, timerId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.getReminderActivityIntent(): PendingIntent {
    val intent = Intent(this, ReminderActivity::class.java)
    return PendingIntent.getActivity(this, REMINDER_ACTIVITY_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.firstDayOrder(bitMask: Int): Int {
    if (bitMask == TODAY_BIT) return -2
    if (bitMask == TOMORROW_BIT) return -1

    val dayBits = arrayListOf(MONDAY_BIT, TUESDAY_BIT, WEDNESDAY_BIT, THURSDAY_BIT, FRIDAY_BIT, SATURDAY_BIT, SUNDAY_BIT)

    val sundayFirst = baseConfig.isSundayFirst
    if (sundayFirst) {
        dayBits.moveLastItemToFront()
    }

    dayBits.forEach { bit ->
        if (bitMask and bit != 0) {
            return if (bit == SUNDAY_BIT && sundayFirst) 0 else bit
        }
    }

    return bitMask
}
