package com.skyroute.core.mqtt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

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

    @RepeatedTest(5)
    fun `clientId should generate unique values`() {
        val config = MqttConfig(brokerUrl = "tcp://test")
        val clientId1 = config.clientId
        val clientId2 = config.clientId

        assertTrue(clientId1 != clientId2)
    }
}
