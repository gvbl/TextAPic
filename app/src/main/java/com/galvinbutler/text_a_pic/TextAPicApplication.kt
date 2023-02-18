package com.galvinbutler.text_a_pic

import android.app.Application
import timber.log.Timber
import timber.log.Timber.Forest.plant


class TextAPicApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            plant(Timber.DebugTree())
        }
    }
}