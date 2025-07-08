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
import com.skyroute.core.mqtt.MqttConfig
import com.skyroute.core.mqtt.TlsConfig
import java.io.IOException

/**
 * Utility methods for extracting configuration objects from [Bundle] metadata,
 * typically defined in `AndroidManifest.xml`.
 *
 * @author Andre Suryana
 */
object MetadataUtils {

    // TODO: Update config handling mechanism
    //  1. Recreate into class namely `ConfigResolver(Bundle)` (class not object)
    //  2. Resolve config by calling `ConfigResolver#resolve()` and return the `MqttConfig` object
    //  3. `resolve` will throw an exception when config validation fails (e.g., incorrect brokerUrl scheme for SSL/TLS configuration)

    /**
     * Converts a [Bundle] to an [MqttConfig], which defines MQTT connection parameters.
     */
    fun Bundle.toMqttConfig() = MqttConfig(
        brokerUrl = getString(KEY_BROKER_URL) ?: Defaults.MQTT_BROKER_URL,
        clientPrefix = getString(KEY_CLIENT_PREFIX) ?: Defaults.MQTT_CLIENT_PREFIX,
        cleanStart = getBoolean(KEY_CLEAN_START, Defaults.MQTT_CLEAN_START),
        sessionExpiryInterval = getInt(KEY_SESSION_EXPIRY_INTERVAL),
        connectionTimeout = getInt(KEY_CONNECTION_TIMEOUT, Defaults.MQTT_CONNECTION_TIMEOUT),
        keepAliveInterval = getInt(KEY_KEEP_ALIVE_INTERVAL, Defaults.MQTT_KEEP_ALIVE_INTERVAL),
        automaticReconnect = getBoolean(KEY_AUTO_RECONNECT, Defaults.MQTT_AUTO_RECONNECT),
        automaticReconnectMinDelay = getInt(KEY_AUTO_RECONNECT_MIN_DELAY, Defaults.MQTT_AUTO_RECONNECT_MIN_DELAY),
        automaticReconnectMaxDelay = getInt(KEY_AUTO_RECONNECT_MAX_DELAY, Defaults.MQTT_AUTO_RECONNECT_MAX_DELAY),
        maxReconnectDelay = getInt(KEY_MAX_RECONNECT_DELAY, Defaults.MQTT_MAX_RECONNECT_DELAY),
        username = getString(KEY_USERNAME),
        password = getString(KEY_PASSWORD),
        tlsConfig = toTlsConfig() ?: TlsConfig.Disabled,
    )

    /**
     * Converts a [Bundle] to a [TlsConfig].
     */
    private fun Bundle.toTlsConfig(): TlsConfig? {
        val caCertPath = getString(KEY_CA_CERT_PATH)
        if (caCertPath.isNullOrEmpty()) return null

        return try {
            val clientCertPath = getString(KEY_CLIENT_CERT_PATH)
            val clientKeyPath = getString(KEY_CLIENT_KEY_PATH)
            val clientKeyPassword = getString(KEY_CLIENT_KEY_PASSWORD)
            val skipVerify = getBoolean(KEY_INSECURE_SKIP_VERIFY, false)

            if (!clientCertPath.isNullOrEmpty() && !clientKeyPath.isNullOrEmpty()) {
                TlsConfig.MutualAuth(
                    caCertPath,
                    clientCertPath,
                    clientKeyPath,
                    clientKeyPassword,
                    skipVerify,
                )
            } else {
                TlsConfig.ServerAuth(caCertPath, skipVerify)
            }
        } catch (e: IOException) {
            return null
        }
    }

    private object Defaults {
        const val MQTT_BROKER_URL = "tcp://127.0.0.1:1883"
        const val MQTT_CLIENT_PREFIX = "skyroute"
        const val MQTT_CLEAN_START = true
        const val MQTT_CONNECTION_TIMEOUT = 10
        const val MQTT_KEEP_ALIVE_INTERVAL = 30
        const val MQTT_AUTO_RECONNECT = true
        const val MQTT_AUTO_RECONNECT_MIN_DELAY = 1
        const val MQTT_AUTO_RECONNECT_MAX_DELAY = 120
        const val MQTT_MAX_RECONNECT_DELAY = 21600
    }

    // Keys used to extract values from Bundle
    private const val KEY_BROKER_URL = "mqttBrokerUrl"
    private const val KEY_CLIENT_PREFIX = "clientPrefix"
    private const val KEY_CLEAN_START = "cleanStart"
    private const val KEY_SESSION_EXPIRY_INTERVAL = "sessionExpiryInterval"
    private const val KEY_CONNECTION_TIMEOUT = "connectionTimeout"
    private const val KEY_KEEP_ALIVE_INTERVAL = "keepAliveInterval"
    private const val KEY_AUTO_RECONNECT = "autoReconnect"
    private const val KEY_AUTO_RECONNECT_MIN_DELAY = "autoReconnectMinDelay"
    private const val KEY_AUTO_RECONNECT_MAX_DELAY = "autoReconnectMaxDelay"
    private const val KEY_MAX_RECONNECT_DELAY = "maxReconnectDelay"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_CA_CERT_PATH = "caCertPath"
    private const val KEY_CLIENT_CERT_PATH = "clientCertPath"
    private const val KEY_CLIENT_KEY_PATH = "clientKeyPath"
    private const val KEY_CLIENT_KEY_PASSWORD = "clientKeyPassword"
    private const val KEY_INSECURE_SKIP_VERIFY = "insecureSkipVerify"
}
