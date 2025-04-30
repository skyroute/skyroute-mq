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
package com.skyroute.service

import com.skyroute.service.config.MqttConfig

/**
 * Defines the basic MQTT operations for connecting, disconnecting,
 * and checking connection status.
 *
 * @author Andre Suryana
 */
interface MqttController {
    /**
     * Connects to the MQTT broker using with the given configuration.
     * Replaces any existing connection.
     *
     * @param config The configuration used for connecting to the broker.
     */
    fun connect(config: MqttConfig)

    /**
     * Disconnects from the MQTT broker connection.
     */
    fun disconnect()

    /**
     * Checks whether the client is currently connected to the MQTT broker.
     *
     * @return `true` if the client is connected, `false` otherwise.
     */
    fun isConnected(): Boolean
}
