package com.simplemobiletools.clock.dialogs

import com.simplemobiletools.clock.activities.SimpleActivity
import com.simplemobiletools.clock.databinding.DialogEditStopwatchBinding
import com.simplemobiletools.clock.extensions.*
import com.simplemobiletools.clock.models.Stopwatch
import com.simplemobiletools.clock.helpers.CurrentStopwatch
import com.simplemobiletools.commons.extensions.*
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.clock.commons.extensions.getProperTextColor

class EditStopwatchDialog(val activity: SimpleActivity, val stopwatchList: List<Stopwatch>, val callback: (id: Long) -> Unit) {
    private val binding = DialogEditStopwatchBinding.inflate(activity.layoutInflater)
    private val textColor = activity.getProperTextColor()

    init {
        binding.apply {
            editStopwatchLabelImage.applyColorFilter(textColor)
            editStopwatch.setText("") // todo change
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.simplemobiletools.commons.R.string.ok, null)
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        stopwatchList.forEach { stopwatch ->
                            if (binding.editStopwatch.value != "")
                                stopwatch.label = binding.editStopwatch.value
                            else
                                stopwatch.label = ""
                        }
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                        activity.stopwatchHelper.insertOrUpdateStopwatch(stopwatchList, onError = { error ->
                            activity.runOnUiThread {
                                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                activity.showErrorToast(error)
                            }
                        }) { ids, setId ->
                            activity.runOnUiThread {
                                ids.firstOrNull()?.let { firstId ->
                                    CurrentStopwatch.currentSetId = setId
                                    callback(firstId)
                                }
                                alertDialog.dismiss()
                            }
                        }
                    }
                }
            }
    }

    private fun restoreLastAlarm() {
        /*if (stopwatch.id == null) {
            activity.config.timerLastConfig?.let { lastConfig ->
                stopwatch.label = lastConfig.label
                stopwatch.milliseconds = lastConfig.milliseconds
                stopwatch.soundTitle = lastConfig.soundTitle
                stopwatch.soundUri = lastConfig.soundUri
                stopwatch.vibrate = lastConfig.vibrate
            }
        }*/
    }

    private fun updateAlarmTime() {
        //binding.editStopwatchInitialTime.text = activity.getFormattedTime(stopwatch.milliseconds, false, true)
    }

    private fun changeDuration(stopwatch: Stopwatch) {
        /*MyTimePickerDialogDialog(activity, stopwatch.milliseconds) { milliseconds ->
            val stopwatchMilliseconds = if (milliseconds <= 0) 10 else milliseconds
            stopwatch.milliseconds = stopwatchMilliseconds
            binding.editStopwatchInitialTime.text = stopwatchMilliseconds.getFormattedDuration()
        }*/
    }
}
