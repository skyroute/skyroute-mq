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
package com.skyroute.service.mqtt

import android.content.Context
import com.skyroute.core.mqtt.MqttConfig
import com.skyroute.core.util.Logger
import com.skyroute.service.mqtt.client.MqttClientFactory
import com.skyroute.service.mqtt.persistence.PersistenceFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttClientPersistence
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@ExtendWith(MockitoExtension::class)
class MqttConnectionHandlerTest {

    @Mock
    private lateinit var clientFactory: MqttClientFactory

    @Mock
    private lateinit var persistenceFactory: PersistenceFactory

    @Mock
    private lateinit var mqttClient: IMqttAsyncClient

    @Mock
    private lateinit var persistence: MqttClientPersistence

    @Mock
    private lateinit var logger: Logger

    @Captor
    private lateinit var callbackCaptor: ArgumentCaptor<MqttCallback>

    private lateinit var handler: MqttConnectionHandler

    @BeforeEach
    fun setUp() {
        reset(mqttClient, clientFactory, persistenceFactory, logger)

        lenient().whenever(persistenceFactory.create()).thenReturn(persistence)
        lenient().whenever(clientFactory.create(any(), any(), any())).thenReturn(mqttClient)

        handler = MqttConnectionHandler(
            context = mock(Context::class.java),
            logger = logger,
            clientFactory = clientFactory,
            persistenceFactory = persistenceFactory,
        )
    }

    @Test
    fun `connect should create client and call connect`() {
        val config = MqttConfig("tcp://localhost:1883", cleanStart = true, clientPrefix = "test")
        handler.connect(config)

        verify(clientFactory).create(eq(config.brokerUrl), any(), eq(persistence))
        verify(mqttClient).setCallback(any())
        verify(mqttClient).connect(any())
    }

    @Test
    fun `disconnect should disconnect client and clear state`() {
        val config = MqttConfig("tcp://localhost:1883", cleanStart = true, clientPrefix = "test")
        handler.connect(config)

        whenever(mqttClient.isConnected).thenReturn(true)

        handler.disconnect()

        verify(mqttClient).disconnect()
        assertNull(handler.getClientId())
        assertFalse(handler.hasPendingRequests())
    }

    @Test
    fun `pending requests are executed on connectComplete`() {
        val config = MqttConfig("tcp://localhost:1883", cleanStart = true, clientPrefix = "test")

        val spyHandler = spy(handler)
        spyHandler.connect(config)

        // Initially disconnected, queue the request
        whenever(spyHandler.isConnected()).thenReturn(false)
        spyHandler.subscribe("topic/pending", 0)

        verify(mqttClient).setCallback(callbackCaptor.capture())

        // Simulate MQTT connected state
        whenever(spyHandler.isConnected()).thenReturn(true)
        callbackCaptor.value.connectComplete(false, config.brokerUrl)

        verify(mqttClient).subscribe("topic/pending", 0)
        assertFalse(handler.hasPendingRequests())
    }

    @Test
    fun `subscribe should invoke client immediately when connected`() {
        val config = MqttConfig("tcp://localhost:1883", cleanStart = true, clientPrefix = "test")
        handler.connect(config)

        whenever(mqttClient.isConnected).thenReturn(true)

        handler.subscribe("test/topic", 1)

        verify(mqttClient, timeout(500)).subscribe(eq("test/topic"), eq(1))
    }

    @Test
    fun `subscribe should add to pending if not connected`() {
        handler.subscribe("test/topic", 1)

        assertTrue(handler.hasPendingRequests())
    }

    @Test
    fun `unsubscribe should add to pending if not connected`() {
        handler.unsubscribe("test/topic")

        assertTrue(handler.hasPendingRequests())
    }

    @Test
    fun `publish should queue request if not connected`() {
        val payload = "hello".toByteArray()
        handler.publish("topic", payload, 1, false, null)

        assertTrue(handler.hasPendingRequests())
    }

    @Test
    fun `onMessageArrival should invoke callback`() {
        val callbackCaptor = argumentCaptor<MqttCallback>()
        val payload = "test-msg".toByteArray()

        whenever(clientFactory.create(any(), any(), any())).thenReturn(mqttClient)
        whenever(mqttClient.connect(any())).thenReturn(mock())

        val config = MqttConfig("tcp://localhost:1883", cleanStart = true, clientPrefix = "test")
        handler.connect(config)

        verify(mqttClient).setCallback(callbackCaptor.capture())
        val mockMessage = MqttMessage(payload)

        var received: ByteArray? = null
        handler.onMessageArrival { _, msg -> received = msg }

        callbackCaptor.firstValue.messageArrived("topic", mockMessage)

        assertArrayEquals(payload, received)
    }

    @Test
    fun `hasPendingRequests should return true after subscribe called before connected`() {
        val config = MqttConfig("tcp://test", cleanStart = true, clientPrefix = "test")

        handler.connect(config)
        handler.subscribe("test/topic", 1)

        assertTrue(handler.hasPendingRequests())
    }

    @Test
    fun `publish should queue if not connected`() {
        val config = MqttConfig("tcp://test", cleanStart = true, clientPrefix = "test")

        handler.connect(config)
        handler.publish("topic/test", "Hello".toByteArray(), qos = 1, retain = false)

        assertTrue(handler.hasPendingRequests())
    }

    @Test
    fun `unsubscribe should queue if not connected`() {
        val config = MqttConfig("tcp://test", cleanStart = true, clientPrefix = "test")

        handler.connect(config)
        handler.unsubscribe("topic/test")

        assertTrue(handler.hasPendingRequests())
    }
}
