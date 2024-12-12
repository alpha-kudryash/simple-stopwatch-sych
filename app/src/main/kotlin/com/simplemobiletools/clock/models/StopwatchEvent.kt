package com.simplemobiletools.clock.models

import com.simplemobiletools.clock.helpers.INVALID_STOPWATCH_ID

sealed class StopwatchEvent(open val lapId: Int) {
    data class Delete(override val lapId: Int) : StopwatchEvent(lapId)
    data class Reset(override val lapId: Int) : StopwatchEvent(lapId)
    data class Start(override val lapId: Int, val duration: Long) : StopwatchEvent(lapId)
    data class Pause(override val lapId: Int, val duration: Long) : StopwatchEvent(lapId)
    data class Finish(override val lapId: Int, val duration: Long) : StopwatchEvent(lapId)
    object Refresh : StopwatchEvent(INVALID_STOPWATCH_ID)
}
