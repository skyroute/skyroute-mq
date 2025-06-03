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
package com.skyroute.core.adapter

/**
 * Interface for serializing and deserializing MQTT message payloads.
 *
 * Implementations of [PayloadAdapter] are responsible for:
 * - Converting an object into a [ByteArray] for publishing (`encode`)
 * - Converting a [ByteArray] received from the broker into a specific type (`decode`)
 *
 * This allows SkyRouteMQ to support different data formats such as JSON, XML,
 * Protobuf, or plain UTF-8 strings via plugin modules.
 *
 * @author Andre Suryana
 */
interface PayloadAdapter {

    /**
     * TODO: Way to install PayloadAdapter (remove me later!)
     * 1. Global
     *    Creating builder-pattern to accommodate `setPayloadAdapter` method.
     * 2. Per-Subscriber/Per-Publisher
     *    Creating new property in `@Subscribe` annotation, e.g. `@Subscribe(topic = "...", payloadAdapter = GsonPayloadAdapter::class)`.
     *    For publish, use `SkyRoute.publish(topic, message, payloadAdapter = GsonPayloadAdapter::class)`.
     */

    /**
     * Content type indicator (e.g., "application/json", "text/plain").
     * This can be used for logging or setting MQTT 5.0 user properties.
     */
    val contentType: String

    /**
     * Encodes an object into a [ByteArray] that will be sent over MQTT.
     *
     * @param payload The object to encode.
     * @return The encoded bytes.
     */
    fun <T> encode(payload: T): ByteArray

    /**
     * Decodes a [ByteArray] received from MQTT into an object of the desired type.
     *
     * @param payload The received byte array.
     * @param type The target class type to decode into.
     */
    fun <T> decode(payload: ByteArray, type: Class<T>): T
}
