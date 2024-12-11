package com.simplemobiletools.clock.activities

import android.content.Intent
import com.simplemobiletools.clock.helpers.*
import com.simplemobiletools.commons.activities.BaseSplashActivity

class SplashActivity : BaseSplashActivity() {
    override fun initActivity() {
        when {
            intent?.action == "android.intent.action.SHOW_LAPS" -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra(OPEN_TAB, TAB_LAP)
                    startActivity(this)
                }
            }

            intent?.action == STOPWATCH_TOGGLE_ACTION -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra(OPEN_TAB, TAB_STOPWATCH)
                    putExtra(TOGGLE_STOPWATCH, intent.getBooleanExtra(TOGGLE_STOPWATCH, false))
                    startActivity(this)
                }
            }

            intent.extras?.containsKey(OPEN_TAB) == true -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra(OPEN_TAB, intent.getIntExtra(OPEN_TAB, TAB_STOPWATCH))
                    putExtra(LAP_ID, intent.getIntExtra(LAP_ID, INVALID_LAP_ID))
                    startActivity(this)
                }
            }
        }
        finish()
    }
}
