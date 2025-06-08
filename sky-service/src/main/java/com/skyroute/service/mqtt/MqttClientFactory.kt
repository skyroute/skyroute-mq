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
package com.skyroute.service.mqtt

import org.eclipse.paho.mqttv5.client.IMqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttClientPersistence

/**
 * Factory interface for creating MQTT client instances.
 *
 * This abstraction allows the creation of custom or mock [IMqttAsyncClient] instances,
 * useful for testing and decoupling the client instantiation from the handler logic.
 *
 * @author Andre Suryana
 */
interface MqttClientFactory {

    /**
     * Creates a new [IMqttAsyncClient] instance configured with the given parameters.
     *
     * @param brokerUrl The MQTT broker URI.
     * @param clientId The client ID to be used when connecting.
     * @param persistence The persistence mechanism for the client session.
     * @return a new [IMqttAsyncClient] instance.
     */
    fun create(
        brokerUrl: String,
        clientId: String,
        persistence: MqttClientPersistence,
    ): IMqttAsyncClient
}
