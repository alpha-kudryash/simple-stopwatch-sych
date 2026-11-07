package com.simplemobiletools.clock.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.simplemobiletools.clock.R
import com.simplemobiletools.clock.activities.SimpleActivity
import com.simplemobiletools.clock.adapters.StopwatchAdapter
import com.simplemobiletools.clock.adapters.LapAdapter
import com.simplemobiletools.clock.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.clock.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.clock.commons.extensions.getProperTextColor
import com.simplemobiletools.clock.databinding.FragmentStopwatchBinding
import com.simplemobiletools.clock.dialogs.EditStopwatchDialog
import com.simplemobiletools.clock.extensions.*
import com.simplemobiletools.clock.models.Lap
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.clock.models.Stopwatch
import com.simplemobiletools.clock.helpers.*
import com.simplemobiletools.commons.dialogs.PermissionRequiredDialog

class StopwatchFragment : Fragment() {

    private lateinit var stopwatchAdapter: StopwatchAdapter
    private lateinit var stopwatchHelper: StopwatchHelper
    private lateinit var binding: FragmentStopwatchBinding
    private var currentEditStopwatchDialog: EditStopwatchDialog? = null
    private lateinit var lapAdapter: LapAdapter
    private var resetDialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val sorting = requireContext().config.stopwatchLapsSort
        Lap.sorting = sorting
        binding = FragmentStopwatchBinding.inflate(inflater, container, false).apply {
            stopwatchTime.setOnClickListener {
                togglePlayLap()
            }

            stopwatchPlayLap.setOnClickListener {
                startLapStopwatch()
            }

            stopwatchPauseReset.setOnClickListener {
                pauseResetStopwatch()
            }

            stopwatchSortingIndicator1.setOnClickListener {
                changeSorting(SORT_BY_LAP)
            }

            stopwatchSortingIndicator2.setOnClickListener {
                changeSorting(SORT_BY_LAP_TIME)
            }

            stopwatchSortingIndicator3.setOnClickListener {
                changeSorting(SORT_BY_TOTAL_TIME)
            }

            stopwatchSave.setOnClickListener {
                stopwatchSortingIndicatorsHolder.beVisible()
                saveLaps()
                updateLaps()
            }

            stopwatchAdapter = StopwatchAdapter(activity as SimpleActivity, ArrayList(), stopwatchList) {
                if (it is Int) {
                    changeSorting(it)
                }
            }
            stopwatchList.adapter = stopwatchAdapter
        }

