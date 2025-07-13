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
import com.skyroute.core.adapter.decode
import com.skyroute.core.adapter.encode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * @author Andre Suryana
 */
class DefaultPayloadAdapterTest {

    private val adapter: PayloadAdapter = DefaultPayloadAdapter()

    @Test
    fun `test encode and decode String`() {
        val original = "Sky Route!"
        val bytes = adapter.encode(original)
        val decoded = adapter.decode<String>(bytes)
        assertEquals(original, decoded)
    }

    @Test
    fun `test encode and decode Int`() {
        val original = 42
        val bytes = adapter.encode(original)
        val decoded = adapter.decode<Int>(bytes)
        assertEquals(original, decoded)
    }

    @Test
    fun `test encode and decode Long`() {
        val original = 123456789L
        val bytes = adapter.encode(original)
        val decoded = adapter.decode<Long>(bytes)
        assertEquals(original, decoded)
    }

    @Test
    fun `test encode and decode Double`() {
        val original = 3.14159
        val bytes = adapter.encode(original)
        val decoded = adapter.decode<Double>(bytes)
        assertEquals(original, decoded, 0.0001)
    }

    @Test
    fun `test encode and decode Float`() {
        val original = 2.71828f
        val bytes = adapter.encode(original)
        val decoded = adapter.decode<Float>(bytes)
        assertEquals(original, decoded, 0.0001f)
    }

    @Test
    fun `test encode and decode Boolean`() {
        val original = true
        val bytes = adapter.encode(original)
        val decoded = adapter.decode<Boolean>(bytes)
        assertEquals(original, decoded)
    }

    @Test
    fun `test unsupported encode type`() {
        val original = object {}
        assertThrows(IllegalArgumentException::class.java) {
            adapter.encode(original)
        }
    }

    @Test
    fun `test unsupported decode type`() {
        val bytes = "Sky Route!".toByteArray()
        assertThrows(IllegalArgumentException::class.java) {
            adapter.decode<Any>(bytes)
        }
    }
}
