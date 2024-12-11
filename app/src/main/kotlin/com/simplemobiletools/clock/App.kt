package com.simplemobiletools.clock

import android.app.Application
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.facebook.stetho.Stetho
import com.simplemobiletools.commons.extensions.checkUseEnglish

class App : Application(), LifecycleObserver {

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }

        checkUseEnglish()
    }

    /*override fun onTerminate() {
        super.onTerminate()
    }*/
}
