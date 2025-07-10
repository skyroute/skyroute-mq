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
package com.skyroute.service.mqtt.client

import com.skyroute.core.mqtt.MqttConfig
import com.skyroute.core.mqtt.TlsConfig
import com.skyroute.service.mqtt.socket.MqttSocketFactory
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence

/**
 * Default implementation of [MqttClientFactory] that creates real
 * [MqttAsyncClient] instances using provided configuration.
 *
 * @param persistenceDir The directory where persistent data will be stored.
 * @param socketFactory A factory for creating [MqttSocketFactory] instances.
 *
 * @author Andre Suryana
 */
internal class DefaultMqttClientFactory(
    private val persistenceDir: String,
    private val socketFactory: MqttSocketFactory,
) : MqttClientFactory {

    override fun create(config: MqttConfig): MqttClientBundle {
        val client = MqttAsyncClient(
            config.brokerUrl,
            config.clientId,
            MqttDefaultFilePersistence(persistenceDir),
        )

        val options = MqttConnectionOptions().apply {
            serverURIs = arrayOf(config.brokerUrl)
            isCleanStart = config.cleanStart
            config.sessionExpiryInterval?.let {
                sessionExpiryInterval = it.toLong()
            }
            connectionTimeout = config.connectionTimeout
            keepAliveInterval = config.keepAliveInterval
            isAutomaticReconnect = config.automaticReconnect
            setAutomaticReconnectDelay(
                config.automaticReconnectMinDelay,
                config.automaticReconnectMaxDelay,
            )
            maxReconnectDelay = config.maxReconnectDelay

            config.username?.let { userName = it }
            config.password?.let { password = it.toByteArray() }

            if (config.tlsConfig !is TlsConfig.None) {
                socketFactory = this@DefaultMqttClientFactory.socketFactory.create(config.tlsConfig)
            }
        }

        return MqttClientBundle(client, options)
    }
}
