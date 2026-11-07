package com.simplemobiletools.clock.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.simplemobiletools.clock.extensions.config
import com.simplemobiletools.clock.helpers.Converters
import com.simplemobiletools.clock.interfaces.TimerDao
import com.simplemobiletools.clock.interfaces.StopwatchDao
import com.simplemobiletools.clock.models.Timer
import com.simplemobiletools.clock.models.Stopwatch
import com.simplemobiletools.clock.models.TimerState
import java.util.concurrent.Executors

@Database(entities = [Timer::class, Stopwatch::class], version = 4)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun TimerDao(): TimerDao
    abstract fun StopwatchDao(): StopwatchDao

    companion object {
        @Volatile
        private var db: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (db == null) {
                synchronized(AppDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "app.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            .addCallback(object : Callback() {
                                override fun onCreate(db: SupportSQLiteDatabase) {
                                    super.onCreate(db)
                                    insertDefaultTimer(context)
                                }
                            })
                            .build()
                    }
                }
            }
            return db!!
        }

        private fun insertDefaultTimer(context: Context) {
            Executors.newSingleThreadScheduledExecutor().execute {
                val config = context.config
                db!!.TimerDao().insertOrUpdateTimer(
                    Timer(
                        id = null,
                        seconds = config.timerSeconds,
                        state = TimerState.Idle,
                        vibrate = config.timerVibrate,
                        soundUri = config.timerSoundUri,
                        soundTitle = config.timerSoundTitle,
                        label = config.timerLabel ?: "",
                        createdAt = System.currentTimeMillis(),
                        channelId = config.timerChannelId,
                    )
                )
            }
        }

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `timers` ADD COLUMN `oneShot` INTEGER NOT NULL DEFAULT 0")
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `laps` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `stopwatchSetNum` INTEGER NOT NULL,
                        `milliseconds` INTEGER NOT NULL,
                        `millisecondsLag` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `channelId` TEXT,
                        `sequence` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Earlier releases changed the laps columns without incrementing database version 3.
        // Accept both layouts and preserve all existing rows when upgrading them to version 4.
        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = mutableSetOf<String>()
                db.query("PRAGMA table_info(`laps`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    while (cursor.moveToNext()) columns.add(cursor.getString(nameIndex))
                }
                if ("millisecondsLag" !in columns) {
                    db.execSQL("ALTER TABLE `laps` ADD COLUMN `millisecondsLag` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("""
                        UPDATE `laps` SET `millisecondsLag` = `milliseconds` - COALESCE((
                            SELECT MAX(previous.milliseconds) FROM laps AS previous
                            WHERE previous.stopwatchSetNum = laps.stopwatchSetNum
                            AND (previous.milliseconds < laps.milliseconds
                                OR (previous.milliseconds = laps.milliseconds AND previous.id < laps.id))
                        ), 0)
                    """.trimIndent())
                }
                if ("sequence" !in columns) {
                    db.execSQL("ALTER TABLE `laps` ADD COLUMN `sequence` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("""
                        UPDATE `laps` SET `sequence` = (
                            SELECT COUNT(*) FROM laps AS previous
                            WHERE previous.stopwatchSetNum = laps.stopwatchSetNum
                            AND (previous.milliseconds < laps.milliseconds
                                OR (previous.milliseconds = laps.milliseconds AND previous.id <= laps.id))
                        )
                    """.trimIndent())
                }
            }
        }
    }
}