        updateSortingIndicators(sorting)
        return binding.root
    }

    private fun initOrUpdateAdapter() {
        if (this::stopwatchAdapter.isInitialized) {
            stopwatchAdapter.updatePrimaryColor()
            stopwatchAdapter.updateBackgroundColor(requireContext().getProperBackgroundColor())
            stopwatchAdapter.updateTextColor(requireContext().getProperTextColor())
        } else {
            //stopwatchAdapter = TimerAdapter(requireActivity() as SimpleActivity, binding.timersList, ::refreshTimers, ::openEditTimer)
            //binding.timersList.adapter = stopwatchAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        setupViews()

        CurrentStopwatch.addUpdateListener(updateListener)
        updateLaps()
        binding.stopwatchSortingIndicatorsHolder.beVisibleIf(CurrentStopwatch.laps.size > 1)
        if (CurrentStopwatch.laps.size > 1) {
            updateSorting(Lap.sorting)
        }

        if (requireContext().config.toggleStopwatch) {
            requireContext().config.toggleStopwatch = false
            startLapStopwatch()
        }
    }

    override fun onPause() {
        super.onPause()
        CurrentStopwatch.removeUpdateListener(updateListener)
    }

    private fun setupViews() {
        val properPrimaryColor = requireContext().getProperPrimaryColor()
        binding.apply {
            requireContext().updateTextColors(stopwatchFragment)
            stopwatchPlayLap.background = resources.getColoredDrawableWithColor(R.drawable.circle_background_filled, properPrimaryColor)
            stopwatchPauseReset.applyColorFilter(requireContext().getProperTextColor())
        }
    }

    private fun updateStartLapIcon(state: StopwatchEngine.State) {
        val drawableId =
            if (state == StopwatchEngine.State.RUNNING) com.simplemobiletools.commons.R.drawable.ic_camera_vector else com.simplemobiletools.commons.R.drawable.ic_play_vector
        val iconColor = if (requireContext().getProperPrimaryColor() == Color.WHITE) Color.BLACK else Color.WHITE
        binding.stopwatchPlayLap.setImageDrawable(resources.getColoredDrawableWithColor(drawableId, iconColor))
    }

    private fun updatePauseResetIcon(state: StopwatchEngine.State) {
        val drawableId =
            if (state == StopwatchEngine.State.RUNNING) com.simplemobiletools.commons.R.drawable.ic_pause_vector else com.simplemobiletools.commons.R.drawable.ic_reset_vector
        val iconColor = if (requireContext().getProperPrimaryColor() == Color.WHITE) Color.BLACK else Color.WHITE
        binding.stopwatchPauseReset.setImageDrawable(resources.getColoredDrawableWithColor(drawableId, iconColor))
    }

    private fun togglePlayLap() {
        // Start at the button event, not after the notification permission dialog closes.
        CurrentStopwatch.start()
        updateLaps()
        (activity as SimpleActivity).handleNotificationPermission { granted ->
            if (!granted) {
                PermissionRequiredDialog(
                    activity as SimpleActivity,
                    com.simplemobiletools.commons.R.string.allow_notifications_reminders,
                    { (activity as SimpleActivity).openNotificationSettings() })
            }
        }
    }

    private fun updateDisplayedText(totalTime: Long, lapTime: Long) {
        binding.stopwatchTime.text = totalTime.formatStopwatchTime()
    }

    private fun pauseResetStopwatch() {
        if (CurrentStopwatch.state == StopwatchEngine.State.PAUSED) {
            confirmResetStopwatch()
        }
        if (CurrentStopwatch.state == StopwatchEngine.State.RUNNING) {
            CurrentStopwatch.pause()
        }
    }

    private fun confirmResetStopwatch() {
        if (resetDialog?.isShowing == true) return
        resetDialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_stopwatch_reset_title)
            .setMessage(R.string.confirm_stopwatch_reset_message)
            .setPositiveButton(R.string.reset_stopwatch_action) { _, _ ->
                CurrentStopwatch.reset()
                updateLaps()
                binding.stopwatchSortingIndicatorsHolder.beInvisible()
            }
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)
            .create().also { dialog ->
                dialog.setOnDismissListener { resetDialog = null }
                dialog.show()
            }
    }

    override fun onDestroyView() {
        resetDialog?.dismiss()
        super.onDestroyView()
    }

    fun pauseStopwatch() {
        if (CurrentStopwatch.state == StopwatchEngine.State.RUNNING) {
            CurrentStopwatch.pause()
        }
    }

    fun defaultSorting() {
        if (Lap.sorting and SORT_DESCENDING != 0) {
            updateSorting(Lap.sorting.flipBit(SORT_DESCENDING))
        }
    }

    private fun changeSorting(clickedValue: Int) {
        val sorting = if (Lap.sorting and clickedValue != 0) {
            Lap.sorting.flipBit(SORT_DESCENDING)
        } else {
            clickedValue or SORT_DESCENDING
        }
        updateSorting(sorting)
    }

    private fun updateSorting(sorting: Int) {
        updateSortingIndicators(sorting)
        Lap.sorting = sorting
        requireContext().config.stopwatchLapsSort = sorting
        updateLaps()
    }

    private fun updateSortingIndicators(sorting: Int) {
        var bitmap = requireContext().resources.getColoredBitmap(R.drawable.ic_sorting_triangle_vector, requireContext().getProperPrimaryColor())
        binding.apply {
            stopwatchSortingIndicator1.beInvisibleIf(sorting and SORT_BY_LAP == 0)
            stopwatchSortingIndicator2.beInvisibleIf(sorting and SORT_BY_LAP_TIME == 0)
            stopwatchSortingIndicator3.beInvisibleIf(sorting and SORT_BY_TOTAL_TIME == 0)

            val activeIndicator = when {
                sorting and SORT_BY_LAP != 0 -> stopwatchSortingIndicator1
                sorting and SORT_BY_LAP_TIME != 0 -> stopwatchSortingIndicator2
                else -> stopwatchSortingIndicator3
            }

            if (sorting and SORT_DESCENDING == 0) {
                val matrix = Matrix()
                matrix.postScale(1f, -1f)
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            activeIndicator.setImageBitmap(bitmap)
        }
    }

    fun startLapStopwatch() {
        when (CurrentStopwatch.state) {
            StopwatchEngine.State.RESETED -> {
                togglePlayLap()
            }
            StopwatchEngine.State.PAUSED -> togglePlayLap()
            StopwatchEngine.State.RUNNING -> {
                CurrentStopwatch.lap()
                binding.stopwatchSortingIndicatorsHolder.beVisible()
                updateLaps()
            }
            else -> {}
        } // todo add pause
    }

    fun keyUpStartLapStopwatch() {
        when (CurrentStopwatch.state) {
            StopwatchEngine.State.RESETED -> togglePlayLap()
            StopwatchEngine.State.PAUSED -> togglePlayLap()
            StopwatchEngine.State.RUNNING -> {
                CurrentStopwatch.lap()
                binding.stopwatchSortingIndicatorsHolder.beVisible()
                updateLaps()
            }
            else -> {}
        }
    }

    private fun updateLaps() {
        stopwatchAdapter.apply {
            updatePrimaryColor()
            updateBackgroundColor(requireContext().getProperBackgroundColor())
            updateTextColor(requireContext().getProperTextColor())
            updateItems(ArrayList(CurrentStopwatch.laps.drop(1)))
            binding.stopwatchList.post {
                scrollToLastLap()
                stopwatchAdapter.lastEditLapText?.requestFocus()
            }
            binding.stopwatchSave.beVisibleIf(CurrentStopwatch.laps.size > 1)
        }
    }

    private fun saveLaps() {
        activity?.run {
            hideKeyboard()
            val completedLaps = createNewListLap(CurrentStopwatch.laps, CurrentStopwatch.currentSetId)
            if (completedLaps.isNotEmpty()) openEditListLap(completedLaps)
        }
    }

    private fun openEditListLap(stopwatchList: List<Stopwatch>) {
        currentEditStopwatchDialog = EditStopwatchDialog(activity as SimpleActivity, stopwatchList) {
            currentEditStopwatchDialog = null
        }
    }

    private val updateListener = object : CurrentStopwatch.UpdateListener {
        override fun onUpdate(totalTime: Long, lapTime: Long) {
            activity?.runOnUiThread {
                updateDisplayedText(totalTime, lapTime)
            }
        }

        override fun onStateChanged(state: StopwatchEngine.State) {
            activity?.runOnUiThread {
                updateStartLapIcon(state)
                updatePauseResetIcon(state)
                binding.stopwatchPauseReset.beVisibleIf(state != StopwatchEngine.State.RESETED)
            }
        }
    }

    private fun scrollToLastLap() {
        val itemCount = binding.stopwatchList.adapter?.itemCount ?: 0
        if (itemCount > 0) {
            binding.stopwatchList.smoothScrollToPosition(itemCount - 1)
        }
    }
}
