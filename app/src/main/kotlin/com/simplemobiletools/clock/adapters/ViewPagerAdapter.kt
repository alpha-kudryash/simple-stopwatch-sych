package com.simplemobiletools.clock.adapters

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.simplemobiletools.clock.fragments.LapFragment
import com.simplemobiletools.clock.fragments.StopwatchFragment
import com.simplemobiletools.clock.helpers.*

class ViewPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
    private val fragments = HashMap<Int, Fragment>()

    override fun getItem(position: Int): Fragment {
        return getFragment(position)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position)
        if (fragment is Fragment) {
            fragments[position] = fragment
        }
        return fragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        fragments.remove(position)
        super.destroyItem(container, position, item)
    }

    override fun getCount() = TABS_COUNT

    private fun getFragment(position: Int) = when (position) {
        0 -> StopwatchFragment()
        1 -> LapFragment()
        else -> throw RuntimeException("Trying to fetch unknown fragment id $position")
    }

    fun updateLapPosition(lapId: Int) {
        (fragments[TAB_LAP] as? LapFragment)?.updatePosition(lapId)
    }

    fun startStopWatch() {
        (fragments[TAB_STOPWATCH] as? StopwatchFragment)?.startLapStopwatch()
    }
}
