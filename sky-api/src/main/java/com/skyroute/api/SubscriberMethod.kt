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
import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * Represents a method that subscribes to a specific topic in the MQTT system.
 * This data class holds information about the method to be invoked, its associated topic,
 * its description, and the thread mode in which the method should be executed.
 *
 * @param method The method to be invoked when a message is received on the associated topic.
 * @param description A human-readable description of the method.
 * @param threadMode The thread mode in which the method should be executed. Defaults to [ThreadMode.MAIN].
 * @param topic The topic that this method subscribed to.
 * @param qos The Quality of Service (QoS) level associated with this method.
 *
 * @author Andre Suryana
 */
internal data class SubscriberMethod(
    val method: Method,
    val description: String,
    val threadMode: ThreadMode = ThreadMode.MAIN,
    val topic: String,
    val qos: Int,
    val adapterClass: KClass<out PayloadAdapter> = DefaultPayloadAdapter::class,
)
