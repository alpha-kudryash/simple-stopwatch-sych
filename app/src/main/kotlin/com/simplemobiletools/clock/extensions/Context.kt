package com.simplemobiletools.clock.extensions

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager.STREAM_ALARM
import android.net.Uri
import android.os.PowerManager
import android.text.SpannableString
import android.text.format.DateFormat
import android.text.style.RelativeSizeSpan
import androidx.core.app.NotificationCompat
import com.simplemobiletools.clock.R
import com.simplemobiletools.clock.activities.ReminderActivity
import com.simplemobiletools.clock.activities.SplashActivity
import com.simplemobiletools.clock.databases.AppDatabase
import com.simplemobiletools.clock.helpers.*
import com.simplemobiletools.clock.interfaces.TimerDao
import com.simplemobiletools.clock.interfaces.StopwatchDao
import com.simplemobiletools.clock.models.*
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import java.util.Calendar

val Context.config: Config get() = Config.newInstance(applicationContext)

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

fun Context.createNewListLap(laps: ArrayList<Lap>): ArrayList<Stopwatch> {
    val stopwatches = ArrayList<Stopwatch>()
    val setNum = try { stopwatchDb.getMaxSetNum() ?: 0 }
    catch (e: Exception) {0}
    laps.forEachIndexed { index, lap ->
            val stopwatch = Stopwatch(
                0,
                setNum + 1,
                lap.totalTime,
                lap.textTime ?:"",
                config.stopwatchLabel ?: "",
                System.currentTimeMillis(),
                config.stopwatchChannelId
        )
        stopwatches.add(stopwatch)
    }
    return stopwatches
}

fun Context.getOpenLapTabIntent(lapId: Int): PendingIntent {
    val intent = getLaunchIntent() ?: Intent(this, SplashActivity::class.java)
    intent.putExtra(OPEN_TAB, TAB_LAP)
    intent.putExtra(LAP_ID, lapId)
    return PendingIntent.getActivity(this, lapId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
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

/*fun Context.getHideTimerPendingIntent(lapId: Int): PendingIntent {
    val intent = Intent(this, HideTimerReceiver::class.java)
    intent.putExtra(LAP_ID, timerId)
    return PendingIntent.getBroadcast(this, timerId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}*/

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
