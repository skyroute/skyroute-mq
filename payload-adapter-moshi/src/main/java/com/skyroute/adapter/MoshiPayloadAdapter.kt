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
package com.skyroute.adapter

import com.skyroute.core.adapter.PayloadAdapter
import com.squareup.moshi.Moshi

/**
 * A [PayloadAdapter] implementation using Moshi for JSON serialization.
 *
 * @author Andre Suryana
 */
class MoshiPayloadAdapter(
    private val moshi: Moshi = Moshi.Builder().build(),
) : PayloadAdapter {

    override val contentType: String = "application/json"

    override fun <T> encode(payload: T, type: Class<out T>): ByteArray {
        val json = moshi.adapter<T>(type).toJson(payload)
        return json.toByteArray(Charsets.UTF_8)
    }

    override fun <T> decode(payload: ByteArray, type: Class<T>): T {
        val json = payload.toString(Charsets.UTF_8)
        return moshi.adapter(type).fromJson(json)
            ?: throw IllegalStateException("Failed to decode JSON")
    }
}
