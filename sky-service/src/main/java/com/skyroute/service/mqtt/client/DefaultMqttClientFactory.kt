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

import org.eclipse.paho.mqttv5.client.IMqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttClientPersistence

/**
 * Default implementation of [MqttClientFactory] that creates real
 * [MqttAsyncClient] instances using provided configuration.
 *
 * @author Andre Suryana
 */
internal class DefaultMqttClientFactory : MqttClientFactory {

    override fun create(
        brokerUrl: String,
        clientId: String,
        persistence: MqttClientPersistence,
    ): IMqttAsyncClient {
        return MqttAsyncClient(brokerUrl, clientId, persistence)
    }
}
