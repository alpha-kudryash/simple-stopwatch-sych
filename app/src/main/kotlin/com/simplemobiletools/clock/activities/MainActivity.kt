package com.simplemobiletools.clock.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.simplemobiletools.clock.BuildConfig
import com.simplemobiletools.clock.R
import com.simplemobiletools.clock.adapters.ViewPagerAdapter
import com.simplemobiletools.clock.databinding.ActivityMainBinding
import com.simplemobiletools.clock.extensions.*
import com.simplemobiletools.clock.fragments.LapFragment
import com.simplemobiletools.clock.fragments.StopwatchFragment
import com.simplemobiletools.clock.helpers.*
import com.simplemobiletools.commons.databinding.BottomTablayoutItemBinding
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FAQItem
import kotlinx.coroutines.launch
import me.grantland.widget.AutofitHelper

enum class VibrationType {
    HIGH, LOW
}//todo replace

class MainActivity : SimpleActivity() {
    private var storedTextColor = 0
    private var storedBackgroundColor = 0
    private var storedPrimaryColor = 0
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::inflate)
    //val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
    //binding.viewPager.adapter = viewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()

        updateMaterialActivityViews(binding.mainCoordinator, binding.mainHolder, useTransparentNavigation = false, useTopSearchMenu = false)

        storeStateVariables()
        initFragments()
        setupTabs()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.mainToolbar, statusBarColor = getProperBackgroundColor())
        val configTextColor = getProperTextColor()
        if (storedTextColor != configTextColor) {
            getInactiveTabIndexes(binding.viewPager.currentItem).forEach {
                binding.mainTabsHolder.getTabAt(it)?.icon?.applyColorFilter(configTextColor)
            }
        }

        val configBackgroundColor = getProperBackgroundColor()
        if (storedBackgroundColor != configBackgroundColor) {
            binding.mainTabsHolder.background = ColorDrawable(configBackgroundColor)
        }

        val configPrimaryColor = getProperPrimaryColor()
        if (storedPrimaryColor != configPrimaryColor) {
            binding.mainTabsHolder.setSelectedTabIndicatorColor(getProperPrimaryColor())
            binding.mainTabsHolder.getTabAt(binding.viewPager.currentItem)?.icon?.applyColorFilter(getProperPrimaryColor())
        }

        if (config.preventPhoneFromSleeping) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        setupTabColors()
        checkShortcuts()
    }

    @SuppressLint("NewApi")
    private fun checkShortcuts() {
        val appIconColor = config.appIconColor
        if (isNougatMR1Plus() && config.lastHandledShortcutColor != appIconColor) {
            val launchDialpad = getLaunchStopwatchShortcut(appIconColor)

            try {
                shortcutManager.dynamicShortcuts = listOf(launchDialpad)
                config.lastHandledShortcutColor = appIconColor
            } catch (ignored: Exception) {
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getLaunchStopwatchShortcut(appIconColor: Int): ShortcutInfo {
        val newEvent = getString(R.string.start_stopwatch)
        val drawable = resources.getDrawable(R.drawable.shortcut_stopwatch)
        (drawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_stopwatch_background).applyColorFilter(appIconColor)
        val bmp = drawable.convertToBitmap()

        val intent = Intent(this, SplashActivity::class.java).apply {
            putExtra(OPEN_TAB, TAB_STOPWATCH)
            putExtra(TOGGLE_STOPWATCH, true)
            action = STOPWATCH_TOGGLE_ACTION
        }

        return ShortcutInfo.Builder(this, STOPWATCH_SHORTCUT_ID)
            .setShortLabel(newEvent)
            .setLongLabel(newEvent)
            .setIcon(Icon.createWithBitmap(bmp))
            .setIntent(intent)
            .build()
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
        if (config.preventPhoneFromSleeping) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        config.lastUsedViewPagerPage = binding.viewPager.currentItem
    }

    private fun setupOptionsMenu() {
        binding.mainToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                //R.id.more_apps_from_us -> launchMoreAppsFromUsIntent()
                //R.id.settings -> launchSettings()
                //R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun refreshMenuItems() {
        binding.mainToolbar.menu.apply {
            findItem(R.id.more_apps_from_us).isVisible = !resources.getBoolean(com.simplemobiletools.commons.R.bool.hide_google_relations)
        }
    }

    override fun onNewIntent(intent: Intent) {
        if (intent.extras?.containsKey(OPEN_TAB) == true) {
            val tabToOpen = intent.getIntExtra(OPEN_TAB, TAB_STOPWATCH)
            binding.viewPager.setCurrentItem(tabToOpen, false)
            if (tabToOpen == TAB_LAP) {
                //val timerId = intent.getIntExtra(TIMER_ID, INVALID_TIMER_ID)
                (binding.viewPager.adapter as ViewPagerAdapter).updateLapPosition()
            }
            if (tabToOpen == TAB_STOPWATCH) {
                if (intent.getBooleanExtra(TOGGLE_STOPWATCH, false)) {
                    (binding.viewPager.adapter as ViewPagerAdapter).startStopWatch()
                }
            }
        }
        super.onNewIntent(intent)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (event.action == KeyEvent.ACTION_UP) {
            //if (intent.extras?.containsKey(OPEN_TAB) == true) {
            val tabToOpen = intent.getIntExtra(OPEN_TAB, TAB_STOPWATCH)
            binding.viewPager.setCurrentItem(tabToOpen, false)
            if (tabToOpen == TAB_STOPWATCH) {
                    (binding.viewPager.adapter as ViewPagerAdapter).startResetwatch()
            }}
            triggerVibration(VibrationType.LOW)
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.action == KeyEvent.ACTION_DOWN) {
            if (binding.viewPager.currentItem == TAB_STOPWATCH) {
                (binding.viewPager.adapter as ViewPagerAdapter).pauseStopwatch()
            }}
            triggerVibration(VibrationType.LOW)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun triggerVibration(type: VibrationType) {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val vibrationDuration = when (type) {
            VibrationType.HIGH -> 200
            VibrationType.LOW -> 100
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(vibrationDuration.toLong(), VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(vibrationDuration.toLong())
        }
    }

    private fun storeStateVariables() {
        storedTextColor = getProperTextColor()
        storedBackgroundColor = getProperBackgroundColor()
        storedPrimaryColor = getProperPrimaryColor()
    }

    private fun getViewPagerAdapter() = binding.viewPager.adapter as? ViewPagerAdapter

    private fun initFragments() {
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.onPageChangeListener {
            binding.mainTabsHolder.getTabAt(it)?.select()
            refreshMenuItems()
            viewPagerAdapter.refreshLapFragment()
            viewPagerAdapter.updateLapPosition()
            viewPagerAdapter.updateCurrentIdStopwatch()
        }

        val tabToOpen = intent.getIntExtra(OPEN_TAB, config.lastUsedViewPagerPage)
        intent.removeExtra(OPEN_TAB)
        if (tabToOpen == TAB_LAP) {
            viewPagerAdapter.updateLapPosition()
        }

        if (tabToOpen == TAB_STOPWATCH) {
            config.toggleStopwatch = intent.getBooleanExtra(TOGGLE_STOPWATCH, false)
        }

        binding.viewPager.offscreenPageLimit = TABS_COUNT - 1
        binding.viewPager.currentItem = tabToOpen
    }

    private fun setupTabs() {
        binding.mainTabsHolder.removeAllTabs()
        val tabDrawables = arrayOf(
            R.drawable.ic_stopwatch_vector,
            R.drawable.ic_clock_shadowed
        )
        val tabLabels = arrayOf(R.string.stopwatch, R.string.lap)

        tabDrawables.forEachIndexed { i, drawableId ->
            binding.mainTabsHolder.newTab().setCustomView(com.simplemobiletools.commons.R.layout.bottom_tablayout_item).apply tab@{
                customView?.let { BottomTablayoutItemBinding.bind(it) }?.apply {
                    tabItemIcon.setImageDrawable(getDrawable(drawableId))
                    tabItemLabel.setText(tabLabels[i])
                    AutofitHelper.create(tabItemLabel)
                    binding.mainTabsHolder.addTab(this@tab)
                }
            }
        }

        binding.mainTabsHolder.onTabSelectionChanged(
            tabUnselectedAction = {
                updateBottomTabItemColors(it.customView, false, getDeselectedTabDrawableIds()[it.position])
            },
            tabSelectedAction = {
                binding.viewPager.currentItem = it.position
                updateBottomTabItemColors(it.customView, true, getSelectedTabDrawableIds()[it.position])
            }


        )
    }

    private fun setupTabColors() {
        val activeView = binding.mainTabsHolder.getTabAt(binding.viewPager.currentItem)?.customView
        updateBottomTabItemColors(activeView, true, getSelectedTabDrawableIds()[binding.viewPager.currentItem])

        getInactiveTabIndexes(binding.viewPager.currentItem).forEach { index ->
            val inactiveView = binding.mainTabsHolder.getTabAt(index)?.customView
            updateBottomTabItemColors(inactiveView, false, getDeselectedTabDrawableIds()[index])
        }

        binding.mainTabsHolder.getTabAt(binding.viewPager.currentItem)?.select()
        val bottomBarColor = getBottomNavigationBackgroundColor()
        binding.mainTabsHolder.setBackgroundColor(bottomBarColor)
        updateNavigationBarColor(bottomBarColor)
    }

    private fun getInactiveTabIndexes(activeIndex: Int) = arrayListOf(0, 1).filter { it != activeIndex }

    private fun getSelectedTabDrawableIds() = arrayOf(
        R.drawable.ic_stopwatch_filled_vector,
        R.drawable.ic_sorting_triangle_vector
    )

    private fun getDeselectedTabDrawableIds() = arrayOf(
        R.drawable.ic_stopwatch_vector,
        R.drawable.ic_clock_shadowed

    )

    private fun launchSettings() {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val licenses = LICENSE_STETHO or LICENSE_NUMBER_PICKER or LICENSE_RTL or LICENSE_AUTOFITTEXTVIEW

        val faqItems = arrayListOf(
            FAQItem(com.simplemobiletools.commons.R.string.faq_4_title_commons, com.simplemobiletools.commons.R.string.faq_4_text_commons),
            FAQItem(com.simplemobiletools.commons.R.string.faq_9_title_commons, com.simplemobiletools.commons.R.string.faq_9_text_commons)
        )

        if (!resources.getBoolean(com.simplemobiletools.commons.R.bool.hide_google_relations)) {
            faqItems.add(FAQItem(com.simplemobiletools.commons.R.string.faq_2_title_commons, com.simplemobiletools.commons.R.string.faq_2_text_commons))
            faqItems.add(FAQItem(com.simplemobiletools.commons.R.string.faq_6_title_commons, com.simplemobiletools.commons.R.string.faq_6_text_commons))
        }

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true)
    }
}
