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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.security.SecureRandom
import kotlin.math.abs

/**
 * Configuration for establishing an MQTT connection.
 *
 * @property brokerUrl The URL of the MQTT broker, including the host and port.
 * @property clientPrefix The client prefix used to generate unique MQTT client identifier.
 * @property cleanStart Whether to use a clean session. When true, any previous session will be discarded. Otherwise, resume the previous session
 * if one exists and keep it for [sessionExpiryInterval] seconds when [sessionExpiryInterval] > 0.
 * @property sessionExpiryInterval The maximum time (in seconds) that the broker will maintain the session for once the client disconnects.
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
@Parcelize
data class MqttConfig(
    var brokerUrl: String? = null,
    var clientPrefix: String = DEFAULT_CLIENT_PREFIX,
    var cleanStart: Boolean = DEFAULT_CLEAN_START,
    var sessionExpiryInterval: Int? = null,
    var connectionTimeout: Int = DEFAULT_CONNECTION_TIMEOUT,
    var keepAliveInterval: Int = DEFAULT_KEEP_ALIVE_INTERVAL,
    var automaticReconnect: Boolean = DEFAULT_AUTO_RECONNECT,
    var automaticReconnectMinDelay: Int = DEFAULT_AUTO_RECONNECT_MIN_DELAY,
    var automaticReconnectMaxDelay: Int = DEFAULT_AUTO_RECONNECT_MAX_DELAY,
    var maxReconnectDelay: Int = DEFAULT_MAX_RECONNECT_DELAY,
    var username: String? = null,
    var password: String? = null,
    var tlsConfig: TlsConfig = TlsConfig.None,
) : Parcelable {

    /**
     * Generates a unique client identifier for the MQTT connection using the [clientPrefix]
     * followed by a dash and a securely generated positive random number.
     *
     * @return A unique MQTT client ID string.
     */
    val clientId: String
        get() {
            val random = abs(SecureRandom().nextLong())
            val separator = if (clientPrefix.endsWith('-')) "" else "-"
            return "$clientPrefix$separator$random"
        }

    fun isSameConfig(other: MqttConfig?): Boolean {
        if (other == null) return false
        if (this === other) return true
        return brokerUrl == other.brokerUrl &&
            clientPrefix == other.clientPrefix &&
            cleanStart == other.cleanStart &&
            sessionExpiryInterval == other.sessionExpiryInterval &&
            connectionTimeout == other.connectionTimeout &&
            keepAliveInterval == other.keepAliveInterval &&
            automaticReconnect == other.automaticReconnect &&
            automaticReconnectMinDelay == other.automaticReconnectMinDelay &&
            automaticReconnectMaxDelay == other.automaticReconnectMaxDelay &&
            maxReconnectDelay == other.maxReconnectDelay &&
            username == other.username &&
            password == other.password &&
            tlsConfig.isSameConfig(other.tlsConfig)
    }

    companion object {
        const val DEFAULT_CLIENT_PREFIX = "skyroute"
        const val DEFAULT_CLEAN_START = true
        const val DEFAULT_CONNECTION_TIMEOUT = 30
        const val DEFAULT_KEEP_ALIVE_INTERVAL = 60
        const val DEFAULT_AUTO_RECONNECT = true
        const val DEFAULT_AUTO_RECONNECT_MIN_DELAY = 1
        const val DEFAULT_AUTO_RECONNECT_MAX_DELAY = 120
        const val DEFAULT_MAX_RECONNECT_DELAY = 21600
    }
}
