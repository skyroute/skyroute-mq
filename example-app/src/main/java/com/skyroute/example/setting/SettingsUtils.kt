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
package com.skyroute.example.setting

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.skyroute.core.mqtt.MqttConfig
import com.skyroute.core.mqtt.MqttConfig.Companion.DEFAULT_AUTO_RECONNECT
import com.skyroute.core.mqtt.MqttConfig.Companion.DEFAULT_AUTO_RECONNECT_MAX_DELAY
import com.skyroute.core.mqtt.MqttConfig.Companion.DEFAULT_AUTO_RECONNECT_MIN_DELAY
import com.skyroute.core.mqtt.MqttConfig.Companion.DEFAULT_CLEAN_START
import com.skyroute.core.mqtt.MqttConfig.Companion.DEFAULT_CLIENT_PREFIX
import com.skyroute.core.mqtt.MqttConfig.Companion.DEFAULT_CONNECTION_TIMEOUT
import com.skyroute.core.mqtt.MqttConfig.Companion.DEFAULT_KEEP_ALIVE_INTERVAL
import com.skyroute.core.mqtt.MqttConfig.Companion.DEFAULT_MAX_RECONNECT_DELAY
import com.skyroute.core.mqtt.TlsConfig

/**
 * @author Andre Suryana
 */
object SettingsUtils {

    fun readConfig(context: Context): MqttConfig? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val brokerUrl = prefs.getString("broker_url", null) ?: return null

        val generateClientId = prefs.getBoolean("generate_client_id", false)
        val clientId = if (generateClientId) {
            val clientPrefix = prefs.getString("client_prefix", DEFAULT_CLIENT_PREFIX) ?: DEFAULT_CLIENT_PREFIX
            MqttConfig.generateRandomClientId(clientPrefix)
        } else {
            prefs.getString("client_id", null)
        }

        return MqttConfig(
            brokerUrl = brokerUrl,
            clientId = clientId,
            cleanStart = prefs.getBoolean("clean_start", DEFAULT_CLEAN_START),
            sessionExpiryInterval = prefs.getString("session_expiry_interval", null)?.toIntOrNull() ?: 0,
            connectionTimeout = prefs.getString("connection_timeout", null)?.toIntOrNull() ?: DEFAULT_CONNECTION_TIMEOUT,
            keepAliveInterval = prefs.getString("keep_alive_interval", null)?.toIntOrNull() ?: DEFAULT_KEEP_ALIVE_INTERVAL,
            automaticReconnect = prefs.getBoolean("automatic_reconnect", DEFAULT_AUTO_RECONNECT),
            automaticReconnectMinDelay = prefs.getString("reconnect_min_delay", null)?.toIntOrNull() ?: DEFAULT_AUTO_RECONNECT_MIN_DELAY,
            automaticReconnectMaxDelay = prefs.getString("reconnect_max_delay", null)?.toIntOrNull() ?: DEFAULT_AUTO_RECONNECT_MAX_DELAY,
            maxReconnectDelay = prefs.getString("max_reconnect_delay", null)?.toIntOrNull() ?: DEFAULT_MAX_RECONNECT_DELAY,
            username = prefs.getString("username", null),
            password = prefs.getString("password", null),
            tlsConfig = readTlsConfig(prefs),
        )
    }

    private fun readTlsConfig(prefs: SharedPreferences): TlsConfig {
        return when (prefs.getString("tls_mode", "none")) {
            "default" -> TlsConfig.Default

            "server_auth" -> TlsConfig.ServerAuth(
                caCertPath = prefs.getString("tls_ca_cert_path", null).orEmpty(),
                skipVerify = prefs.getBoolean("tls_skip_verify", false),
            )

            "mutual_auth" -> TlsConfig.MutualAuth(
                caCertPath = prefs.getString("tls_ca_cert_path", null).orEmpty(),
                clientCertPath = prefs.getString("tls_client_cert_path", null).orEmpty(),
                clientKeyPath = prefs.getString("tls_client_key_path", null).orEmpty(),
                clientKeyPassword = prefs.getString("tls_client_key_password", null),
                skipVerify = prefs.getBoolean("tls_skip_verify", false),
            )

            else -> TlsConfig.None
        }
    }
}
