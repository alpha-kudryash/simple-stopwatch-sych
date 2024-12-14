package com.simplemobiletools.clock.models

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "laps")
@Keep
data class Stopwatch(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var stopwatchSetNum: Int,
    var milliseconds: Long,
    var text: String,
    var label: String,
    var createdAt: Long,
    var channelId: String? = null,
)

@Keep
data class ObfuscatedStopwatch(
    var a: Int?,
    var b: Int,
    var c: Long,
    val d: String,
    var e: String,
    var f: Long,
    var g: String? = null,
) {
    fun toStopwatch() = Stopwatch(a ?: 0, b, c, d, e, f, g)
}
