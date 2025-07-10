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
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient

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
     * @param config the configuration for the MQTT client.
     * @return a new [IMqttAsyncClient] instance.
     */
    fun create(config: MqttConfig): MqttClientBundle
}
