package com.skyroute.core.mqtt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.io.InputStream

class MqttConfigTest {

    private val mockTlsConfig = TlsConfig(
        caCertInput = InputStream.nullInputStream(),
        clientCertInput = null,
        clientKeyInput = null,
        clientKeyPassword = null
    )

    @RepeatedTest(5)
    fun `getClientId should generate unique client IDs with default prefix`() {
        val config = MqttConfig(brokerUrl = "tcp://test", clientPrefix = "skyroute")
        val clientId = config.getClientId()

        assertTrue(clientId.startsWith("skyroute-"))
        assertTrue(clientId.substringAfter("skyroute-").toLongOrNull() != null)
    }

    @Test
    fun `getClientId should not insert extra hyphen when prefix ends with hyphen`() {
        val config = MqttConfig(brokerUrl = "tcp://test", clientPrefix = "client-")
        val clientId = config.getClientId()

        assertTrue(clientId.startsWith("client-"))
        assertEquals(1, clientId.count { it == '-' }) // only one hyphen at end of prefix
    }

    @Test
    fun `should correctly assign custom values`() {
        val config = MqttConfig(
            brokerUrl = "ssl://custom-broker:8883",
            clientPrefix = "myclient",
            cleanStart = false,
            sessionExpiryInterval = 60,
            connectionTimeout = 10,
            keepAliveInterval = 20,
            automaticReconnect = false,
            automaticReconnectMinDelay = 2,
            automaticReconnectMaxDelay = 30,
            maxReconnectDelay = 1000,
            username = "user",
            password = "pass",
            tlsConfig = mockTlsConfig,
        )

        assertEquals("ssl://custom-broker:8883", config.brokerUrl)
        assertEquals(false, config.cleanStart)
        assertEquals(60, config.sessionExpiryInterval)
        assertEquals(10, config.connectionTimeout)
        assertEquals(20, config.keepAliveInterval)
        assertEquals(false, config.automaticReconnect)
        assertEquals(2, config.automaticReconnectMinDelay)
        assertEquals(30, config.automaticReconnectMaxDelay)
        assertEquals(1000, config.maxReconnectDelay)
        assertEquals("user", config.username)
        assertEquals("pass", config.password)
        assertNotNull(config.tlsConfig)
    }
}
