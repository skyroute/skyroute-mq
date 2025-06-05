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
import android.util.Log
import com.skyroute.api.adapter.DefaultPayloadAdapter
import com.skyroute.api.util.TopicUtils.extractWildcards
import com.skyroute.api.util.TopicUtils.matchesTopic
import com.skyroute.core.adapter.PayloadAdapter
import com.skyroute.core.message.OnDisconnect
import com.skyroute.core.message.OnMessageArrival
import com.skyroute.core.message.TopicMessenger
import com.skyroute.service.MqttController
import com.skyroute.service.SkyRouteBinder
import com.skyroute.service.SkyRouteService
import com.skyroute.service.config.MqttConfig
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
    }

    private val subscriptionsByTopic: MutableMap<String, CopyOnWriteArrayList<Subscription>> = ConcurrentHashMap()
    private val typesBySubscriber: MutableMap<Any, MutableList<String>> = ConcurrentHashMap()
    private val pendingRegistrations = mutableListOf<Any>()

    private val adapterCache: MutableMap<KClass<out PayloadAdapter>, PayloadAdapter> = ConcurrentHashMap()

    private var topicMessenger: TopicMessenger? = null
    private var mqttController: MqttController? = null
    private var config: MqttConfig? = null
    private var bound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.i(TAG, "SkyRoute service connected!")
            (binder as? SkyRouteBinder)?.let { skyRouteBinder ->
                // Initialize interface
                topicMessenger = skyRouteBinder.getTopicMessenger()
                mqttController = skyRouteBinder.getMqttController()
                bound = true

                // Load the config from builder, this will ignore the defined config in manifest
                config?.let {
                    Log.w(TAG, "Custom SkyRoute builder config found, config in 'AndroidManifest.xml' will be replaced")
                    mqttController?.connect(it)
                }

                // Register message arrival
                topicMessenger?.onMessageArrival(onMessageArrivalHandler)

                synchronized(pendingRegistrations) {
                    pendingRegistrations.forEach { internalRegister(it) }
                    pendingRegistrations.clear()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "SkyRoute service disconnected!")
            topicMessenger = null
            bound = false
        }
    }

    private val onMessageArrivalHandler: OnMessageArrival = { topic, message ->
        // Find all subscribers that have subscribed to this topic
        subscriptionsByTopic.filterKeys { it.matchesTopic(topic) }
            .values
            .flatten()
            .forEach { subscription ->
                invokeMethod(
                    subscription,
                    message,
                    extractWildcards(subscription.subscriberMethod.topic, topic),
                )
            }
    }

    /**
     * Initializes [SkyRoute] and binds it to the [SkyRouteService].
     *
     * @param context The application context to bind the service.
     */
    fun init(context: Context, config: MqttConfig? = null) {
        Log.i(TAG, "SkyRoute init...")
        this.config = config

        context.applicationContext.run { // Using application context to avoid memory leaks
            val intent = Intent(this, SkyRouteService::class.java)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    /**
     * Registers a subscriber to receive messages for topics it is subscribed to.
     *
     * @param subscriber The subscriber object that has methods annotated with [Subscribe].
     */
    fun register(subscriber: Any) {
        if (bound && mqttController?.isConnected() == true) {
            internalRegister(subscriber)
        } else {
            Log.i(TAG, "Service not yet bound. Queuing subscriber: ${subscriber::class.java.name}")
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
        Log.d(TAG, "internalRegister: Registering ${methods.size} methods for subscriber: ${subscriberClass.name}")

        if (methods.isEmpty()) {
            Log.w(
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
            topicMessenger?.subscribe(topic, qos)

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
     */
    private fun invokeMethod(subscription: Subscription, message: ByteArray, wildcards: List<String>? = null) {
        // Check if the subscription is still active
        if (!subscription.active) return

        val method = subscription.subscriberMethod.method
        val threadMode = subscription.subscriberMethod.threadMode
        val subscriber = subscription.subscriber

        // Use the globally configured PayloadAdapter by default
        var adapter = builder.payloadAdapter

        // If a custom adapter is defined, try to retrieve it from the cache or instantiate it
        subscription.subscriberMethod.adapterClass.let { adapterClass ->
            if (adapterClass != DefaultPayloadAdapter::class) {
                adapter = adapterCache.getOrPut(adapterClass) {
                    adapterClass.objectInstance ?: adapterClass.java.getDeclaredConstructor().newInstance()
                }
            }
        }

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
                Log.e(TAG, "Method invocation failed", e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during method invocation", e)
            }
            // TODO: Should determine to handle exception or not,
            //  we can rethrow it if we want (maybe configured by flag in builder)
            //  or just log it and continue.
        }

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
                    builder.executorService.execute { invoke() }
                } else {
                    invoke()
                }
            }

            ThreadMode.ASYNC -> {
                builder.executorService.execute { invoke() }
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
            Log.w(TAG, "Subscriber $subscriber is not registered.")
            return
        }

        val topics = typesBySubscriber[subscriber]
        topics?.forEach { topic ->
            val subscriptions = subscriptionsByTopic[topic]
            subscriptions?.forEach { subscription ->
                if (subscription.subscriber == subscriber) {
                    subscription.active = false
                    topicMessenger?.unsubscribe(topic)
                    Log.d(TAG, "Marked subscription as inactive: ${subscription.subscriberMethod.description}")
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
    fun publish(topic: String, message: Any, qos: Int, retain: Boolean) {
        publish(topic, message, qos, retain, null)
    }

    /**
     * Publishes a message with a specified Quality of Service (QoS) level and retain flag.
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
            Log.w(TAG, "publish: Service not yet bound! Message will be queued")
            return
        }
        if (qos < 0 || qos > 2) throw IllegalArgumentException("QoS must be between 0 and 2")

        val encoded = builder.payloadAdapter.encode(message, message::class.java)
        topicMessenger?.publish(topic, encoded, qos, retain, ttl)
    }

    // FIXME: This implementation overrides any previously set callback, which makes the disconnect listener global.
    //        This could lead to unintended side effects if multiple components attempt to register their own callbacks.
    //        Consider alternative approaches:
    //        - Maintain a list of callbacks and invoke all of them on disconnect.
    //        - Allow each component to register its own listener with isolation.
    //        - Introduce a dispatcher or observer pattern to support multiple listeners.
    fun setOnDisconnectCallback(callback: OnDisconnect) {
        topicMessenger?.onDisconnect(callback)
    }
}
