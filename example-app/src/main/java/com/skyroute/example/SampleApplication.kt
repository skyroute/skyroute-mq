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
import android.util.Log
import com.skyroute.api.SkyRoute
import com.skyroute.example.setting.SettingsUtils
import kotlin.concurrent.Volatile

/**
 * An application class that initializes SkyRouteMQ when it is created.
 *
 * @author Andre Suryana
 */
class SampleApplication : Application() {

    companion object {
        private const val TAG = "SampleApplication"

        @Volatile
        private var _skyRoute: SkyRoute? = null

        val skyRoute: SkyRoute
            get() = _skyRoute
                ?: throw IllegalStateException("SkyRouteMQ has not been initialized yet.")
    }

    override fun onCreate() {
        super.onCreate()

        val instance = customSkyRoute() ?: SkyRoute.getDefault()
        instance.init(applicationContext)

        _skyRoute = instance
    }

    private fun customSkyRoute(): SkyRoute? {
        val config = SettingsUtils.readConfig(this) ?: return null
        val brokerUrl = config.brokerUrl ?: return null
        val clientId = config.clientId ?: return null

        return SkyRoute.newBuilder()
            .brokerUrl(brokerUrl)
            .clientId(clientId)
            .cleanStart(config.cleanStart)
            .sessionExpiryInterval(config.sessionExpiryInterval)
            .connectionTimeout(config.connectionTimeout)
            .keepAliveInterval(config.keepAliveInterval)
            .automaticReconnect(config.automaticReconnect)
            .automaticReconnectDelay(
                config.automaticReconnectMinDelay,
                config.automaticReconnectMaxDelay,
            )
            .maxReconnectDelay(config.maxReconnectDelay)
            .tlsConfig(config.tlsConfig)
            .onDisconnectCallback { code, reason ->
                Log.w(TAG, "Disconnected: code=$code, reason=$reason")
            }
            .build()
    }
}
