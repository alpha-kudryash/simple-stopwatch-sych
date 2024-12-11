package com.simplemobiletools.clock.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.provider.AlarmClock
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.AnimationUtils
import com.simplemobiletools.clock.R
import com.simplemobiletools.clock.databinding.ActivityReminderBinding
import com.simplemobiletools.clock.extensions.*
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*

class ReminderActivity : SimpleActivity() {
    private val increaseVolumeHandler = Handler(Looper.getMainLooper())
    private val maxReminderDurationHandler = Handler(Looper.getMainLooper())
    private val swipeGuideFadeHandler = Handler()
    private val vibrationHandler = Handler(Looper.getMainLooper())
    private var isAlarmReminder = false
    private var didVibrate = false
    private var dragDownX = 0f
    private val binding: ActivityReminderBinding by viewBinding(ActivityReminderBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        showOverLockscreen()
        updateTextColors(binding.root)
        updateStatusbarColor(getProperBackgroundColor())

        setupButtons()
    }

    private fun setupButtons() {
        if (isAlarmReminder) {
            setupAlarmButtons()
        } else {
            setupTimerButtons()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupAlarmButtons() {
        binding.reminderStop.beGone()
        binding.reminderDraggableBackground.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulsing_animation))
        binding.reminderDraggableBackground.applyColorFilter(getProperPrimaryColor())

        val textColor = getProperTextColor()
        binding.reminderDismiss.applyColorFilter(textColor)
        binding.reminderDraggable.applyColorFilter(textColor)
        binding.reminderSnooze.applyColorFilter(textColor)

        var minDragX = 0f
        var maxDragX = 0f
        var initialDraggableX = 0f

        binding.reminderDismiss.onGlobalLayout {
            minDragX = binding.reminderSnooze.left.toFloat()
            maxDragX = binding.reminderDismiss.left.toFloat()
            initialDraggableX = binding.reminderDraggable.left.toFloat()
        }

        binding.reminderDraggable.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragDownX = event.x
                    binding.reminderDraggableBackground.animate().alpha(0f)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragDownX = 0f
                    if (!didVibrate) {
                        binding.reminderDraggable.animate().x(initialDraggableX).withEndAction {
                            binding.reminderDraggableBackground.animate().alpha(0.2f)
                        }

                        binding.reminderGuide.animate().alpha(1f).start()
                        swipeGuideFadeHandler.removeCallbacksAndMessages(null)
                        swipeGuideFadeHandler.postDelayed({
                            binding.reminderGuide.animate().alpha(0f).start()
                        }, 2000L)
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    binding.reminderDraggable.x = Math.min(maxDragX, Math.max(minDragX, event.rawX - dragDownX))
                    if (binding.reminderDraggable.x >= maxDragX - 50f) {
                        if (!didVibrate) {
                            binding.reminderDraggable.performHapticFeedback()
                            didVibrate = true
                        }

                        if (isOreoPlus()) {
                            notificationManager.cancelAll()
                        }
                    } else if (binding.reminderDraggable.x <= minDragX + 50f) {
                        if (!didVibrate) {
                            binding.reminderDraggable.performHapticFeedback()
                            didVibrate = true
                        }

                        if (isOreoPlus()) {
                            notificationManager.cancelAll()
                        }
                    }
                }
            }
            true
        }
    }

    private fun setupTimerButtons() {
        binding.reminderStop.background = resources.getColoredDrawableWithColor(R.drawable.circle_background_filled, getProperPrimaryColor())
        arrayOf(binding.reminderSnooze, binding.reminderDraggableBackground, binding.reminderDraggable, binding.reminderDismiss).forEach {
            it.beGone()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        increaseVolumeHandler.removeCallbacksAndMessages(null)
        maxReminderDurationHandler.removeCallbacksAndMessages(null)
        swipeGuideFadeHandler.removeCallbacksAndMessages(null)
        vibrationHandler.removeCallbacksAndMessages(null)
    }

    private fun showOverLockscreen() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        if (isOreoMr1Plus()) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }
}
