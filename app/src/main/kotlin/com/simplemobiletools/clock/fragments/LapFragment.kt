package com.simplemobiletools.clock.fragments

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.clock.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.simplemobiletools.clock.activities.SimpleActivity
import com.simplemobiletools.clock.adapters.LapAdapter
import com.simplemobiletools.clock.databinding.FragmentLapBinding
import com.simplemobiletools.clock.dialogs.EditTimerDialog
import com.simplemobiletools.clock.extensions.stopwatchHelper
import com.simplemobiletools.clock.helpers.DisabledItemChangeAnimator
import com.simplemobiletools.clock.models.Stopwatch
import com.simplemobiletools.clock.models.StopwatchEvent
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.updateTextColors
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
            lapDelete.setOnClickListener {
                activity?.run {
                    hideKeyboard()
                    openWarningDeleteAll{
                        stopwatchHelper.deleteAllLaps {
                            activity?.runOnUiThread { refreshLaps() }
                        }
                    }
                }
            }
        }

        initOrUpdateAdapter()
        refreshLaps()

        return binding.root
    }

    private fun initOrUpdateAdapter() {
        if (this::lapAdapter.isInitialized) {
            lapAdapter.updatePrimaryColor()
            lapAdapter.updateBackgroundColor(requireContext().getProperBackgroundColor())
            lapAdapter.updateTextColor(requireContext().getProperTextColor())
        } else {
            lapAdapter = LapAdapter(requireActivity() as SimpleActivity, ArrayList(), binding.lapsList, ::refreshLaps, ::openEditTimer)
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
                    lapAdapter.updateItems(laps)
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
        /*lapAdapter.apply {
            updatePrimaryColor()
            updateBackgroundColor(requireContext().getProperBackgroundColor())
            updateTextColor(requireContext().getProperTextColor())
            updateItems(activity?.stopwatchHelper?.getLaps)
            binding.stopwatchSave.beVisibleIf(CurrentStopwatch.laps.isNotEmpty())
        }*/
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: StopwatchEvent.Refresh) {
        refreshLaps()
    }

    fun updatePosition() {
        activity?.stopwatchHelper?.getLaps { laps ->
            val position = laps.lastIndex// { it.id == lapId }
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

    fun updateLaps() {
        refreshLaps()
    }

    private fun openEditTimer(stopwatch: Stopwatch) {

    }

    private fun openWarningDeleteAll(onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete_all_laps_title)
            .setMessage(R.string.confirm_delete_all_laps_message)
            .setPositiveButton(R.string.delete_laps_action) { _, _ ->
                onConfirm() // Выполняем переданную функцию, если пользователь согласился
            }
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null) // Просто закрываем диалог, если пользователь отказался
            .show()
        /*currentEditAlarmDialog = EditTimerDialog(activity as SimpleActivity, stopwatch) {
            currentEditAlarmDialog = null
            refreshTimers()
        }*/
    }
}
