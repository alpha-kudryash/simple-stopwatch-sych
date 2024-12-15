package com.simplemobiletools.clock.extensions

import android.text.format.DateFormat
import com.simplemobiletools.commons.extensions.getFormattedDuration
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

fun Long.formatStopwatchTime(): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) - TimeUnit.HOURS.toMinutes(hours)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(this))
    var ms = this % 1000 / 10

       // ms /= 10

    if (hours > 0) {
        val format = "%02d:%02d:%02d.%02d"
        return        String.format(format, hours, minutes, seconds, ms) }
    else {
        val format = "%02d:%02d.%02d"
        return        String.format(format, minutes, seconds, ms)
    }
        /*}

        minutes > 0 -> {
            val format = "%02d:%02d.$MSFormat"
            String.format(format, minutes, seconds, ms)
        }

        else -> {
            val format = "%d.$MSFormat"
            String.format(format, seconds, ms)
        }*/

}

fun Long.formatStopwatchLag(): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) - TimeUnit.HOURS.toMinutes(hours)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(this))
    var ms = this % 1000 / 10

    return when {
        hours > 0 -> {
            val format = "%02d:%02d:%02d.%02d"
            return String.format(format, hours, minutes, seconds, ms)
        }

        minutes > 0 -> {
            val format = "%02d:%02d.%02d"
            String.format(format, minutes, seconds, ms)
        }

        else -> {
            val format = "%02d.%02d"
            return String.format(format, seconds, ms)
        }
    }
}

fun Long.timestampFormat(format: String = "dd. MM. yyyy"): String {
    val calendar = Calendar.getInstance(Locale.getDefault())
    calendar.timeInMillis = this

    return DateFormat.format(format, calendar).toString()
}

/*fun Long.getFormattedDuration(seconds: Int, milliseconds: Long, forceShowHours: Boolean): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    val millis = milliseconds / 10

    return if (forceShowHours || hours > 0) {
        String.format("%02d:%02d:%02d:%02d", hours, minutes, remainingSeconds, millis)
    } else {
        String.format("%02d:%02d:%02d", minutes, remainingSeconds, millis)
    }
    //return this.div(1000F).roundToInt().getFormattedDuration(forceShowHours)
}

fun Long.getFormattedDuration(forceShowHours: Boolean = false): String {
    val seconds = this / 1000
    val milliseconds = this % 1000
    return seconds.getFormattedDuration(seconds, milliseconds, forceShowHours)
    //return this.div(1000F).roundToInt().getFormattedDuration(forceShowHours)
}
*/
fun Long.getFormattedDuration(): String {
    val hours = this / 3600000
    val minutes = (this % 3600000) / 60000
    val seconds = (this % 60000) / 1000
    val milliseconds = this % 1000

    return String.format("%02d:%02d:%02d:%02d", hours, minutes, seconds, milliseconds / 10)
}

val Long.secondsToMillis get() = TimeUnit.SECONDS.toMillis(this)
val Long.millisToSeconds get() = TimeUnit.MILLISECONDS.toSeconds(this)
