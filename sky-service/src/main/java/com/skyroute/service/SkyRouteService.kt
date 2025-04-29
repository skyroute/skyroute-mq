package com.skyroute.service

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.skyroute.service.config.MqttConfig
import com.skyroute.service.util.MetadataUtils.toMqttConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import java.io.File
import kotlin.math.pow

/**
 * [SkyRouteService] is a service that manages MQTT client connections and handles topic-based messaging.
 * It allows other components to subscribe to topics, publish messages, and register callbacks for
 * incoming messages.
 *
 * @author Andre Suryana
 */
class SkyRouteService : Service(), TopicMessenger, MqttController {

    private val binder = SkyRouteBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingRequests = mutableListOf<() -> Unit>()

    private lateinit var mqttClient: MqttClient
    private lateinit var config: MqttConfig
    private var onMessageArrivalCallback: MessageArrival? = null

    private var retryCount = 0
    private var isMqttConnected = false
        set(value) {
            if (value) retryCount = 0 // Reset the retry count
            field = value
        }

    /**
     * Initializes the MQTT connection using configuration parameters from the service metadata.
     */
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "SkyRouteService is created")

        val metaData = packageManager.getServiceInfo(
            ComponentName(this, SkyRouteService::class.java),
            PackageManager.GET_META_DATA
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
                val clientId = config.generateClientId()

                mqttClient = MqttClient(config.brokerUrl, clientId, createPersistence())
                Log.i(TAG, "MQTT init... url=${config.brokerUrl}, client=$clientId")

                val options = MqttConnectOptions().apply {
                    isCleanSession = config.cleanSession
                    connectionTimeout = config.connectionTimeout
                    keepAliveInterval = config.keepAliveInterval
                    maxInflight = config.maxInFlight
                    isAutomaticReconnect = config.automaticReconnect

                    config.username?.let { userName = it }
                    config.password?.let { password = it.toCharArray() }
                }

                mqttClient.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        if (cause is MqttException) {
                            handleMqttException(cause)
                        } else {
                            Log.e(TAG, "MQTT connection lost, unknown error!", cause)
                        }
                    }

                    override fun messageArrived(topic: String, message: MqttMessage) {
                        Log.d(TAG, "MQTT message arrived: topic=$topic, message=$message")
                        onMessageArrivalCallback?.invoke(topic, message.toString())
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        Log.d(TAG, "MQTT delivery complete: token=$token")
                    }
                })

                // Set waiting timeout to prevent blocking the main thread
                mqttClient.timeToWait = config.connectionTimeout * 1000L

                mqttClient.connect(options)
                isMqttConnected = true
                Log.i(TAG, "MQTT connected")

                executePendingRequests()
            } catch (e: MqttException) {
                handleMqttException(e)
                isMqttConnected = false
            } catch (e: Exception) {
                Log.e(TAG, "MQTT init unknown error", e)
                isMqttConnected = false
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

    private fun handleMqttException(e: MqttException) {
        Log.w(TAG, "MQTT client exception, $e")
        when (e.reasonCode.toShort()) {
            MqttException.REASON_CODE_CLIENT_TIMEOUT,
            MqttException.REASON_CODE_CONNECTION_LOST,
                -> {
                if (config.automaticReconnect) {
                    // Exponential backoff with maximum delay
                    val delayTime = minOf(10_000L * 1.5.pow(retryCount).toLong(), MAX_RETRY_DELAY)

                    serviceScope.launch {
                        delay(delayTime)
                        initMqtt()
                        retryCount++
                    }
                }
            }

            else -> Log.e(TAG, "MQTT init error!", e)
        }
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

    override fun publish(topic: String, message: Any, qos: Int, retain: Boolean) {
        if (!isConnected()) {
            Log.w(TAG, "MQTT client not connected. Request queued.")
            pendingRequests.add { publish(topic, message, qos, retain) }
            return
        }

        serviceScope.launch {
            Log.d(TAG, "publish: Publish to MQTT topic '$topic' with QoS $qos, retain: $retain, message: '$message'")
            val msg = MqttMessage(message.toString().toByteArray()).apply {
                this.qos = qos
                this.isRetained = retain
            }
            mqttClient.publish(topic, msg)
        }
    }

    override fun onMessageArrival(callback: MessageArrival) {
        this.onMessageArrivalCallback = callback
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
        return ::mqttClient.isInitialized && mqttClient.isConnected && isMqttConnected
    }

    /**
     * A [Binder] subclass that allows clients to bind to the [SkyRouteService] and interact with its methods.
     */
    inner class SkyRouteBinder : Binder() {

        /**
         * Retrieves the [TopicMessenger] instance for interacting with the service.
         *
         * @return The [TopicMessenger] instance associated with the service.
         */
        fun getTopicMessenger(): TopicMessenger = this@SkyRouteService

        fun getMqttController(): MqttController = this@SkyRouteService
    }

    companion object {
        private const val TAG = "SkyRouteService"

        /** Maximum retry delay in milliseconds (5 minutes) */
        private const val MAX_RETRY_DELAY = 300_000L
    }
}