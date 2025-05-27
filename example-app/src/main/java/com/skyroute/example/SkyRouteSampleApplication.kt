/*
 * Copyright (C) 2025 Andre Suryana, SkyRoute (https://github.com/skyroute)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
                    clientPrefix = "broker-test",
                    cleanStart = true,
                    connectionTimeout = 30,
                    keepAliveInterval = 60,
                    automaticReconnect = true,
                    username = "your-username",
                    password = "your-password",
                ),
            )
        } else {
            // Initialize SkyRouteMQ for the first time
            SkyRoute.getDefault().init(applicationContext)
        }
    }
}
