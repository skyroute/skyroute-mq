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
package com.skyroute.service.mqtt.socket

import android.content.Context
import com.skyroute.core.mqtt.TlsConfig
import com.skyroute.service.mqtt.socket.cert.CertLoaderFactory
import javax.net.SocketFactory
import javax.net.ssl.SSLSocketFactory

/**
 * Creates platform-specific [SocketFactory] instance based on [TlsConfig].
 *
 * This factory supports the following TLS configurations:
 * - [TlsConfig.None] - Plain TCP socket (no TLS).
 * - [TlsConfig.Default] - Default system TLS (via [SSLSocketFactory.getDefault]).
 * - [TlsConfig.ServerAuth] - One-way TLS using a CA certificate.
 * - [TlsConfig.MutualAuth] - Mutual TLS using CA + client certificate + client private key.
 *
 * @author Andre Suryana
 */
class MqttSocketFactory(private val context: Context) {

    /**
     * Creates a [SocketFactory] instance based on the given TLS configuration.
     *
     * @param config TLS configuration for the MQTT connection.
     * @return A configured [SocketFactory] instance.
     */
    fun create(config: TlsConfig): SocketFactory = when (config) {
        is TlsConfig.None -> SocketFactory.getDefault()

        is TlsConfig.Default -> SSLSocketFactory.getDefault()

        is TlsConfig.ServerAuth -> {
            val caLoader = CertLoaderFactory(context).create(config.caCertPath)
            ServerAuthSocketFactory(caLoader).create(config)
        }

        is TlsConfig.MutualAuth -> {
            val factory = CertLoaderFactory(context)

            val caLoader = factory.create(config.caCertPath)
            val clientCertLoader = factory.create(config.clientCertPath)
            val clientKeyLoader = factory.create(config.clientKeyPath)

            MutualAuthSocketFactory(caLoader, clientCertLoader, clientKeyLoader).create(config)
        }
    }
}
