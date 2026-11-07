package com.simplemobiletools.clock.databases

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.simplemobiletools.clock.models.Stopwatch
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class StopwatchDatabaseTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private fun oldDatabase(version: Int, legacyLaps: Boolean = false): String {
        val name = "migration-${UUID.randomUUID()}.db"
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        val schema = javaClass.classLoader!!.getResourceAsStream(
            "com.simplemobiletools.clock.databases.AppDatabase/$version.json"
        )!!.bufferedReader().use { it.readText() }
        val entities = JSONObject(schema).getJSONObject("database").getJSONArray("entities")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                var sql = entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName"))
                if (legacyLaps && entity.getString("tableName") == "laps") {
                    sql = sql.replace(", `millisecondsLag` INTEGER NOT NULL", "")
                        .replace(", `sequence` INTEGER NOT NULL", "")
                }
                db.execSQL(sql)
            }
            val oneShot = if (version >= 2) ",oneShot" else ""
            val oneShotValue = if (version >= 2) ",0" else ""
            db.execSQL("INSERT INTO timers (id,seconds,state,vibrate,soundUri,soundTitle,label,createdAt$oneShot) VALUES (1,60,'{}',0,'','','keep timer',1$oneShotValue)")
            if (version == 3) {
                if (legacyLaps) {
                    db.execSQL("INSERT INTO laps VALUES (1,1,1234,'keep text','keep label',1,NULL)")
                    db.execSQL("INSERT INTO laps VALUES (2,1,2000,'second','keep label',1,NULL)")
                } else {
                    db.execSQL("INSERT INTO laps VALUES (1,1,1234,1234,'keep text','keep label',1,NULL,1)")
                }
            }
            db.version = version
        }
        return name
    }

    private fun open(name: String) = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
        .allowMainThreadQueries()
        .build()

    @Test fun migrateVersionOnePreservesTimerAndCreatesUsableLaps() = verifyMigration(1)
    @Test fun migrateVersionTwoPreservesTimerAndCreatesUsableLaps() = verifyMigration(2)

    private fun verifyMigration(version: Int) {
        val name = oldDatabase(version)
        open(name).withDatabase { db ->
            val connection = db.openHelper.writableDatabase // Room validates the migrated schema here.
            connection.query("SELECT label FROM timers WHERE id=1").use {
                assertTrue(it.moveToFirst())
                assertEquals("keep timer", it.getString(0))
            }
            assertTrue(db.StopwatchDao().getLaps().isEmpty())
            val (setId, ids) = db.StopwatchDao().saveSet(listOf(lap()))
            assertEquals(1, setId)
            assertEquals(1, ids.size)
        }
        context.deleteDatabase(name)
    }

    @Test fun versionThreeHistoryOpensWithoutDestructiveMigration() {
        val name = oldDatabase(3)
        open(name).withDatabase { db ->
            val saved = db.StopwatchDao().getLaps().single()
            assertEquals("keep text", saved.text)
            assertEquals("keep label", saved.label)
            assertEquals(1234L, saved.milliseconds)
        }
        context.deleteDatabase(name)
    }

    @Test fun olderVersionThreeLayoutGetsMissingColumnsWithoutLosingHistory() {
        val name = oldDatabase(3, legacyLaps = true)
        open(name).withDatabase { db ->
            val saved = db.StopwatchDao().getLaps()
            assertEquals(listOf("keep text", "second"), saved.map { it.text })
            assertEquals(listOf(1234L, 766L), saved.map { it.millisecondsLag })
            assertEquals(listOf(1, 2), saved.map { it.sequence })
        }
        context.deleteDatabase(name)
    }

    @Test fun savingAgainReplacesTheSameSetAndAllocatesANewNumberOnlyForANewMeasurement() {
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build().withDatabase { db ->
            val dao = db.StopwatchDao()
            assertEquals(0 to emptyList<Long>(), dao.saveSet(emptyList()))
            val (setId, _) = dao.saveSet(listOf(lap()))
            assertEquals("1", dao.getLaps().single().label)
            dao.saveSet(listOf(lap().copy(stopwatchSetNum = setId, text = "updated")))
            assertEquals(1, dao.getLaps().size)
            assertEquals("updated", dao.getLaps().single().text)
            assertEquals(2, dao.saveSet(listOf(lap())).first)
            assertEquals(2, dao.getLaps().size)
        }
    }

    @Test fun replacementRollsBackDeletionIfAnInsertFails() {
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build().withDatabase { db ->
            val dao = db.StopwatchDao()
            val setId = dao.saveSet(listOf(lap().copy(text = "original"))).first
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_new_laps BEFORE INSERT ON laps BEGIN SELECT RAISE(ABORT, 'test failure'); END")
            try {
                dao.saveSet(listOf(lap().copy(stopwatchSetNum = setId, text = "replacement")))
                fail("Expected insert failure")
            } catch (_: android.database.SQLException) {
                assertEquals("original", dao.getLaps().single().text)
            }
        }
    }

    private inline fun <T> AppDatabase.withDatabase(block: (AppDatabase) -> T): T {
        return try {
            block(this)
        } finally {
            close()
        }
    }

    private fun lap() = Stopwatch(
        stopwatchSetNum = 0, milliseconds = 1234L, millisecondsLag = 1234L,
        text = "", label = "", createdAt = 1L, sequence = 1,
    )
}
