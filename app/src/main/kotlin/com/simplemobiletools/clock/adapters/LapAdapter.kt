package com.simplemobiletools.clock.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.simplemobiletools.clock.R
import com.simplemobiletools.clock.activities.SimpleActivity
import com.simplemobiletools.clock.databinding.ItemTimerBinding
import com.simplemobiletools.clock.extensions.getFormattedDuration
import com.simplemobiletools.clock.extensions.secondsToMillis
import com.simplemobiletools.clock.models.*
import com.simplemobiletools.commons.adapters.MyRecyclerViewListAdapter
import com.simplemobiletools.commons.dialogs.PermissionRequiredDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.views.MyRecyclerView
import me.grantland.widget.AutofitHelper
import org.greenrobot.eventbus.EventBus

class LapAdapter(
    private val simpleActivity: SimpleActivity,
    recyclerView: MyRecyclerView,
    onRefresh: () -> Unit,
    onItemClick: (Stopwatch) -> Unit,
) : MyRecyclerViewListAdapter<Stopwatch>(simpleActivity, recyclerView, diffUtil, onItemClick, onRefresh) {

    companion object {
        private val diffUtil = object : DiffUtil.ItemCallback<Stopwatch>() {
            override fun areItemsTheSame(oldItem: Stopwatch, newItem: Stopwatch): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Stopwatch, newItem: Stopwatch): Boolean {
                return oldItem == newItem
            }
        }
    }

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_alarms

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_delete -> deleteItems()
        }
    }

    override fun getSelectableItemCount() = itemCount

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = getItem(position).id

    override fun getItemKeyPosition(key: Int): Int {
        var position = -1
        for (i in 0 until itemCount) {
            if (key == getItem(i).id) {
                position = i
                break
            }
        }
        return position
    }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemTimerBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(getItem(position), true, true) { itemView, _ ->
            setupView(itemView, getItem(position))
        }
        bindViewHolder(holder)
    }

    private fun deleteItems() {
        val positions = getSelectedItemPositions()
        val timersToRemove = positions.map { position ->
            getItem(position)
        }
        removeSelectedItems(positions)
        timersToRemove.forEach(::deleteLap)
    }

    private fun setupView(view: View, stopwatch: Stopwatch) {
        ItemTimerBinding.bind(view).apply {
            val isSelected = selectedKeys.contains(stopwatch.id)
            timerFrame.isSelected = isSelected

            timerLabel.setTextColor(textColor)
            timerLabel.setHintTextColor(textColor.adjustAlpha(0.7f))
            timerLabel.text = stopwatch.label

            AutofitHelper.create(timerTime)
            timerTime.setTextColor(textColor)
            /*timerTime.text = when (stopwatch.state) {
                is TimerState.Finished -> 0.getFormattedDuration()
                is TimerState.Idle -> stopwatch.seconds.getFormattedDuration()
                is TimerState.Paused -> stopwatch.state.tick.getFormattedDuration()
                is TimerState.Running -> stopwatch.state.tick.getFormattedDuration()
            }*/

            timerReset.applyColorFilter(textColor)
            timerReset.setOnClickListener {
                resetLap(stopwatch)
            }

            timerPlayPause.applyColorFilter(textColor)
            /*timerPlayPause.setOnClickListener {
                (activity as SimpleActivity).handleNotificationPermission { granted ->
                    if (granted) {
                        when (val state = timer.state) {
                            is TimerState.Idle -> EventBus.getDefault().post(TimerEvent.Start(timer.id!!, timer.seconds.secondsToMillis))
                            is TimerState.Paused -> EventBus.getDefault().post(TimerEvent.Start(timer.id!!, state.tick))
                            is TimerState.Running -> EventBus.getDefault().post(TimerEvent.Pause(timer.id!!, state.tick))
                            is TimerState.Finished -> EventBus.getDefault().post(TimerEvent.Start(timer.id!!, timer.seconds.secondsToMillis))
                        }
                    } else {
                        PermissionRequiredDialog(
                            activity,
                            com.simplemobiletools.commons.R.string.allow_notifications_reminders,
                            { activity.openNotificationSettings() })
                    }
                }
            }*/

            /*val state = stopwatch.state
            val resetPossible = state is TimerState.Running || state is TimerState.Paused || state is TimerState.Finished
            timerReset.beInvisibleIf(!resetPossible)
            val drawableId = if (state is TimerState.Running) {
                com.simplemobiletools.commons.R.drawable.ic_pause_vector
            } else {
                com.simplemobiletools.commons.R.drawable.ic_play_vector
            }
            timerPlayPause.setImageDrawable(simpleActivity.resources.getColoredDrawableWithColor(drawableId, textColor))*/
        }
    }

    private fun resetLap(stopwatch: Stopwatch) {
        EventBus.getDefault().post(StopwatchEvent.Reset(stopwatch.id!!))
    }

    private fun deleteLap(stopwatch: Stopwatch) {
        EventBus.getDefault().post(StopwatchEvent.Delete(stopwatch.id!!))
    }
}
