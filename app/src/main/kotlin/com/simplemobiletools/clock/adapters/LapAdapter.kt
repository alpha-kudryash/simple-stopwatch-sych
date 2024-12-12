package com.simplemobiletools.clock.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.simplemobiletools.clock.R
import com.simplemobiletools.clock.activities.SimpleActivity
import com.simplemobiletools.clock.databinding.ItemSavesLapBinding
import com.simplemobiletools.clock.extensions.getFormattedDuration
import com.simplemobiletools.clock.models.Stopwatch
import com.simplemobiletools.clock.models.StopwatchEvent
import com.simplemobiletools.clock.models.TimerEvent
import com.simplemobiletools.commons.adapters.MyRecyclerViewListAdapter
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.views.MyRecyclerView
import me.grantland.widget.AutofitHelper
import org.greenrobot.eventbus.EventBus

class LapAdapter(
    private val simpleActivity: SimpleActivity, var laps: List<Stopwatch>,
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
        return createViewHolder(ItemSavesLapBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(getItem(position), true, true) { itemView, _ ->
            setupView(itemView, getItem(position))
        }
        bindViewHolder(holder)
    }

    private fun deleteItems() {
        val positions = getSelectedItemPositions()
        val lapsToRemove = positions.map { position ->
            getItem(position)
        }
        removeSelectedItems(positions)
        lapsToRemove.forEach(::deleteLap)
    }

    private fun setupView(view: View, stopwatch: Stopwatch) {
        ItemSavesLapBinding.bind(view).apply {
            val isSelected = selectedKeys.contains(stopwatch.id)
            savesLapFrame.isSelected = isSelected

            lapLabel.setTextColor(textColor)
            lapLabel.setHintTextColor(textColor.adjustAlpha(0.7f))
            lapLabel.text = "123" //stopwatch.label

            AutofitHelper.create(lapTime)
            lapTime.setTextColor(textColor)
            lapTime.text = stopwatch.milliseconds.getFormattedDuration()
        }
    }

    private fun deleteLap(lap: Stopwatch) {
        EventBus.getDefault().post(StopwatchEvent.Delete(lap.id!!))
    }

    fun updateItems(newItems: List<Stopwatch>) {
        //lastLapId = 0
        submitList(newItems)
        //laps.sort()
        //notifyDataSetChanged()
        finishActMode()
    }
}