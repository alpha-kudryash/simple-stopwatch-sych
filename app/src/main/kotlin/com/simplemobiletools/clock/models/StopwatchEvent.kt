package com.simplemobiletools.clock.models

import com.simplemobiletools.clock.helpers.INVALID_STOPWATCH_ID

sealed class StopwatchEvent(open val timerId: Int) {
    data class Delete(override val timerId: Int) : StopwatchEvent(timerId)
    data class Reset(override val timerId: Int) : StopwatchEvent(timerId)
    data class Start(override val timerId: Int, val duration: Long) : StopwatchEvent(timerId)
    data class Pause(override val timerId: Int, val duration: Long) : StopwatchEvent(timerId)
    data class Finish(override val timerId: Int, val duration: Long) : StopwatchEvent(timerId)
    object Refresh : StopwatchEvent(INVALID_STOPWATCH_ID)
}
