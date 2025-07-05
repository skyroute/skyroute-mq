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
import android.os.Binder
import android.os.IBinder
import com.skyroute.core.mqtt.MqttConfig
import com.skyroute.core.mqtt.MqttHandler
import com.skyroute.core.util.Logger
import com.skyroute.service.mqtt.MqttConnectionHandler
import com.skyroute.service.mqtt.socket.DefaultMqttSocketFactory
import com.skyroute.service.util.MetadataUtils
import com.skyroute.service.util.MetadataUtils.toMqttConfig

/**
 * [SkyRouteService] is a service that manages MQTT client connections and handles topic-based messaging.
 * It allows other components to subscribe to topics, publish messages, and register callbacks for
 * incoming messages.
 *
 * @author Andre Suryana
 */
class SkyRouteService : Service() {

    private val binder = SkyRouteBinder()
    private val logger = Logger.Default()

    private lateinit var config: MqttConfig
    private lateinit var mqttHandler: MqttHandler

    init {
        MetadataUtils.setLogger(logger)
    }

    /**
     * Initializes the MQTT connection using configuration parameters from the service metadata.
     */
    override fun onCreate() {
        super.onCreate()
        logger.i(TAG, "SkyRouteService is created")

        val metaData = packageManager.getServiceInfo(
            ComponentName(this, SkyRouteService::class.java),
            PackageManager.GET_META_DATA,
        ).metaData
        config = metaData.toMqttConfig(applicationContext)

        mqttHandler = MqttConnectionHandler(
            context = this,
            logger = logger,
            socketFactory = DefaultMqttSocketFactory(),
        )
        mqttHandler.connect(config)
    }

    /**
     * Stops the MQTT client and cleans up when the service is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        mqttHandler.disconnect()

        logger.w(TAG, "SkyRouteService destroyed")
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
        logger.d(TAG, "Starting SkyRouteService! startId=$startId")
        return START_STICKY
    }

    /**
     * Binds the service to a client and return the binder for accessing the service's methods.
     *
     * @param intent The intent used to bind the service.
     * @return A [SkyRouteBinder] instance that allows the client to interact with the service.
     */
    override fun onBind(intent: Intent?): IBinder = binder

    inner class SkyRouteBinder : Binder() {

        /**
         * Provides access to the underlying MQTT operations through the [MqttHandler] interface.
         *
         * @return The [MqttHandler] instance associated with the service.
         */
        fun getMqttHandler(): MqttHandler = mqttHandler
    }

    companion object {
        private const val TAG = "SkyRouteService"
    }
}
