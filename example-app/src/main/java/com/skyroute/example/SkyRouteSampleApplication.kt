package com.skyroute.example

import android.app.Application
import com.skyroute.api.SkyRoute

class SkyRouteSampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize SkyRouteMQ for the first time
        SkyRoute.getDefault().init(applicationContext)
    }
}