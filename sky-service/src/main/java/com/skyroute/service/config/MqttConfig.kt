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
package com.skyroute.service.config

/**
 * Configuration for establishing an MQTT connection.
 *
 * @param brokerUrl The MQTT broker URL (host and port).
 * @param clientPrefix Client prefix for the MQTT client identifier.
 * @param cleanSession If true, a clean session will be used (defaults to true).
 * @param connectionTimeout Connection timeout in seconds (defaults to 10).
 * @param keepAliveInterval Interval in seconds for PING requests (defaults to 30).
 * @param maxInFlight Maximum number of in-flight messages (defaults to 10).
 * @param automaticReconnect If true, the client will automatically reconnect (defaults to true).
 * @param username Username for broker authentication (optional).
 * @param password Password for broker authentication (optional).
 *
 * @author Andre Suryana
 * @todo Since this data model used in both `sky-api` and `sky-service`, we should extract it to a `common` module
 */
data class MqttConfig(
    val brokerUrl: String,
    val clientPrefix: String,
    val cleanSession: Boolean = true,
    val connectionTimeout: Int = 10,
    val keepAliveInterval: Int = 30,
    val maxInFlight: Int = 10,
    val automaticReconnect: Boolean = true,
    val username: String? = null,
    val password: String? = null,
) {
    fun generateClientId(): String {
        val suffix = System.currentTimeMillis()
        val separator = if (clientPrefix.endsWith('-')) "" else "-"
        return "$clientPrefix$separator$suffix"
    }
}
