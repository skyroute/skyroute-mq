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
package com.skyroute.api.adapter

import com.skyroute.core.adapter.PayloadAdapter

/**
 * A default [PayloadAdapter] that handles primitive types using UTF-8 encoding.
 *
 * Supported types: [String], [Int], [Long], [Double], [Float], [Boolean]
 *
 * @author Andre Suryana
 */
object DefaultPayloadAdapter : PayloadAdapter {

    override val contentType: String = "text/plain"

    override fun <T> encode(payload: T): ByteArray {
        return when (payload) {
            is String,
            is Int,
            is Long,
            is Double,
            is Float,
            is Boolean,
            ->
                payload.toString().toByteArray(Charsets.UTF_8)

            else -> throw IllegalArgumentException("Unsupported encode type: ${payload!!::class.java}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> decode(payload: ByteArray, type: Class<T>): T {
        val str = payload.toString(Charsets.UTF_8)
        return when (type) {
            String::class.java -> str as T
            Int::class.java, java.lang.Integer::class.java -> str.toInt() as T
            Long::class.java, java.lang.Long::class.java -> str.toLong() as T
            Double::class.java, java.lang.Double::class.java -> str.toDouble() as T
            Float::class.java, java.lang.Float::class.java -> str.toFloat() as T
            Boolean::class.java, java.lang.Boolean::class.java -> str.toBoolean() as T
            else -> throw IllegalArgumentException("Unsupported decode type: ${type.name}")
        }
    }
}
