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
package com.skyroute.service

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.skyroute.core.message.OnDisconnect
import com.skyroute.core.message.OnMessageArrival
import com.skyroute.core.message.TopicMessenger
import com.skyroute.service.config.MqttConfig
import com.skyroute.service.util.MetadataUtils.toMqttConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
 * [SkyRouteService] is a service that manages MQTT client connections and handles topic-based messaging.
 * It allows other components to subscribe to topics, publish messages, and register callbacks for
 * incoming messages.
 *
 * @author Andre Suryana
 */
class SkyRouteService : Service(), TopicMessenger, MqttController {

    private val binder = SkyRouteBinder(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingRequests = mutableListOf<() -> Unit>()

    private lateinit var mqttClient: IMqttAsyncClient
    private lateinit var config: MqttConfig
    private var onMessageArrivalCallback: OnMessageArrival? = null
    private var onDisconnectCallback: OnDisconnect? = null

    /**
     * Initializes the MQTT connection using configuration parameters from the service metadata.
     */
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "SkyRouteService is created")

        val metaData = packageManager.getServiceInfo(
            ComponentName(this, SkyRouteService::class.java),
            PackageManager.GET_META_DATA,
        ).metaData
        config = metaData.toMqttConfig()

        initMqtt()
    }

    /**
     * Initialize the MQTT client with the given configuration.
     */
    private fun initMqtt() {
        serviceScope.launch {
            try {
                val clientId = config.getClientId()

                Log.i(TAG, "MQTT init... url=${config.brokerUrl}, client=$clientId")
                mqttClient = MqttAsyncClient(
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
                        Log.e(TAG, "MQTT disconnected! $response")
                        onDisconnectCallback?.invoke(response?.returnCode, response?.reasonString)
                    }

                    override fun mqttErrorOccurred(exception: MqttException?) {
                        Log.e(TAG, "MQTT unknown error!", exception)
                    }

                    override fun messageArrived(topic: String, message: MqttMessage) {
                        Log.d(TAG, "MQTT message arrived: topic=$topic, message=$message")
                        onMessageArrivalCallback?.invoke(topic, message.payload)
                    }

                    override fun deliveryComplete(token: IMqttToken?) {
                        if (token == null) {
                            Log.d(TAG, "MQTT delivery complete: token is null")
                            return
                        }

                        val topics = token.topics?.joinToString() ?: "Unknown"
                        val message = token.message?.toString() ?: "No message"
                        val isComplete = token.isComplete

                        Log.d(TAG, "MQTT delivery complete: topics=[$topics], message=$message, isComplete=$isComplete")
                    }

                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        Log.d(TAG, "MQTT connected: reconnect=$reconnect, serverURI=$serverURI")
                        executePendingRequests()
                    }

                    override fun authPacketArrived(reasonCode: Int, properties: MqttProperties?) {
                        Log.d(TAG, "MQTT auth packet arrived: reasonCode=$reasonCode, properties=$properties")
                    }
                })

                mqttClient.connect(options)
            } catch (e: Exception) {
                Log.e(TAG, "MQTT init unknown error", e)
            }
        }
    }

    /**
     * Creates and returns the MQTT persistence directory.
     *
     * @return A persistence object for MQTT client session.
     */
    private fun createPersistence(): MqttDefaultFilePersistence {
        val persistenceDir = File(cacheDir, "skyroute")
        if (!persistenceDir.exists()) persistenceDir.mkdirs()

        return MqttDefaultFilePersistence(persistenceDir.absolutePath)
    }

    private fun executePendingRequests() {
        pendingRequests.forEach { request ->
            request() // Execute the queued action
        }
        pendingRequests.clear() // Clear the queue after execution
    }

    /**
     * Stops the MQTT client and cleans up when the service is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Cancel ongoing coroutines

        try {
            if (isConnected()) mqttClient.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "MQTT disconnect error", e)
        }
        Log.w(TAG, "SkyRouteService destroyed")
    }

    /**
     * Called when the service is started, returning a sticky service status.
     *
     * @param intent The start command intent.
     * @param flags Additional flags.
     * @param startId A unique start ID for the service.
     * @return The start mode for the service.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Starting SkyRouteService! startId=$startId")
        return START_STICKY
    }

    /**
     * Binds the service to a client and return the binder for accessing the service's methods.
     *
     * @param intent The intent used to bind the service.
     * @return A [SkyRouteBinder] instance that allows the client to interact with the service.
     */
    override fun onBind(intent: Intent?): IBinder = binder

    override fun subscribe(topic: String, qos: Int) {
        if (!isConnected()) {
            Log.w(TAG, "MQTT client not connected. Request queued.")
            pendingRequests.add { subscribe(topic, qos) }
            return
        }

        serviceScope.launch {
            Log.d(TAG, "subscribe: Subscribe to MQTT topic '$topic' with QoS $qos")
            mqttClient.subscribe(topic, qos)
        }
    }

    override fun unsubscribe(topic: String) {
        if (!isConnected()) {
            Log.w(TAG, "MQTT client not connected. Request queued.")
            pendingRequests.add { unsubscribe(topic) }
            return
        }

        serviceScope.launch {
            Log.d(TAG, "unsubscribe: Unsubscribe from MQTT topic '$topic'")
            mqttClient.unsubscribe(topic)
        }
    }

    override fun publish(topic: String, message: ByteArray, qos: Int, retain: Boolean, ttlInSeconds: Long?) {
        if (!isConnected()) {
            Log.w(TAG, "MQTT client not connected. Request queued.")
            pendingRequests.add { publish(topic, message, qos, retain) }
            return
        }

        serviceScope.launch {
            Log.d(TAG, "publish: Publish to MQTT topic '$topic' with QoS $qos, retain: $retain, message: '${String(message)}'")
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

    override fun connect(config: MqttConfig) {
        try {
            // Force clear the previous pending request
            pendingRequests.clear()

            if (isConnected()) {
                Log.i(TAG, "MQTT is already connected, disconnecting for reconfiguration")
                mqttClient.disconnect()
            }

            this.config = config
            initMqtt()
        } catch (e: Exception) {
            Log.e(TAG, "MQTT connect error", e)
        }
    }

    override fun disconnect() {
        try {
            if (isConnected()) {
                pendingRequests.clear()
                mqttClient.disconnect()
                Log.i(TAG, "MQTT disconnected via controller")
            }
        } catch (e: Exception) {
            Log.e(TAG, "MQTT disconnect error", e)
        }
    }

    override fun isConnected(): Boolean {
        return ::mqttClient.isInitialized && mqttClient.isConnected
    }

    companion object {
        private const val TAG = "SkyRouteService"
    }
}
