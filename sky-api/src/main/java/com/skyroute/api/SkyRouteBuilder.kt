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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A builder class for configuring and creating an instance of [SkyRoute].
 *
 * This builder provides a fluent API to customize core behaviour of the SkyRouteMQ messaging system,
 * such as event handling, task execution, and payload serialization.
 *
 * Example usage:
 * ```
 * val skyRoute = SkyRouteBuilder()
 *     .sendNoSubscriberEvent(true)
 *     .sendInvocationFailedEvent(true)
 *     .executorService(Executors.newSingleThreadExecutor())
 *     .payloadAdapter(GsonPayloadAdapter())
 * ```
 *
 * @author Andre Suryana
 */
class SkyRouteBuilder {

    /**
     * Whether SkyRoute should emit an event when a message arrives for which no subscriber exists.
     *
     * If `true`, the system can emit an event or throw an exception to indicate that no subscriber handled the message.
     * Default is `true`.
     *
     * TODO: RFU, when topic received but there's no subscriber, we should throw exception
     */
    var sendNoSubscriberEvent = true
        private set

    /**
     * Whether SkyRoute should emit an event or throw an exception when a subscriber method fails to execute.
     *
     * If `true`, the failure will be propagated through a custom mechanism or logged.
     * Default is `true`.
     *
     * TODO: RFU, throw exception for method invocation failed
     */
    var sendInvocationFailedEvent = true
        private set

    /**
     * The [ExecutorService] used for dispatching message callbacks to subscribers.
     *
     * By default, this uses a cached thread pool, but you can customize it to match your desired concurrency model.
     */
    var executorService = DEFAULT_EXECUTOR_SERVICE
        private set

    /**
     * The [PayloadAdapter] used for encoding and decoding message payloads.
     *
     * By default, it uses [DefaultPayloadAdapter], which supports basic types such as String, Int, Boolean, etc.
     * You can provide a custom adapter such as Gson, Moshi, or XML-based adapters.
     */
    var payloadAdapter: PayloadAdapter = DefaultPayloadAdapter
        private set

    /**
     * Set whether to emit an event when no subscriber is found for a received message.
     *
     * @param sendNoSubscriberEvent Whether to enable this feature.
     * @return The current builder instance for chaining.
     */
    fun sendNoSubscriberEvent(sendNoSubscriberEvent: Boolean): SkyRouteBuilder {
        this.sendNoSubscriberEvent = sendNoSubscriberEvent
        return this
    }

    /**
     * Set whether to emit an event or exception when a subscriber method invocation fails.
     *
     * @param sendInvocationFailedEvent Whether to enable this feature.
     * @return The current builder instance for chaining.
     */
    fun sendInvocationFailedEvent(sendInvocationFailedEvent: Boolean): SkyRouteBuilder {
        this.sendInvocationFailedEvent = sendInvocationFailedEvent
        return this
    }

    /**
     * Set a custom [ExecutorService] for subscriber method execution.
     *
     * @param executorService The executor service to use.
     * @return The current builder instance for chaining.
     */
    fun executorService(executorService: ExecutorService): SkyRouteBuilder {
        this.executorService = executorService
        return this
    }

    /**
     * Set a custom [PayloadAdapter] for message serialization and deserialization.
     *
     * @param payloadAdapter The payload adapter to use.
     * @return The current builder instance for chaining.
     */
    fun payloadAdapter(payloadAdapter: PayloadAdapter): SkyRouteBuilder {
        this.payloadAdapter = payloadAdapter
        return this
    }

    /**
     * Build and return a fully configured [SkyRoute] instance.
     *
     * @return The constructed [SkyRoute] object.
     */
    fun build(): SkyRoute {
        return SkyRoute(this)
    }

    companion object {
        /**
         * Default executor service used if none is specified.
         * Uses a cached thread pool which creates new threads as needed and reuses previously constructed threads.
         */
        val DEFAULT_EXECUTOR_SERVICE: ExecutorService = Executors.newCachedThreadPool()
    }
}
