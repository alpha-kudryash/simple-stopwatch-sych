package com.simplemobiletools.clock.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.simplemobiletools.clock.activities.SimpleActivity
import com.simplemobiletools.clock.databinding.ItemLapBinding
import com.simplemobiletools.clock.extensions.formatStopwatchTime
import com.simplemobiletools.clock.helpers.CurrentStopwatch
import com.simplemobiletools.clock.helpers.SORT_BY_LAP
import com.simplemobiletools.clock.helpers.SORT_BY_LAP_TIME
import com.simplemobiletools.clock.helpers.SORT_BY_TOTAL_TIME
import com.simplemobiletools.clock.models.Lap
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.views.MyRecyclerView

class StopwatchAdapter(activity: SimpleActivity, var laps: ArrayList<Lap>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) :
    MyRecyclerViewAdapter(activity, recyclerView, itemClick) {
    var lastEditLapText: TextView? = null


    override fun getActionMenuId() = 0

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = laps.size

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemSelectionKey(position: Int) = laps.getOrNull(position)?.id

    override fun getItemKeyPosition(key: Int) = laps.indexOfFirst { it.id == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemLapBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val lap = laps[position]
        holder.bindView(lap, false, false) { itemView, layoutPosition ->
            setupView(itemView, lap)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = laps.size

    fun updateItems(newItems: ArrayList<Lap>) {
        lastEditLapText = null
        laps = newItems.clone() as ArrayList<Lap>
        laps.sort()
        notifyDataSetChanged()
        finishActMode()
    }

    private fun setupView(view: View, lap: Lap) {
        ItemLapBinding.bind(view).apply {
            lapOrder.text = lap.id.toString()
            lapOrder.setTextColor(textColor)
            lapOrder.setOnClickListener {
                itemClick(SORT_BY_LAP)
            }

            lapLapTime.text = lap.lapTime.formatStopwatchTime()
            lapLapTime.setTextColor(textColor)
            lapLapTime.setOnClickListener {
                itemClick(SORT_BY_LAP_TIME)
            }

            lapTotalTime.text = lap.totalTime.formatStopwatchTime()
            lapTotalTime.setTextColor(textColor)
            lapTotalTime.setOnClickListener {
                itemClick(SORT_BY_TOTAL_TIME)
            }

            editLapText.apply {
                (tag as? TextWatcher)?.let { removeTextChangedListener(it) }
                setText(lap.textTime)
                val watcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(text: Editable?) {
                        lap.textTime = text.toString()
                        CurrentStopwatch.updateLapText(lap.id, lap.textTime)
                    }
                }
                addTextChangedListener(watcher)
                tag = watcher
            }

            if (lastEditLapText === editLapText) lastEditLapText = null
            if (lap.id == laps.maxOfOrNull { it.id }) {
                lastEditLapText = editLapText
            }
        }
    }


}
