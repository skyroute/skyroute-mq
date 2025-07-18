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

import android.os.Bundle
import com.skyroute.core.mqtt.MqttConfig
import com.skyroute.core.mqtt.TlsConfig
import com.skyroute.core.util.Logger

/**
 * Resolver to extract [MqttConfig] from [Bundle] metadata.
 *
 * @author Andre Suryana
 */
class ConfigResolver(
    private val metaData: Bundle,
    private val logger: Logger = Logger.Default(),
) {

    /** Resolves the [MqttConfig] from the provided [Bundle] metadata. */
    fun resolve(): MqttConfig {
        val base = MqttConfig() // contains default values

        val brokerUrl = metaData.getString(KEY_BROKER_URL) ?: base.brokerUrl
        require(!brokerUrl.isNullOrBlank()) { "Missing required value: $KEY_BROKER_URL" }
        require(brokerUrl.startsWith("tcp://") || brokerUrl.startsWith("ssl://")) {
            "Invalid value for $KEY_BROKER_URL: must start with tcp:// or ssl://"
        }

        val sessionExpiryInterval = metaData.getIntOrNull(KEY_SESSION_EXPIRY_INTERVAL)
        sessionExpiryInterval?.let {
            require(it >= 0) { "$KEY_SESSION_EXPIRY_INTERVAL must be positive or 0" }
        }

        val clientPrefix = metaData.getString(KEY_CLIENT_PREFIX) ?: MqttConfig.DEFAULT_CLIENT_PREFIX
        require(clientPrefix.isNotBlank()) { "$KEY_CLIENT_PREFIX cannot be blank" }

        val connectionTimeout = metaData.getInt(KEY_CONNECTION_TIMEOUT, base.connectionTimeout)
        require(connectionTimeout > 0) { "$KEY_CONNECTION_TIMEOUT must be positive" }

        val keepAliveInterval = metaData.getInt(KEY_KEEP_ALIVE_INTERVAL, base.keepAliveInterval)
        require(keepAliveInterval > 0) { "$KEY_KEEP_ALIVE_INTERVAL must be positive" }

        val autoReconnectMinDelay = metaData.getInt(KEY_AUTO_RECONNECT_MIN_DELAY, base.automaticReconnectMinDelay)
        require(autoReconnectMinDelay >= 0) { "$KEY_AUTO_RECONNECT_MIN_DELAY must be positive" }

        val autoReconnectMaxDelay = metaData.getInt(KEY_AUTO_RECONNECT_MAX_DELAY, base.automaticReconnectMaxDelay)
        require(autoReconnectMaxDelay >= 0) { "$KEY_AUTO_RECONNECT_MAX_DELAY must be positive" }
        require(autoReconnectMaxDelay >= autoReconnectMinDelay) {
            "$KEY_AUTO_RECONNECT_MAX_DELAY must be greater than or equal to $KEY_AUTO_RECONNECT_MIN_DELAY"
        }

        val maxReconnectDelay = metaData.getInt(KEY_MAX_RECONNECT_DELAY, base.maxReconnectDelay)
        require(maxReconnectDelay >= 0) { "$KEY_MAX_RECONNECT_DELAY must be positive" }
        require(maxReconnectDelay >= autoReconnectMinDelay && maxReconnectDelay >= autoReconnectMaxDelay) {
            "$KEY_MAX_RECONNECT_DELAY must be greater than or equal to $KEY_AUTO_RECONNECT_MIN_DELAY and $KEY_AUTO_RECONNECT_MAX_DELAY"
        }

        val tlsConfig = if (brokerUrl.startsWith("tcp://")) {
            logger.w("TLS configuration provided but not used for TCP connections")
            TlsConfig.None
        } else {
            resolveTlsConfig()
        }

        if (brokerUrl.startsWith("ssl://") && tlsConfig is TlsConfig.None) {
            logger.w("TLS configuration is required for SSL-based connections")
        }

        return base.copy(
            brokerUrl = brokerUrl,
            clientId = MqttConfig.generateRandomClientId(clientPrefix),
            cleanStart = metaData.getBoolean(KEY_CLEAN_START, base.cleanStart),
            sessionExpiryInterval = metaData.getIntOrNull(KEY_SESSION_EXPIRY_INTERVAL) ?: base.sessionExpiryInterval,
            connectionTimeout = connectionTimeout,
            keepAliveInterval = keepAliveInterval,
            automaticReconnect = metaData.getBoolean(KEY_AUTO_RECONNECT, base.automaticReconnect),
            automaticReconnectMinDelay = autoReconnectMinDelay,
            automaticReconnectMaxDelay = autoReconnectMaxDelay,
            maxReconnectDelay = maxReconnectDelay,
            username = metaData.getString(KEY_USERNAME) ?: base.username,
            password = metaData.getString(KEY_PASSWORD) ?: base.password,
            tlsConfig = tlsConfig,
        )
    }

    /** Resolves the [TlsConfig] from the provided [Bundle] metadata. */
    private fun resolveTlsConfig(): TlsConfig {
        val caCertPath = metaData.getString(KEY_CA_CERT_PATH) ?: return TlsConfig.Default
        val clientCertPath = metaData.getString(KEY_CLIENT_CERT_PATH)
        val clientKeyPath = metaData.getString(KEY_CLIENT_KEY_PATH)

        require(
            (clientCertPath.isNullOrEmpty() && clientKeyPath.isNullOrEmpty()) ||
                (!clientCertPath.isNullOrEmpty() && !clientKeyPath.isNullOrEmpty()),
        ) {
            "Both $KEY_CLIENT_CERT_PATH and $KEY_CLIENT_KEY_PATH must be provided or neither must be provided"
        }

        val supportedSchemes = TlsConfig.supportedSchemes.joinToString(prefix = "(", postfix = ")")

        require(TlsConfig.isValidPath(caCertPath)) {
            "$KEY_CA_CERT_PATH must start with a valid scheme $supportedSchemes"
        }

        if (!clientCertPath.isNullOrEmpty()) {
            require(TlsConfig.isValidPath(clientCertPath)) {
                "$KEY_CLIENT_CERT_PATH must start with a valid scheme $supportedSchemes"
            }
        }

        if (!clientKeyPath.isNullOrEmpty()) {
            require(TlsConfig.isValidPath(clientKeyPath)) {
                "$KEY_CLIENT_KEY_PATH must start with a valid scheme $supportedSchemes"
            }
        }

        val clientKeyPassword = metaData.getString(KEY_CLIENT_KEY_PASSWORD)
        val skipVerify = metaData.getBoolean(KEY_INSECURE_SKIP_VERIFY, false)

        return if (!clientCertPath.isNullOrEmpty() && !clientKeyPath.isNullOrEmpty()) {
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
    }

    /** Extension to handle nullable Ints */
    private fun Bundle.getIntOrNull(key: String): Int? {
        return if (containsKey(key)) getInt(key) else null
    }

    companion object {
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
}
