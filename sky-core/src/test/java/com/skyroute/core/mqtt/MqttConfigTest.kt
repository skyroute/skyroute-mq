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
package com.skyroute.core.mqtt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @author Andre Suryana
 */
class MqttConfigTest {

    @Test
    fun `clientId should use default prefix when clientPrefix not provided`() {
        val config = MqttConfig(brokerUrl = "tcp://test")
        val clientId = config.clientId

        val prefixWithHyphen = MqttConfig.DEFAULT_CLIENT_PREFIX + "-"

        assertTrue(clientId.startsWith(prefixWithHyphen))
        assertTrue(clientId.substringAfter(prefixWithHyphen).toLongOrNull() != null)
    }

    @Test
    fun `clientId should not insert extra hyphen when prefix ends with hyphen`() {
        val config = MqttConfig(brokerUrl = "tcp://test", clientPrefix = "client-")
        val clientId = config.clientId

        assertTrue(clientId.startsWith("client-"))
        assertEquals(1, clientId.count { it == '-' }) // only one hyphen at end of prefix
    }

    @Test
    fun `clientId should generate unique values`() {
        for (i in 1..10) {
            val config = MqttConfig(brokerUrl = "tcp://test/$i")
            val clientId1 = config.clientId
            val clientId2 = config.clientId

            assertTrue(clientId1 != clientId2)
        }
    }
}
