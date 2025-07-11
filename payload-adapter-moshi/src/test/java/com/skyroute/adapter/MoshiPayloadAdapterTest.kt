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
import com.skyroute.core.adapter.decode
import com.skyroute.core.adapter.encode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * @author Andre Suryana
 */
class MoshiPayloadAdapterTest {

    private val adapter: PayloadAdapter = MoshiPayloadAdapter()

    @Test
    fun `test encode and decode object`() {
        val original = TestObject("Test", 123)

        val encoded = adapter.encode(original)
        val decoded = adapter.decode<TestObject>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `test encode and decode primitive types`() {
        val originalString = "Hello, World!"
        val originalInt = 42
        val originalLong = 123456789L
        val originalDouble = 3.14159
        val originalFloat = 2.71828f
        val originalBoolean = true

        val encodedString = adapter.encode(originalString)
        val encodedInt = adapter.encode(originalInt)
        val encodedLong = adapter.encode(originalLong)
        val encodedDouble = adapter.encode(originalDouble)
        val encodedFloat = adapter.encode(originalFloat)
        val encodedBoolean = adapter.encode(originalBoolean)

        val decodedString = adapter.decode<String>(encodedString)
        val decodedInt = adapter.decode<Int>(encodedInt)
        val decodedLong = adapter.decode<Long>(encodedLong)
        val decodedDouble = adapter.decode<Double>(encodedDouble)
        val decodedFloat = adapter.decode<Float>(encodedFloat)
        val decodedBoolean = adapter.decode<Boolean>(encodedBoolean)

        assertEquals(decodedString, originalString)
        assertEquals(decodedInt, originalInt)
        assertEquals(decodedLong, originalLong)
        assertEquals(decodedDouble, originalDouble, 0.0001)
        assertEquals(decodedFloat, originalFloat)
        assertEquals(decodedBoolean, originalBoolean)
    }
}
