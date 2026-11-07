package com.simplemobiletools.clock.helpers

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.simplemobiletools.clock.models.Lap
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

class StopwatchSnapshotStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("current_stopwatch", Context.MODE_PRIVATE)

    data class Restored(val snapshot: StopwatchEngine.Snapshot, val sameBoot: Boolean)

    fun save(snapshot: StopwatchEngine.Snapshot) {
        if (snapshot.state == StopwatchEngine.State.RESETED) {
            preferences.edit().remove("snapshot").apply()
            return
        }
        val laps = JSONArray()
        snapshot.laps.forEach { lap ->
            laps.put(JSONObject().apply {
                put("id", lap.id)
                put("lapTime", lap.lapTime)
                put("totalTime", lap.totalTime)
                put("text", lap.textTime)
            })
        }
        val json = JSONObject().apply {
            put("version", 1)
            put("state", snapshot.state.name)
            put("totalTime", snapshot.totalTime)
            put("savedAt", snapshot.savedAtElapsedRealtime)
            put("lapStartTime", snapshot.lapStartTime)
            put("nextLapId", snapshot.nextLapId)
            put("setId", snapshot.currentSetId)
            put("laps", laps)
            put("bootCount", bootCount())
            put("bootEpoch", bootEpoch())
        }
        preferences.edit().putString("snapshot", json.toString()).apply()
    }

    fun load(): Restored? = runCatching {
        val json = JSONObject(preferences.getString("snapshot", null) ?: return@runCatching null)
        require(json.getInt("version") == 1)
        val rows = json.getJSONArray("laps")
        val laps = (0 until rows.length()).map { index ->
            val lap = rows.getJSONObject(index)
            Lap(lap.getInt("id"), lap.getLong("lapTime"), lap.getLong("totalTime"), lap.getString("text"))
        }
        val savedBoot = json.getInt("bootCount")
        val currentBoot = bootCount()
        // Android 6 may not expose BOOT_COUNT; an epoch estimate is a conservative fallback.
        val sameBoot = if (savedBoot >= 0 && currentBoot >= 0) savedBoot == currentBoot
            else abs(json.getLong("bootEpoch") - bootEpoch()) < 60_000L
        Restored(
            StopwatchEngine.Snapshot(
                StopwatchEngine.State.valueOf(json.getString("state")),
                json.getLong("totalTime"), json.getLong("savedAt"), json.getLong("lapStartTime"),
                json.getInt("nextLapId"), json.getInt("setId"), laps,
            ),
            sameBoot,
        )
    }.getOrNull()

    private fun bootCount() = Settings.Global.getInt(context.contentResolver, "boot_count", -1)
    private fun bootEpoch() = System.currentTimeMillis() - SystemClock.elapsedRealtime()
}
