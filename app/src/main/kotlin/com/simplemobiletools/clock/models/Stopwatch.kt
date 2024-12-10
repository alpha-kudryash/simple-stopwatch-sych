package com.simplemobiletools.clock.models

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "stopwatch_sets")
data class StopwatchSet(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val label: String,
    val createdAt: Long
)

@Entity(tableName = "laps",
    foreignKeys = [
        ForeignKey(
            entity = StopwatchSet::class,
            parentColumns = ["id"],
            childColumns = ["stopwatchSetId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Keep
data class Stopwatch(
    @PrimaryKey(autoGenerate = true) var id: Int?,
    val stopwatchSetId: Int,
    var milliseconds: Long,
    var text: String,
    var label: String,
    var createdAt: Long,
    var channelId: String? = null,
    var oneShot: Boolean = false,
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
    var h: Boolean = false
) {
    fun toStopwatch() = Stopwatch(a, b, c, d, e, f, g, h)
}
