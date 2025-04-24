package com.skyroute.example

import android.app.Application
import com.skyroute.api.SkyRoute
import com.skyroute.service.config.MqttConfig

/**
 * An application class that initializes SkyRouteMQ when it is created.
 *
 * @author Andre Suryana
 */
class SkyRouteSampleApplication : Application() {

    companion object {
        private const val USE_CUSTOM_CONFIG = false
    }

    override fun onCreate() {
        super.onCreate()

        // Example SkyRouteMQ initialization with and without
        // specifying custom configuration
        if (USE_CUSTOM_CONFIG) {
            // Initialize SkyRouteMQ with custom configuration
            // This will replace the default configuration in 'AndroidManifest.xml'
            SkyRoute.getDefault().init(
                applicationContext,
                MqttConfig(
                    brokerUrl = "tcp://your-broker.url",
                    clientId = "broker-test",
                    cleanSession = true,
                    connectionTimeout = 30,
                    keepAliveInterval = 60,
                    automaticReconnect = true,
                    username = "your-username",
                    password = "your-password"
                )
            )
        } else {
            // Initialize SkyRouteMQ for the first time
            SkyRoute.getDefault().init(applicationContext)
        }
    }
}