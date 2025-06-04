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

import com.skyroute.core.adapter.DefaultPayloadAdapter
import com.skyroute.core.adapter.PayloadAdapter
import kotlin.reflect.KClass

/**
 * Identifies a method as a subscriber to a topic in the SkyRouteMQ event system.
 *
 * Methods annotated with `@Subscribe` will be automatically registered to receive
 * messages published to the specified topic, and executed according to the defined [threadMode].
 *
 * Usage example:
 * ```
 * @Subscribe(topic = "test/topic", threadMode = ThreadMode.BACKGROUND)
 * fun onMessageReceived(message: String) {
 *     // Handle the message
 * }
 * ```
 *
 * @property topic The MQTT topic to subscribe to.
 * @property qos The Quality of Service (QoS) level for the subscription.
 * @property threadMode The thread mode for executing the subscriber. Defaults to [ThreadMode.MAIN].
 *
 * @author Andre Suryana
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Subscribe(
    val topic: String,
    val qos: Int = 0,
    val threadMode: ThreadMode = ThreadMode.MAIN,
    val adapter: KClass<out PayloadAdapter> = DefaultPayloadAdapter::class,
)
