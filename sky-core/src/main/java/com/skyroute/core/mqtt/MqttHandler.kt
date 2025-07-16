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

/**
 * Interface for handling MQTT operations.
 *
 * @author Andre Suryana
 */
interface MqttHandler {

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

    /**
     * Subscribes to a topic with a specified QoS.
     *
     * @param topic The topic to subscribe to.
     * @param qos The Quality of Service level (0, 1, or 2, default is 0).
     */
    fun subscribe(topic: String, qos: Int = 0)

    /**
     * Unsubscribes from a topic.
     *
     * @param topic The topic to unsubscribe from.
     */
    fun unsubscribe(topic: String)

    /**
     * Publish a message to a topic with a specified QoS.
     *
     * @param topic The topic to publish to.
     * @param message The message payload to publish in [ByteArray].
     * @param qos The Quality of Service level (0, 1, or 2, default is 0).
     * @param retain Whether to retain the message (default is `false`).
     */
    fun publish(topic: String, message: ByteArray, qos: Int = 0, retain: Boolean = false, ttlInSeconds: Long? = null)

    /**
     * Register a callback to handle incoming messages for a specific topic.
     *
     * @param callback A function that processes the received message.
     */
    fun onMessageArrival(callback: OnMessageArrival)

    /**
     * Register a callback to handle disconnection events.
     *
     * @param callback A function that processes the disconnection event.
     */
    fun onDisconnect(callback: OnDisconnect)

    /**
     * Checks if there are any pending MQTT requests queued for execution.
     *
     * This can be used to determine whether any subscribe, unsubscribe, or publish
     * requests are waiting to be executed once the MQTT connection is established.
     *
     * @return `true` if there is at least one pending request, `false` otherwise.
     */
    fun hasPendingRequests(): Boolean

    /**
     * Returns the current MQTT client ID.
     *
     * This is typically the client ID used when establishing the MQTT connection.
     * If the connection has not been established yet or already disconnected,
     * this will return `null`.
     *
     * @return The current client ID, or `null` if not connected.
     */
    fun getClientId(): String?
}
