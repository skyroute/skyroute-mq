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
import com.skyroute.core.mqtt.MqttHandler
import com.skyroute.core.mqtt.OnDisconnect
import com.skyroute.core.mqtt.OnMessageArrival
import com.skyroute.core.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import java.io.File

/**
 * Implementation of the MQTT handler with Paho MQTT v5.
 *
 * @author Andre Suryana
 */
class MqttConnectionHandler(
    private val context: Context,
    private val logger: Logger = Logger.Default(),
) : MqttHandler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var mqttClient: IMqttAsyncClient
    private lateinit var config: MqttConfig

    private var clientId: String? = null
        get() = field ?: config.getClientId()

    private val pendingRequests = mutableListOf<() -> Unit>()

    private var onMessageArrivalCallback: OnMessageArrival? = null
    private var onDisconnectCallback: OnDisconnect? = null

    override fun connect(config: MqttConfig) {
        if (isConnected()) {
            logger.i(TAG, "MQTT is already connected, disconnecting for configuration changes")
            mqttClient.disconnect()
        }

        logger.i(TAG, "MQTT connecting to '${config.brokerUrl}' with client '$clientId'")
        this.mqttClient = MqttAsyncClient(
            config.brokerUrl,
            clientId,
            createPersistence(),
        )

        val options = MqttConnectionOptions().apply {
            serverURIs = arrayOf(config.brokerUrl)
            isCleanStart = config.cleanStart
            config.sessionExpiryInterval?.let {
                sessionExpiryInterval = it.toLong()
            }
            connectionTimeout = config.connectionTimeout
            keepAliveInterval = config.keepAliveInterval
            isAutomaticReconnect = config.automaticReconnect
            setAutomaticReconnectDelay(
                config.automaticReconnectMinDelay,
                config.automaticReconnectMaxDelay,
            )
            maxReconnectDelay = config.maxReconnectDelay

            config.username?.let { userName = it }
            config.password?.let { password = it.toByteArray() }

            // TODO: Implement SSL/TLS support
        }

        mqttClient.setCallback(object : MqttCallback {
            override fun disconnected(response: MqttDisconnectResponse?) {
                logger.e(TAG, "MQTT disconnected! $response")
                onDisconnectCallback?.invoke(response?.returnCode, response?.reasonString)
            }

            override fun mqttErrorOccurred(me: MqttException?) {
                logger.e(TAG, "MQTT unknown error!", me)
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                logger.d(TAG, "MQTT message arrived: topic=$topic, message=$message")
                onMessageArrivalCallback?.invoke(topic, message.payload)
            }

            override fun deliveryComplete(token: IMqttToken?) {
                if (token == null) {
                    logger.d(TAG, "MQTT delivery complete: token is null")
                    return
                }

                val topics = token.topics?.joinToString() ?: "Unknown"
                val message = token.message?.toString() ?: "No message"
                val isComplete = token.isComplete

                logger.d(TAG, "MQTT delivery complete: topics=[$topics], message=$message, isComplete=$isComplete")
            }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                logger.d(TAG, "MQTT connected: reconnect=$reconnect, serverURI=$serverURI")

                // Execute pending requests
                pendingRequests.forEach { request -> request() }
                pendingRequests.clear()
            }

            override fun authPacketArrived(reasonCode: Int, properties: MqttProperties?) {
                logger.d(TAG, "MQTT auth packet arrived: reasonCode=$reasonCode, properties=$properties")
            }
        })

        mqttClient.connect(options)
    }

    override fun disconnect() {
        try {
            if (isConnected()) {
                mqttClient.disconnect()
            }
            logger.i(TAG, "MQTT disconnected with client '$clientId'")

            // Clear any resources
            clientId = null
            pendingRequests.clear()
        } catch (e: Exception) {
            logger.e(TAG, "MQTT disconnect error", e)
        }
    }

    override fun isConnected(): Boolean {
        return ::mqttClient.isInitialized && mqttClient.isConnected
    }

    override fun subscribe(topic: String, qos: Int) {
        if (!isConnected()) {
            logger.w(TAG, "MQTT client not connected. Request queued.")
            pendingRequests.add { subscribe(topic, qos) }
            return
        }

        scope.launch {
            logger.d(TAG, "subscribe: Subscribe to MQTT topic '$topic' with QoS $qos")
            mqttClient.subscribe(topic, qos)
        }
    }

    override fun unsubscribe(topic: String) {
        if (!isConnected()) {
            logger.w(TAG, "MQTT client not connected. Request queued.")
            pendingRequests.add { unsubscribe(topic) }
            return
        }

        scope.launch {
            logger.d(TAG, "unsubscribe: Unsubscribe from MQTT topic '$topic'")
            mqttClient.unsubscribe(topic)
        }
    }

    override fun publish(topic: String, message: ByteArray, qos: Int, retain: Boolean, ttlInSeconds: Long?) {
        if (!isConnected()) {
            logger.w(TAG, "MQTT client not connected. Request queued.")
            pendingRequests.add { publish(topic, message, qos, retain) }
            return
        }

        scope.launch {
            logger.d(TAG, "publish: Publish to MQTT topic '$topic' with QoS $qos, retain: $retain, message: '${String(message)}'")
            val msg = MqttMessage(message).apply {
                this.qos = qos
                this.isRetained = retain

                // Set message TTL if provided
                this.properties = MqttProperties().apply {
                    messageExpiryInterval = ttlInSeconds
                }
            }
            mqttClient.publish(topic, msg)
        }
    }

    override fun onMessageArrival(callback: OnMessageArrival) {
        this.onMessageArrivalCallback = callback
    }

    override fun onDisconnect(callback: OnDisconnect) {
        this.onDisconnectCallback = callback
    }

    /**
     * Creates and returns the MQTT persistence directory.
     *
     * @return A persistence object for MQTT client session.
     */
    private fun createPersistence(): MqttDefaultFilePersistence {
        val persistenceDir = File(context.cacheDir, PERSISTENCE_DIR).apply {
            if (!exists()) mkdirs() // Create directory if not exists
        }
        return MqttDefaultFilePersistence(persistenceDir.absolutePath)
    }

    companion object {
        private const val TAG = "MqttConnectionHandler"
        private const val PERSISTENCE_DIR = "skyroute"
    }
}
