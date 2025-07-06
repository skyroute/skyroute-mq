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
package com.skyroute.api

import com.skyroute.api.adapter.DefaultPayloadAdapter
import com.skyroute.core.adapter.PayloadAdapter
import com.skyroute.core.mqtt.MqttConfig
import com.skyroute.core.mqtt.TlsConfig
import com.skyroute.core.util.Logger
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A builder class for configuring and creating an instance of [SkyRoute].
 *
 * This builder provides a fluent API to customize core behaviour of the SkyRouteMQ messaging system,
 * such as event handling, task execution, and payload serialization.
 *
 * @author Andre Suryana
 */
class SkyRouteBuilder {

    // TODO: RFU, when topic received but there's no subscriber, we should throw exception
    internal var sendNoSubscriberEvent: Boolean = true
        private set

    // TODO: RFU, throw exception for method invocation failed
    internal var sendInvocationFailedEvent: Boolean = true
        private set

    internal var executorService: ExecutorService = Executors.newCachedThreadPool()
        private set

    internal var logger: Logger = Logger.Default()
        private set

    internal var payloadAdapter: PayloadAdapter = DefaultPayloadAdapter()
        private set

    internal var config: MqttConfig = MqttConfig()
        private set

    /**
     * Whether to emit an event when a message has no matching subscriber.
     */
    fun sendNoSubscriberEvent(sendNoSubscriberEvent: Boolean) = apply {
        this.sendNoSubscriberEvent = sendNoSubscriberEvent
    }

    /**
     * Whether to emit an event when a subscriber method throws an error.
     */
    fun sendInvocationFailedEvent(sendInvocationFailedEvent: Boolean) = apply {
        this.sendInvocationFailedEvent = sendInvocationFailedEvent
    }

    /**
     * Set a custom executor for the subscriber's method execution.
     */
    fun executorService(executorService: ExecutorService) = apply {
        this.executorService = executorService
    }

    /**
     * Sets a custom logger for all SkyRoute operations.
     */
    fun logger(logger: Logger) = apply {
        this.logger = logger
    }

    /**
     * Sets the payload adapter for serializing/deserializing messages.
     */
    fun payloadAdapter(payloadAdapter: PayloadAdapter) = apply {
        this.payloadAdapter = payloadAdapter
    }

    /**
     * Sets the MQTT broker URL, including host and port.
     *
     * Example: `"tcp://broker.example.com:1883"`
     */
    fun brokerUrl(brokerUrl: String) = apply {
        this.config.brokerUrl = brokerUrl
    }

    /**
     * Sets the prefix for generating a unique MQTT client ID.
     */
    fun clientPrefix(clientPrefix: String) = apply {
        this.config.clientPrefix = clientPrefix
    }

    /**
     * Whether to use a clean session on connect.
     */
    fun cleanStart(cleanStart: Boolean) = apply {
        this.config.cleanStart = cleanStart
    }

    /**
     * Sets how long (in seconds) the broker should persist the session after disconnect.
     */
    fun sessionExpiryInterval(sessionExpiryInterval: Int?) = apply {
        this.config.sessionExpiryInterval = sessionExpiryInterval
    }

    /**
     * Sets the connection timeout in seconds.
     */
    fun connectionTimeout(connectionTimeout: Int) = apply {
        this.config.connectionTimeout = connectionTimeout
    }

    /**
     * Sets the keep-alive interval in seconds.
     */
    fun keepAliveInterval(keepAliveInterval: Int) = apply {
        this.config.keepAliveInterval = keepAliveInterval
    }

    /**
     * Enables or disables automatic reconnect.
     */
    fun automaticReconnect(automaticReconnect: Boolean) = apply {
        this.config.automaticReconnect = automaticReconnect
    }

    /**
     * Sets the minimum and maximum delay (in seconds) between reconnect attempts.
     *
     * The client starts with [minDelay] and gradually increases the delay up to [maxDelay]
     * when reconnecting after a connection loss.
     */
    fun automaticReconnectDelay(minDelay: Int, maxDelay: Int) = apply {
        this.config.automaticReconnectMinDelay = minDelay
        this.config.automaticReconnectMaxDelay = maxDelay
    }

    /**
     * Sets the maximum total delay (in seconds) allowed between disconnection and the next reconnect attempt.
     *
     * This acts as an upper limit to prevent the reconnect mechanism from backing off indefinitely.
     * If reached, reconnect attempts will be made no later than this duration, even with backoff applied.
     */
    fun maxReconnectDelay(maxReconnectDelay: Int) = apply {
        this.config.maxReconnectDelay = maxReconnectDelay
    }

    /**
     * Sets the MQTT username and password for broker authentication.
     */
    fun credentials(username: String, password: String) = apply {
        this.config.username = username
        this.config.password = password
    }

    /**
     * Sets the TLS configuration (e.g., for TLS or mTLS)
     */
    fun tlsConfig(tlsConfig: TlsConfig) = apply {
        this.config.tlsConfig = tlsConfig
    }

    /**
     * Builds and returns a configured [SkyRoute] instance.
     */
    fun build(): SkyRoute {
        return SkyRoute(this)
    }
}
