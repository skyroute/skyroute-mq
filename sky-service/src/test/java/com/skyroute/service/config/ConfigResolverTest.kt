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
package com.skyroute.service.config

import android.os.Bundle
import com.skyroute.core.mqtt.MqttConfig.Companion.DEFAULT_CLEAN_START
import com.skyroute.core.mqtt.MqttConfig.Companion.DEFAULT_CLIENT_PREFIX
import com.skyroute.core.mqtt.MqttConfig.Companion.DEFAULT_CONNECTION_TIMEOUT
import com.skyroute.core.mqtt.MqttConfig.Companion.DEFAULT_KEEP_ALIVE_INTERVAL
import com.skyroute.core.mqtt.TlsConfig
import com.skyroute.core.util.TestLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * @author Andre Suryana
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE)
class ConfigResolverTest {

    private lateinit var metaData: Bundle
    private val logger = TestLogger()

    @Before
    fun setUp() {
        metaData = Bundle()
    }

    @Test
    fun `returns config with default values when optional fields are missing`() {
        metaData.putString("mqttBrokerUrl", "tcp://localhost:1883")
        val config = ConfigResolver(metaData, logger).resolve()

        assertEquals("tcp://localhost:1883", config.brokerUrl)
        assertEquals(DEFAULT_CLIENT_PREFIX, config.clientPrefix)
        assertEquals(DEFAULT_CLEAN_START, config.cleanStart)
        assertEquals(DEFAULT_CONNECTION_TIMEOUT, config.connectionTimeout)
        assertEquals(DEFAULT_KEEP_ALIVE_INTERVAL, config.keepAliveInterval)
    }

    @Test
    fun `throws when brokerUrl is missing`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            ConfigResolver(metaData, logger).resolve()
        }
        assertTrue(exception.message!!.contains("Missing required value: mqttBrokerUrl"))
    }

    @Test
    fun `throws when brokerUrl scheme is invalid`() {
        metaData.putString("mqttBrokerUrl", "http://localhost:1883")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            ConfigResolver(metaData, logger).resolve()
        }
        assertTrue(exception.message!!.contains("Invalid value for mqttBrokerUrl"))
    }

    @Test
    fun `throws when reconnect delay constraints are invalid`() {
        metaData.putString("mqttBrokerUrl", "tcp://localhost:1883")
        metaData.putInt("autoReconnectMinDelay", 10)
        metaData.putInt("autoReconnectMaxDelay", 5)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            ConfigResolver(metaData, logger).resolve()
        }
        println("Exception message: ${exception.message}")
        assertTrue(exception.message!!.contains("autoReconnectMaxDelay must be greater than or equal to autoReconnectMinDelay"))
    }

    @Test
    fun `throws when only one of client cert or key is provided`() {
        metaData.putString("mqttBrokerUrl", "ssl://localhost:1883")
        metaData.putString("caCertPath", "asset://certs/ca.crt")
        metaData.putString("clientCertPath", "asset://certs/client.key")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            ConfigResolver(metaData, logger).resolve()
        }
        assertTrue(exception.message!!.contains("Both clientCertPath and clientKeyPath must be provided"))
    }

    @Test
    fun `sets tlsConfig as ServerAuth when only caCert is set`() {
        metaData.putString("mqttBrokerUrl", "ssl://localhost:8883")
        metaData.putString("caCertPath", "asset://certs/ca.crt")

        val config = ConfigResolver(metaData, logger).resolve()
        assertTrue(config.tlsConfig is TlsConfig.ServerAuth)
    }

    @Test
    fun `sets tlsConfig as MutualAuth when all certs are provided`() {
        metaData.putString("mqttBrokerUrl", "ssl://localhost:8883")
        metaData.putString("caCertPath", "asset://certs/ca.crt")
        metaData.putString("clientCertPath", "asset://certs/client.crt")
        metaData.putString("clientKeyPath", "asset://certs/client.key")

        val config = ConfigResolver(metaData, logger).resolve()
        assertTrue(config.tlsConfig is TlsConfig.MutualAuth)
    }

    @Test
    fun `throws when certs and key path prefix scheme is invalid`() {
        metaData.putString("mqttBrokerUrl", "ssl://localhost:8883")
        metaData.putString("caCertPath", "certs/ca.crt")
        metaData.putString("clientCertPath", "assets/client.crt")
        metaData.putString("clientKeyPath", "client.key")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            ConfigResolver(metaData, logger).resolve()
        }
        assertTrue(exception.message!!.contains("must start with a valid scheme"))
    }

    @Test
    fun `uses TlsConfig Default when brokerUrl scheme is ssl but no CA or client cert is provided`() {
        metaData.putString("mqttBrokerUrl", "ssl://localhost:8883")

        val config = ConfigResolver(metaData, logger).resolve()
        assertTrue(config.tlsConfig is TlsConfig.Default)
    }

    @Test
    fun `uses TlsConfig None when brokerUrl scheme is tcp regardless of CA or client cert presence`() {
        metaData.putString("mqttBrokerUrl", "tcp://localhost:8883")
        metaData.putString("caCertPath", "asset://certs/ca.crt")
        metaData.putString("clientCertPath", "asset://certs/client.crt")
        metaData.putString("clientKeyPath", "asset://certs/client.key")

        val config = ConfigResolver(metaData, logger).resolve()
        assertTrue(config.tlsConfig is TlsConfig.None)
    }
}
