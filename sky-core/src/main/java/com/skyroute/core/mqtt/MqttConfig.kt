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
package com.skyroute.core.mqtt

import java.security.SecureRandom
import kotlin.math.abs

/**
 * Configuration for establishing an MQTT connection.
 *
 * @property brokerUrl The URL of the MQTT broker, including the host and port.
 * @property clientPrefix The client prefix used to generate unique MQTT client identifier.
 * @property cleanStart Whether to use a clean session. When true, any previous session will be discarded. Otherwise, resume the previous session
 * if one exists and keep it for [sessionExpiryInterval] seconds when [sessionExpiryInterval] > 0.
 * @property connectionTimeout The maximum time (in seconds) to wait when establishing a connection before timing out. Default is 30 seconds.
 * @property keepAliveInterval The interval (in seconds) between PING messages sent to the broker to keep connection alive. Default is 60 seconds.
 * @property automaticReconnect Whether the client should automatically attempt to reconnect if the connection is lost. Default is true.
 * @property automaticReconnectMinDelay The minimum delay (in seconds) before attempting the first reconnect. Default is 1 second.
 * @property automaticReconnectMaxDelay The maximum delay (in seconds) between reconnect attempts. Default is 120 seconds.
 * @property maxReconnectDelay The upper limit (in seconds) for the total delay before a reconnect attempt. Default is 21600 seconds (6 hours).
 * @property username Optional username for authenticating with the broker.
 * @property password Optional password for authenticating with the broker.
 * @property tlsConfig Optional SSL/TLS configuration, support both TLS and mTLS.
 *
 * @constructor Creates a new instance of [MqttConfig] with the specified connection and reconnection parameters.
 *
 * @author Andre Suryana
 */
data class MqttConfig(
    val brokerUrl: String,
    private val clientPrefix: String = "skyroute",
    val cleanStart: Boolean = true,
    val sessionExpiryInterval: Int? = null,
    val connectionTimeout: Int = 30,
    val keepAliveInterval: Int = 60,
    val automaticReconnect: Boolean = true,
    val automaticReconnectMinDelay: Int = 1,
    val automaticReconnectMaxDelay: Int = 120,
    val maxReconnectDelay: Int = 21600,
    val username: String? = null,
    val password: String? = null,
    val tlsConfig: TlsConfig? = null,
) {

    /**
     * Generates a unique client identifier for the MQTT connection based on the [clientPrefix]
     * and the current system time in milliseconds.
     *
     * @return A unique MQTT client ID string.
     */
    fun getClientId(): String {
        val random = SecureRandom()
        val randomNum = abs(random.nextLong())

        val separator = if (clientPrefix.endsWith('-')) "" else "-"
        return "$clientPrefix$separator$randomNum"
    }
}
