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

import com.google.gson.Gson
import com.skyroute.core.adapter.PayloadAdapter
import com.skyroute.core.util.Logger

/**
 * A [PayloadAdapter] implementation using Gson for JSON serialization.
 *
 * @author Andre Suryana
 */
class GsonPayloadAdapter(
    private val gson: Gson = Gson(),
    private val logger: Logger = Logger.Default(),
) : PayloadAdapter {

    override val contentType: String = "application/json"

    override fun <T> encode(payload: T, type: Class<out T>): ByteArray {
        val json = gson.toJson(payload, type)
        logger.d(TAG, "encode: $json")
        return json.toByteArray(Charsets.UTF_8)
    }

    override fun <T> decode(payload: ByteArray, type: Class<T>): T {
        val json = payload.toString(Charsets.UTF_8)
        logger.d(TAG, "decode: $json")
        return gson.fromJson(json, type)
    }

    companion object {
        private const val TAG = "GsonPayloadAdapter"
    }
}
