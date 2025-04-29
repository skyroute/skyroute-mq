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
import com.google.gson.Gson
import com.skyroute.api.util.TopicUtils.extractWildcards
import com.skyroute.api.util.TopicUtils.matchesTopic
import com.skyroute.service.MqttController
import com.skyroute.service.SkyRouteService
import com.skyroute.service.TopicMessenger
import com.skyroute.service.config.MqttConfig
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.Volatile

/**
 * SkyRoute is the core class that manage MQTT connection and message subscriptions.
 * It acts as an event bus for handling incoming MQTT messages and dispatching them
 * to registered subscribers based on their subscribed topics.
 *
 * @author Andre Suryana
 */
class SkyRoute private constructor() {

    companion object {
        private const val TAG = "SkyRoute"

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

    private var executorService = Executors.newCachedThreadPool()

    private var topicMessenger: TopicMessenger? = null
    private var mqttController: MqttController? = null
    private var config: MqttConfig? = null
    private var bound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.i(TAG, "SkyRoute service connected!")
            (binder as? SkyRouteService.SkyRouteBinder)?.let { skyRouteBinder ->
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
                topicMessenger?.onMessageArrival { topic, message ->
                    val subscriptions = subscriptionsByTopic
                        .filterKeys { it.matchesTopic(topic) }
                        .values
                        .flatten()

                    subscriptions.forEach { subscription ->
                        invokeMethod(subscription, message, extractWildcards(subscription.subscriberMethod.topic, topic))
                    }
                }

                synchronized(pendingRegistrations) {
                    for (subscriber in pendingRegistrations) {
                        internalRegister(subscriber)
                    }
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
                TAG, "No methods with @Subscribe annotation were found in class ${subscriberClass.name}. " +
                        "Ensure you have at least one method annotated with @Subscribe(topic = ...) " +
                        "and the method is not private or static."
            )
            return
        }

        for (method in methods) {
            val subscribeAnnotation = method.getAnnotation(Subscribe::class.java)
                ?: throw IllegalArgumentException("Method ${method.name} in class ${subscriberClass.name} must be annotated with @Subscribe.")

            val topic = subscribeAnnotation.topic
            val qos = subscribeAnnotation.qos
            val threadMode = subscribeAnnotation.threadMode

            // Wrap the method as a lambda
            val subscriberMethod = SubscriberMethod(
                method = method,
                description = "${subscriberClass.name}#${method.name}",
                threadMode = threadMode,
                topic = topic,
                qos = qos
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
     * Converts the received message to the expected type.
     *
     * @param message The message to be converted.
     * @param expectedType The type that the message should be converted to.
     * @return The converted message or null if deserialization fails.
     */
    private fun convertMessage(message: Any, expectedType: Class<*>): Any? {
        return try {
            when (expectedType) {
                String::class.java -> message as String
                Int::class.java, java.lang.Integer::class.java -> (message as String).toInt()
                Long::class.java, java.lang.Long::class.java -> (message as String).toLong()
                Double::class.java, java.lang.Double::class.java -> (message as String).toDouble()
                Float::class.java, java.lang.Float::class.java -> (message as String).toFloat()
                Boolean::class.java, java.lang.Boolean::class.java -> (message as String).toBoolean()
                else -> Gson().fromJson(message as String, expectedType)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize message to ${expectedType.name}", e)
            null
        }
    }

    /**
     * Invokes the subscriber's method with the given message and thread mode.
     *
     * @param subscription The subscription that holds the subscriber method.
     * @param message The message to pass to the subscriber's method.
     * @param wildcards Any wildcards extracted from the topic.
     */
    private fun invokeMethod(subscription: Subscription, message: Any, wildcards: List<String>? = null) {
        // Check if the subscription is still active
        if (!subscription.active) return

        val method = subscription.subscriberMethod.method
        val threadMode = subscription.subscriberMethod.threadMode
        val instance = subscription.subscriber

        val invoke = lambda@{
            try {
                val params = method.parameterTypes
                val args = when (params.size) {
                    1 -> arrayOf(convertMessage(message, params[0]))

                    2 -> arrayOf(
                        convertMessage(message, params[0]),
                        wildcards ?: emptyList<String>()
                    )

                    else -> throw IllegalArgumentException("Method ${method.name} in class ${instance::class.java.name} must have 1 or 2 parameters")
                }

                if (args[0] == null) return@lambda // Deserialization failed

                method.invoke(instance, *args)
            } catch (e: InvocationTargetException) {
                Log.e(TAG, "Method invocation failed", e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during method invocation", e)
            }
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
    @Throws(IllegalArgumentException::class)
    fun publish(topic: String, message: Any, qos: Int, retain: Boolean) {
        if (!bound) {
            Log.w(TAG, "publish: Service not yet bound! Message will be queued")
            return
        }
        if (qos < 0 || qos > 2) throw IllegalArgumentException("QoS must be between 0 and 2")

        topicMessenger?.publish(topic, message, qos, retain)
    }

    /**
     * Sets a custom [ExecutorService] for internal asynchronous operations.
     *
     * @param executor The executor to use for background tasks.
     */
    fun setExecutor(executor: ExecutorService) {
        this.executorService = executor
    }
}