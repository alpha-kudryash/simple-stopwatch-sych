package com.simplemobiletools.clock.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.simplemobiletools.clock.activities.SimpleActivity
import com.simplemobiletools.clock.adapters.LapAdapter
import com.simplemobiletools.clock.databinding.FragmentLapBinding
import com.simplemobiletools.clock.databinding.FragmentTimerBinding
import com.simplemobiletools.clock.dialogs.EditTimerDialog
import com.simplemobiletools.clock.extensions.config
import com.simplemobiletools.clock.extensions.createNewTimer
import com.simplemobiletools.clock.helpers.DisabledItemChangeAnimator
import com.simplemobiletools.clock.models.Stopwatch
import com.simplemobiletools.clock.models.Timer
import com.simplemobiletools.clock.models.TimerEvent
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.models.AlarmSound
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class LapFragment : Fragment() {
    private val INVALID_POSITION = -1
    private lateinit var binding: FragmentLapBinding
    private lateinit var lapAdapter: LapAdapter
    private var lapPositionToScrollTo = INVALID_POSITION
    private var currentEditAlarmDialog: EditTimerDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLapBinding.inflate(inflater, container, false).apply {
            lapsList.itemAnimator = DisabledItemChangeAnimator()
            lapAdd.setOnClickListener {
                activity?.run {
                    hideKeyboard()
                    //openEditTimer(createNewTimer())
                }
            }
        }

        initOrUpdateAdapter()
        refreshTimers()

        return binding.root
    }

    private fun initOrUpdateAdapter() {
        if (this::lapAdapter.isInitialized) {
            lapAdapter.updatePrimaryColor()
            lapAdapter.updateBackgroundColor(requireContext().getProperBackgroundColor())
            lapAdapter.updateTextColor(requireContext().getProperTextColor())
        } else {
            lapAdapter = LapAdapter(requireActivity() as SimpleActivity, binding.lapsList, ::refreshTimers, ::openEditTimer)
            binding.lapsList.adapter = lapAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        requireContext().updateTextColors(binding.root)
        initOrUpdateAdapter()
        refreshTimers()
    }

    private fun refreshTimers(scrollToLatest: Boolean = false) {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: TimerEvent.Refresh) {
        refreshTimers()
    }

    fun updateAlarmSound(alarmSound: AlarmSound) {
        currentEditAlarmDialog?.updateAlarmSound(alarmSound)
    }

    fun updatePosition(timerId: Int) {
    }

    private fun openEditTimer(stopwatch: Stopwatch) {
        /*currentEditAlarmDialog = EditTimerDialog(activity as SimpleActivity, stopwatch) {
            currentEditAlarmDialog = null
            refreshTimers()
        }*/
    }
}
