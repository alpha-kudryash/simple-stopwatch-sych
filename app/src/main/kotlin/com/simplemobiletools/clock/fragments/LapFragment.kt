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
import com.simplemobiletools.clock.extensions.config
import com.simplemobiletools.clock.extensions.createNewListLap
import com.simplemobiletools.clock.extensions.stopwatchHelper
import com.simplemobiletools.clock.helpers.DisabledItemChangeAnimator
import com.simplemobiletools.clock.models.Stopwatch
import com.simplemobiletools.clock.models.StopwatchEvent
import com.simplemobiletools.clock.models.Timer
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
            /*lapAdd.setOnClickListener {
                activity?.run {
                    hideKeyboard()
                    //openEditLap(createNewTimer())
                }
            }*/
        }

        initOrUpdateAdapter()
        refreshLaps()

        // the initial timer is created asynchronously at first launch, make sure we show it once created
        if (context?.config?.appRunCount == 1) {
            Handler(Looper.getMainLooper()).postDelayed({
                refreshLaps()
            }, 1000)
        }

        return binding.root
    }

    private fun initOrUpdateAdapter() {
        if (this::lapAdapter.isInitialized) {
            lapAdapter.updatePrimaryColor()
            lapAdapter.updateBackgroundColor(requireContext().getProperBackgroundColor())
            lapAdapter.updateTextColor(requireContext().getProperTextColor())
        } else {
            lapAdapter = LapAdapter(requireActivity() as SimpleActivity, binding.lapsList, ::refreshLaps, ::openEditStopwatch)
            binding.lapsList.adapter = lapAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        requireContext().updateTextColors(binding.root)
        initOrUpdateAdapter()
        refreshLaps()
    }

    private fun refreshLaps(scrollToLatest: Boolean = false) {
        activity?.stopwatchHelper?.getLaps { laps ->
            activity?.runOnUiThread {
                lapAdapter.submitList(laps) {
                    getView()?.post {
                        if (lapPositionToScrollTo != INVALID_POSITION && lapAdapter.itemCount > lapPositionToScrollTo) {
                            binding.lapsList.scrollToPosition(lapPositionToScrollTo)
                            lapPositionToScrollTo = INVALID_POSITION
                        } else if (scrollToLatest) {
                            binding.lapsList.scrollToPosition(laps.lastIndex)
                        }
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: StopwatchEvent.Refresh) {
        refreshLaps()
    }

    fun updatePosition(lapId: Int) {
        activity?.stopwatchHelper?.getLaps { laps ->
            val position = laps.indexOfFirst { it.id == lapId }
            if (position != INVALID_POSITION) {
                activity?.runOnUiThread {
                    if (lapAdapter.itemCount > position) {
                        binding.lapsList.scrollToPosition(position)
                    } else {
                        lapPositionToScrollTo = position
                    }
                }
            }
        }
    }

    private fun openEditStopwatch(stopwatch: Stopwatch) {
        /*currentEditAlarmDialog = EditTimerDialog(activity as SimpleActivity, timer) {
            currentEditAlarmDialog = null
            refreshTimers()
        }*/
    }
}
