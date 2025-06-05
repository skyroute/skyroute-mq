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

import android.os.Binder
import com.skyroute.core.message.TopicMessenger

/**
 * A [Binder] subclass used for binding clients to the [SkyRouteService].
 *
 * This binder exposes core components of the service, allowing bound components such as activities
 * or other services to interact with MQTT operations through [TopicMessenger] or to gain low-level
 * access via [MqttController].
 *
 * Usage example:
 * ```
 * val binder = service as SkyRouteService.SkyRouteBinder
 * val messenger = binder.getTopicMessenger()
 * messenger.subscribe("some/topic")
 * ```
 *
 * This pattern is common in Android services for exposing internal logic to bound clients.
 */
class SkyRouteBinder internal constructor(
    private val service: SkyRouteService,
) : Binder() {

    /**
     * Provides access to the [TopicMessenger] instance, allowing publish/subscribe interactions.
     *
     * @return The [TopicMessenger] implementation associated with the service.
     */
    fun getTopicMessenger(): TopicMessenger = service

    /**
     * Provides access to the underlying MQTT operations through the [MqttController] instance.
     *
     * @return The [MqttController] implementation associated with the service.
     */
    fun getMqttController(): MqttController = service
}
