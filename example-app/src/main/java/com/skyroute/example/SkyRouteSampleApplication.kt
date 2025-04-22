package com.skyroute.example

import android.app.Application
import com.skyroute.api.SkyRoute

/**
 * An application class that initializes SkyRouteMQ when it is created.
 *
 * @author Andre Suryana
 */
class SkyRouteSampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize SkyRouteMQ for the first time
        SkyRoute.getDefault().init(applicationContext)
    }
}