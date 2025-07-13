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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.skyroute.api.adapter.DefaultPayloadAdapter
import com.skyroute.api.util.TopicUtils.extractWildcards
import com.skyroute.api.util.TopicUtils.matchesTopic
import com.skyroute.core.adapter.PayloadAdapter
import com.skyroute.core.mqtt.MqttHandler
import com.skyroute.core.mqtt.OnMessageArrival
import com.skyroute.service.ServiceRegistry
import com.skyroute.service.SkyRouteService
import com.skyroute.service.SkyRouteService.SkyRouteBinder
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.Volatile
import kotlin.reflect.KClass

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

    private val subscriptionsByTopic: MutableMap<String, CopyOnWriteArrayList<Subscription>> = ConcurrentHashMap()
    private val typesBySubscriber: MutableMap<Any, MutableList<String>> = ConcurrentHashMap()
    private val pendingRegistrations = mutableListOf<Any>()

    private val adapterCache: MutableMap<KClass<out PayloadAdapter>, PayloadAdapter> = ConcurrentHashMap()

    private var mqttHandler: MqttHandler? = null
    private var bound = false

    private val executorService = builder.executorService
    private val logger = builder.logger
    private val payloadAdapter = builder.payloadAdapter

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
                pendingRegistrations.forEach { internalRegister(it) }
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
        // Find all subscribers that have subscribed to this topic
        subscriptionsByTopic.filterKeys { it.matchesTopic(topic) }
            .values
            .flatten()
            .forEach { subscription ->
                try {
                    invokeMethod(
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
     * Registers a subscriber to receive messages for topics it is subscribed to.
     *
     * @param subscriber The subscriber object that has methods annotated with [Subscribe].
     */
    fun register(subscriber: Any) {
        if (bound && mqttHandler?.isConnected() == true) {
            internalRegister(subscriber)
        } else {
            logger.i(TAG, "Service not yet bound. Queuing subscriber: ${subscriber::class.java.name}")
            synchronized(pendingRegistrations) {
                pendingRegistrations.add(subscriber)
            }
        }
    }

    /**
     * Internal method to register a subscriber.
     *
     * @param subscriber The subscriber object that has methods annotated with [Subscribe].
     */
    private fun internalRegister(subscriber: Any) {
        val subscriberClass = subscriber::class.java
        val methods = subscriberClass.declaredMethods.filter { it.getAnnotation(Subscribe::class.java) != null }
        logger.d(TAG, "internalRegister: Registering ${methods.size} methods for subscriber: ${subscriberClass.name}")

        if (methods.isEmpty()) {
            logger.w(
                TAG,
                "No methods with @Subscribe annotation were found in class ${subscriberClass.name}. " +
                    "Ensure you have at least one method annotated with @Subscribe(topic = ...) " +
                    "and the method is not private or static.",
            )
            return
        }

        for (method in methods) {
            val subscribeAnnotation = method.getAnnotation(Subscribe::class.java)
                ?: throw IllegalArgumentException("Method ${method.name} in class ${subscriberClass.name} must be annotated with @Subscribe.")

            val topic = subscribeAnnotation.topic
            val qos = subscribeAnnotation.qos
            val threadMode = subscribeAnnotation.threadMode
            val adapterClass = subscribeAnnotation.adapter

            // Wrap the method as a lambda
            val subscriberMethod = SubscriberMethod(
                method = method,
                description = "${subscriberClass.name}#${method.name}",
                threadMode = threadMode,
                topic = topic,
                qos = qos,
                adapterClass = adapterClass,
            )

            val subscription = Subscription(subscriber, subscriberMethod)

            // Subscribe to the topic
            mqttHandler?.subscribe(topic, qos)

            // Register into the maps
            subscriptionsByTopic.getOrPut(topic) { CopyOnWriteArrayList() }.add(subscription)
            typesBySubscriber.getOrPut(subscriber) { mutableListOf() }.add(topic)
        }
    }

    /**
     * Invokes the subscriber's method with the given message and thread mode.
     *
     * @param subscription The subscription that holds the subscriber method.
     * @param message The message to pass to the subscriber's method.
     * @param wildcards Any wildcards extracted from the topic.
     * @throws Exception if an error occurs during method invocation.
     */
    private fun invokeMethod(subscription: Subscription, message: ByteArray, wildcards: List<String>? = null) {
        // Check if the subscription is still active
        if (!subscription.active) return

        val method = subscription.subscriberMethod.method
        val threadMode = subscription.subscriberMethod.threadMode
        val subscriber = subscription.subscriber

        // Use the globally configured PayloadAdapter by default
        var adapter = payloadAdapter

        // If a custom adapter is defined, try to retrieve it from the cache or instantiate it
        // FIXME: Introduce into separate method
        subscription.subscriberMethod.adapterClass.let { adapterClass ->
            if (adapterClass != DefaultPayloadAdapter::class) {
                adapter = adapterCache.getOrPut(adapterClass) {
                    adapterClass.objectInstance ?: adapterClass.java.getDeclaredConstructor().newInstance()
                }
            }
        }

        // FIXME: Introduce into separate method
        val invoke = lambda@{
            try {
                val params = method.parameterTypes
                if (params.isEmpty() || params.size > 2) {
                    throw IllegalArgumentException(
                        "Method ${method.name} in class ${subscriber::class.java.name} must have 1 or 2 parameters",
                    )
                }

                val decoded = adapter.decode(message, params[0])

                val args = when (params.size) {
                    1 -> arrayOf(decoded)
                    2 -> arrayOf(decoded, wildcards ?: emptyList<String>())
                    else -> throw IllegalArgumentException(
                        "Method ${method.name} in class ${subscriber::class.java.name} must have 1 or 2 parameters",
                    )
                }

                method.invoke(subscriber, *args)
            } catch (e: InvocationTargetException) {
                logger.e(TAG, "Method invocation failed", e)
                throw e
            } catch (e: Exception) {
                logger.e(TAG, "Unexpected error during method invocation", e)
                throw e
            }
        }

        // FIXME: Introduce into separate method
        when (threadMode) {
            ThreadMode.MAIN -> {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    invoke()
                } else {
                    Handler(Looper.getMainLooper()).post { invoke() }
                }
            }

            ThreadMode.BACKGROUND -> {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    executorService.execute { invoke() }
                } else {
                    invoke()
                }
            }

            ThreadMode.ASYNC -> {
                executorService.execute { invoke() }
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
        return typesBySubscriber.containsKey(subscriber)
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

        val topics = typesBySubscriber[subscriber]
        topics?.forEach { topic ->
            val subscriptions = subscriptionsByTopic[topic]
            subscriptions?.forEach { subscription ->
                if (subscription.subscriber == subscriber) {
                    subscription.active = false
                    mqttHandler?.unsubscribe(topic)
                    logger.d(TAG, "Marked subscription as inactive: ${subscription.subscriberMethod.description}")
                }
            }
        }

        typesBySubscriber.remove(subscriber)
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
