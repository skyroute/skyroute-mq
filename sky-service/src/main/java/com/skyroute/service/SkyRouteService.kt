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
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import java.io.File

/**
 * [SkyRouteService] is a service that manages MQTT client connections and handles topic-based messaging.
 * It allows other components to subscribe to topics, publish messages, and register callbacks for
 * incoming messages.
 *
 * @author Andre Suryana
 */
class SkyRouteService : Service(), TopicMessenger, MqttController {

    private val binder = SkyRouteBinder()
    private lateinit var mqttClient: MqttClient

    private var onMessageArrivalCallback: MessageArrival? = null

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

        initMqtt(metaData.toMqttConfig())
    }

    /**
     * Initialize the MQTT client with the given configuration.
     */
    private fun initMqtt(config: MqttConfig) {
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
                    Log.e(TAG, "MQTT connection lost", cause)
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    Log.d(TAG, "MQTT message arrived: topic=$topic, message=$message")
                    onMessageArrivalCallback?.invoke(topic, message.toString())
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "MQTT delivery complete: token=$token")
                }
            })

            mqttClient.connect(options)
            Log.i(TAG, "MQTT connected")
        } catch (e: Exception) {
            Log.e(TAG, "MQTT init error", e)
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

    /**
     * Stops the MQTT client and cleans up when the service is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        try {
            if (mqttClient.isConnected) mqttClient.disconnect()
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
        Log.d(TAG, "subscribe: Subscribe to MQTT topic '$topic' with QoS $qos")
        if (isConnected()) {
            mqttClient.subscribe(topic, qos)
        }
    }

    override fun unsubscribe(topic: String) {
        Log.d(TAG, "unsubscribe: Unsubscribe from MQTT topic '$topic'")
        if (isConnected()) {
            mqttClient.unsubscribe(topic)
        }
    }

    override fun publish(topic: String, message: Any, qos: Int, retain: Boolean) {
        Log.d(TAG, "publish: Publish to MQTT topic '$topic' with QoS $qos, retain: $retain, message: '$message'")
        if (isConnected()) {
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
            if (isConnected()) {
                Log.i(TAG, "MQTT is already connected, disconnecting for reconfiguration")
                mqttClient.disconnect()
            }
            initMqtt(config)
        } catch (e: Exception) {
            Log.e(TAG, "MQTT connect error", e)
        }
    }

    override fun disconnect() {
        try {
            if (isConnected()) {
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
    }
}