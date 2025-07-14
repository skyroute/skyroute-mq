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
package com.skyroute.api

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.skyroute.api.util.TopicUtils.extractWildcards
import com.skyroute.core.mqtt.MqttConfig
import com.skyroute.core.mqtt.MqttHandler
import com.skyroute.core.mqtt.OnMessageArrival
import com.skyroute.service.ServiceRegistry
import com.skyroute.service.SkyRouteService
import com.skyroute.service.SkyRouteService.SkyRouteBinder
import kotlin.concurrent.Volatile

/**
 * SkyRoute is the core class that manage MQTT connection and message subscriptions.
 * It acts as an event bus for handling incoming MQTT messages and dispatching them
 * to registered subscribers based on their subscribed topics.
 *
 * @author Andre Suryana
 */
class SkyRoute internal constructor(
    private val builder: SkyRouteBuilder = DEFAULT_BUILDER,
) {

    companion object {
        private const val TAG = "SkyRoute"

        private val DEFAULT_BUILDER = SkyRouteBuilder()

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: SkyRoute? = null

        /**
         * Gets the default instance of [SkyRoute].
         *
         * @return the singleton instance of [SkyRoute].
         */
        fun getDefault(): SkyRoute = instance ?: synchronized(this) {
            instance ?: SkyRoute().also { instance = it }
        }

        /**
         * Creates a new instance of [SkyRouteBuilder].
         */
        fun newBuilder(): SkyRouteBuilder = SkyRouteBuilder()
    }

    private var mqttHandler: MqttHandler? = null
    private var bound = false

    private val logger = builder.logger

    private val pendingRegistrations = mutableListOf<Any>()
    private val subscriberManager by lazy {
        SubscriberManager(
            logger = logger,
            payloadAdapter = builder.payloadAdapter,
            executorService = builder.executorService,
            topicDelegate = object : TopicSubscriptionDelegate {
                override fun subscribe(topic: String, qos: Int) {
                    mqttHandler?.subscribe(topic, qos)
                }

                override fun unsubscribe(topic: String) {
                    mqttHandler?.unsubscribe(topic)
                }
            },
        )
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            logger.i(TAG, "SkyRoute service connected!")

            // Retrieve the handler via binder
            if (binder == null || binder !is SkyRouteBinder) {
                throw IllegalStateException("MQTT handler not retrieved properly")
            }

            mqttHandler = binder.getMqttHandler()
            bound = true

            // Register message arrival
            mqttHandler?.onMessageArrival(onMessageArrivalHandler)
            mqttHandler?.onDisconnect { code, reason ->
                logger.w(TAG, "SkyRoute disconnected! Code: $code, Reason: $reason")
                builder.onDisconnect?.invoke(code, reason)
            }

            synchronized(pendingRegistrations) {
                pendingRegistrations.forEach { subscriberManager.registerSubscriber(it) }
                pendingRegistrations.clear()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            logger.w(TAG, "SkyRoute service disconnected!")
            mqttHandler = null
            bound = false
        }
    }

    private val onMessageArrivalHandler: OnMessageArrival = { topic, message ->
        subscriberManager.forEachMatchingSubscriptions(topic) { subscription ->
            try {
                subscriberManager.invoke(
                    subscription,
                    message,
                    extractWildcards(subscription.subscriberMethod.topic, topic),
                )
            } catch (e: Exception) {
                if (builder.throwsInvocationException) throw e
            }
        }
    }

    /**
     * Initializes [SkyRoute] and binds it to the [SkyRouteService].
     *
     * @param context The application context to bind the service.
     */
    fun init(context: Context) {
        logger.i(TAG, "SkyRoute init...")
        ServiceRegistry.initLogger(builder.logger)

        context.applicationContext.run { // Using application context to avoid memory leaks
            val intent = Intent(this, SkyRouteService::class.java)
            intent.putExtra(SkyRouteService.EXTRA_CONFIG, builder.config)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    /**
     * Applies a new [MqttConfig] and reconnects the MQTT client.
     *
     * This will disconnect the current session (if any) and reconnect with the new configuration.
     *
     * @param config The new MQTT configuration to apply.
     */
    fun applyConfig(config: MqttConfig) {
        if (!bound || mqttHandler == null) {
            logger.w(TAG, "applyConfig: Service not yet bound! Cannot update config.")
            return
        }

        logger.i(TAG, "applyConfig: Reconfiguring MQTT with new config...")

        if (mqttHandler == null) {
            logger.w(TAG, "applyConfig: MQTT handler is null! Cannot update config.")
            return
        }

        if (pendingRegistrations.isNotEmpty()) {
            logger.w(TAG, "applyConfig: Pending registrations will be lost!")
            pendingRegistrations.clear()
        }

        mqttHandler?.reconnect(config)
    }

    /**
     * Registers a subscriber to receive messages for topics it is subscribed to.
     *
     * @param subscriber The subscriber object that has methods annotated with [Subscribe].
     */
    fun register(subscriber: Any) {
        if (bound && mqttHandler?.isConnected() == true) {
            subscriberManager.registerSubscriber(subscriber)
        } else {
            logger.i(TAG, "Service not yet bound. Queuing subscriber: ${subscriber::class.java.name}")
            synchronized(pendingRegistrations) {
                pendingRegistrations.add(subscriber)
            }
        }
    }

    /**
     * Checks if a subscriber is already registered.
     *
     * @param subscriber The subscriber to check.
     * @return `true` if the subscriber is registered, `false` otherwise.
     */
    fun isRegistered(subscriber: Any): Boolean {
        return subscriberManager.isRegistered(subscriber)
    }

    /**
     * Unregisters a subscriber from receiving messages.
     *
     * @param subscriber The subscriber to unregister.
     */
    fun unregister(subscriber: Any) {
        if (!isRegistered(subscriber)) {
            logger.w(TAG, "Subscriber $subscriber is not registered.")
            return
        }

        subscriberManager.unregisterSubscriber(subscriber)
    }

    /**
     * Publishes a message to the specified topic.
     *
     * @param topic The topic to publish the message to.
     * @param message The message to be published.
     * @throws IllegalArgumentException if value of QoS is not 0, 1, or 2.
     */
    @Throws(IllegalArgumentException::class)
    fun publish(topic: String, message: Any) {
        publish(topic, message, 0, false)
    }

    /**
     * Publishes a message with a specified Quality of Service (QoS) level.
     *
     * @param topic The topic to publish the message to.
     * @param message The message to be published.
     * @param qos The Quality of Service level for the message.
     * @throws IllegalArgumentException if value of QoS is not 0, 1, or 2.
     */
    @Throws(IllegalArgumentException::class)
    fun publish(topic: String, message: Any, qos: Int) {
        publish(topic, message, qos, false)
    }

    /**
     * Publishes a message with a specified Quality of Service (QoS) level and retain flag.
     *
     * @param topic The topic to publish the message to.
     * @param message The message to be published.
     * @param qos The Quality of Service level for the message.
     * @param retain Whether the message should be retained after delivery.
     * @throws IllegalArgumentException if value of QoS is not 0, 1, or 2.
     */
    @Throws(IllegalArgumentException::class)
    fun publish(topic: String, message: Any, qos: Int, retain: Boolean) {
        publish(topic, message, qos, retain, null)
    }

    /**
     * Publishes a message with a specified Quality of Service (QoS) level, retain flag, and TTL.
     *
     * @param topic The topic to publish the message to.
     * @param message The message to be published.
     * @param qos The Quality of Service level for the message.
     * @param retain Whether the message should be retained after delivery.
     * @param ttl The Time To Live (TTL) for the message in seconds.
     * @throws IllegalArgumentException if value of QoS is not 0, 1, or 2.
     */
    @Throws(IllegalArgumentException::class)
    fun publish(topic: String, message: Any, qos: Int, retain: Boolean, ttl: Long? = null) {
        if (!bound) {
            logger.w(TAG, "publish: Service not yet bound! Message will be queued")
            return
        }
        if (qos < 0 || qos > 2) throw IllegalArgumentException("QoS must be between 0 and 2")

        val encoded = builder.payloadAdapter.encode(message, message::class.java)
        mqttHandler?.publish(topic, encoded, qos, retain, ttl)
    }
}
