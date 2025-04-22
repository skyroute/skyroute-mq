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
import com.skyroute.service.SkyRouteService
import com.skyroute.service.TopicMessenger
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.Volatile

class SkyRoute private constructor() {

    companion object {
        private const val TAG = "SkyRoute"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: SkyRoute? = null

        fun getDefault(): SkyRoute = instance ?: synchronized(this) {
            instance ?: SkyRoute().also { instance = it }
        }
    }

    private val subscriptionsByTopic: MutableMap<String, CopyOnWriteArrayList<Subscription>> = ConcurrentHashMap()
    private val typesBySubscriber: MutableMap<Any, MutableList<String>> = ConcurrentHashMap()
    private val pendingRegistrations = mutableListOf<Any>()

    private var topicMessenger: TopicMessenger? = null
    private var bound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "onServiceConnected!")
            (binder as? SkyRouteService.SkyRouteBinder)?.let {
                topicMessenger = it.getTopicMessenger()
                bound = true

                // Register message arrival
                topicMessenger?.onMessageArrival { topic, message ->
                    val subscriptions = subscriptionsByTopic[topic]
                    subscriptions?.forEach { subscription ->
                        invokeMethod(subscription, message)
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
            Log.d(TAG, "onServiceDisconnected!")
            topicMessenger = null
            bound = false
        }
    }

    fun init(context: Context) {
        if (bound) return
        context.applicationContext.run { // Using application context to avoid memory leaks
            val intent = Intent(this, SkyRouteService::class.java)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun register(subscriber: Any) {
        if (bound) {
            internalRegister(subscriber)
        } else {
            Log.i(TAG, "Service not yet bound. Queuing subscriber: ${subscriber::class.java.name}")
            synchronized(pendingRegistrations) {
                pendingRegistrations.add(subscriber)
            }
        }
    }

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
            val parameterTypes = method.parameterTypes
            if (parameterTypes.size != 1) {
                throw IllegalArgumentException("Method ${method.name} in class ${subscriberClass.name} must have exactly one parameter.")
            }

            val subscribeAnnotation = method.getAnnotation(Subscribe::class.java)
                ?: throw IllegalArgumentException("Method ${method.name} in class ${subscriberClass.name} must be annotated with @Subscribe.")

            val topic = subscribeAnnotation.topic
            val threadMode = subscribeAnnotation.threadMode
            val parameterType = parameterTypes[0]

            // Wrap the method as a lambda
            val subscriberMethod = SubscriberMethod(
                method = { message ->
                    method.isAccessible = true

                    // Skip calling the method if deserialization fails
                    val finalMessage = convertMessage(message, parameterType) ?: return@SubscriberMethod

                    try {
                        method.invoke(subscriber, finalMessage)
                    } catch (e: InvocationTargetException) {
                        Log.e(TAG, "Method invocation failed", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "Unknown error", e)
                    }
                },
                description = "${subscriberClass.name}#${method.name}(${parameterType.name})",
                threadMode = threadMode
            )

            val subscription = Subscription(subscriber, subscriberMethod)

            // Subscribe to the topic
            topicMessenger?.subscribe(topic)

            // Register into the maps
            subscriptionsByTopic.getOrPut(topic) { CopyOnWriteArrayList() }.add(subscription)
            typesBySubscriber.getOrPut(subscriber) { mutableListOf() }.add(topic)
        }
    }

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

    private fun invokeMethod(subscription: Subscription, message: Any) {
        // Check if the subscription is still active
        if (!subscription.active) return

        val method = subscription.subscriberMethod.method
        val threadMode = subscription.subscriberMethod.threadMode

        val invoke = {
            try {
                method.invoke(message)
            } catch (e: InvocationTargetException) {
                Log.e(TAG, "Method invocation failed", e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during method invocation", e)
            }
        }

        when (threadMode) {
            ThreadMode.POSTING, ThreadMode.MAIN -> {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    invoke()
                } else {
                    Handler(Looper.getMainLooper()).post { invoke() }
                }
            }

            ThreadMode.BACKGROUND -> {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    Thread { invoke() }.start()
                } else {
                    invoke()
                }
            }

            ThreadMode.ASYNC -> {
                Thread { invoke() }.start()
            }
        }
    }

    fun isRegistered(subscriber: Any): Boolean {
        return typesBySubscriber.containsKey(subscriber)
    }

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

    fun publish(topic: String, message: Any) {
        publish(topic, message, 0, false)
    }

    fun publish(topic: String, message: Any, qos: Int) {
        publish(topic, message, qos, false)
    }

    fun publish(topic: String, message: Any, qos: Int, retain: Boolean) {
        if (!bound) {
            Log.w(TAG, "publish: Service not yet bound! Message will be queued")
            return
        }
        topicMessenger?.publish(topic, message, qos, retain)
    }
}