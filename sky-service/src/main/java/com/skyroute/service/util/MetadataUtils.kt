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
package com.skyroute.service.util

import android.os.Bundle
import com.skyroute.service.config.MqttConfig

/**
 * Utility methods for extracting configuration objects from [Bundle] metadata,
 * typically defined in `AndroidManifest.xml`.
 *
 * @author Andre Suryana
 */
object MetadataUtils {

    /**
     * Converts a [Bundle] to an [MqttConfig], which defines MQTT connection parameters.
     */
    fun Bundle.toMqttConfig() = MqttConfig(
        brokerUrl = getString(KEY_BROKER_URL) ?: Defaults.MQTT_BROKER_URL,
        clientPrefix = getString(KEY_CLIENT_PREFIX) ?: Defaults.MQTT_CLIENT_PREFIX,
        cleanSession = getBoolean(KEY_CLEAN_SESSION, Defaults.MQTT_CLEAN_SESSION),
        connectionTimeout = getInt(KEY_CONNECTION_TIMEOUT, Defaults.MQTT_CONNECTION_TIMEOUT),
        keepAliveInterval = getInt(KEY_KEEP_ALIVE_INTERVAL, Defaults.MQTT_KEEP_ALIVE_INTERVAL),
        maxInFlight = getInt(KEY_MAX_IN_FLIGHT, Defaults.MQTT_MAX_IN_FLIGHT),
        automaticReconnect = getBoolean(KEY_AUTO_RECONNECT, Defaults.MQTT_AUTO_RECONNECT),
        username = getString(KEY_USERNAME),
        password = getString(KEY_PASSWORD),
    )

    private object Defaults {
        // MQTT
        const val MQTT_BROKER_URL = "tcp://127.0.0.1:1883"
        const val MQTT_CLIENT_PREFIX = "skyroute"
        const val MQTT_CLEAN_SESSION = true
        const val MQTT_CONNECTION_TIMEOUT = 10
        const val MQTT_KEEP_ALIVE_INTERVAL = 30
        const val MQTT_MAX_IN_FLIGHT = 10
        const val MQTT_AUTO_RECONNECT = true
    }

    // Keys used to extract values from Bundle
    private const val KEY_BROKER_URL = "mqttBrokerUrl"
    private const val KEY_CLIENT_PREFIX = "clientPrefix"
    private const val KEY_CLEAN_SESSION = "cleanSession"
    private const val KEY_CONNECTION_TIMEOUT = "connectionTimeout"
    private const val KEY_KEEP_ALIVE_INTERVAL = "keepAliveInterval"
    private const val KEY_MAX_IN_FLIGHT = "maxInFlight"
    private const val KEY_AUTO_RECONNECT = "autoReconnect"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
}
