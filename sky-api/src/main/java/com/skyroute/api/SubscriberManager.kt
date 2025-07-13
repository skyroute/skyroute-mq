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

import android.os.Handler
import android.os.Looper
import com.skyroute.TopicSubscriptionDelegate
import com.skyroute.api.adapter.DefaultPayloadAdapter
import com.skyroute.api.util.TopicUtils.matchesTopic
import com.skyroute.core.adapter.PayloadAdapter
import com.skyroute.core.util.Logger
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import kotlin.reflect.KClass

/**
 * Manages the subscriptions and delivery of messages to subscribers.
 *
 * @param logger The logger for logging messages.
 * @param payloadAdapter The global payload adapter used for message encoding and decoding.
 *
 * @author Andre Suryana
 */
internal class SubscriberManager(
    private val logger: Logger,
    private val payloadAdapter: PayloadAdapter,
    private val executorService: ExecutorService,
    private val topicDelegate: TopicSubscriptionDelegate,
) {

    private val subscriptionsByTopic: MutableMap<String, CopyOnWriteArrayList<Subscription>> = ConcurrentHashMap()
    private val typesBySubscriber: MutableMap<Any, MutableList<String>> = ConcurrentHashMap()
    private val adapterCache: MutableMap<KClass<out PayloadAdapter>, PayloadAdapter> = ConcurrentHashMap()

    /**
     * Registers a new subscriber.
     *
     * @param subscriber The subscriber object that has methods annotated with [Subscribe].
     */
    fun registerSubscriber(subscriber: Any) {
        val subscriberClass = subscriber::class.java
        val methods = subscriberClass.declaredMethods.filter {
            it.getAnnotation(Subscribe::class.java) != null
        }

        logger.d(TAG, "internalRegister: Registering ${methods.size} methods for subscriber: ${subscriberClass.name}")

        if (methods.isEmpty()) {
            logger.w(TAG, "No @Subscribe methods found in ${subscriberClass.name}")
            return
        }

        for (method in methods) {
            val annotation = method.getAnnotation(Subscribe::class.java)
                ?: throw IllegalArgumentException("Method ${method.name} in class ${subscriberClass.name} must be annotated with @Subscribe.")

            val topic = annotation.topic
            val qos = annotation.qos
            val threadMode = annotation.threadMode
            val adapterClass = annotation.adapter

            val subscriberMethod = SubscriberMethod(
                method = method,
                description = "${subscriberClass.name}#${method.name}",
                threadMode = threadMode,
                topic = topic,
                qos = qos,
                adapterClass = adapterClass,
            )

            val subscription = Subscription(subscriber, subscriberMethod)

            topicDelegate.subscribe(topic, qos)
            subscriptionsByTopic.getOrPut(topic) { CopyOnWriteArrayList() }.add(subscription)
            typesBySubscriber.getOrPut(subscriber) { mutableListOf() }.add(topic)
        }
    }

    /**
     * Unregisters a subscriber.
     *
     * @param subscriber The subscriber to unregister.
     */
    fun unregisterSubscriber(subscriber: Any) {
        val topics = typesBySubscriber[subscriber] ?: return

        topics.forEach { topic ->
            val subscriptions = subscriptionsByTopic[topic]

            val toRemove = mutableSetOf<Subscription>()
            subscriptions?.forEach { sub ->
                if (sub.subscriber == subscriber) {
                    logger.d(TAG, "Marked subscription as inactive: ${sub.subscriberMethod.description}")
                    sub.active = false
                    toRemove.add(sub)
                }
            }
            subscriptions?.removeAll(toRemove)

            // Only unsubscribe if topic has no remaining active subscriptions
            val stillActive = subscriptions?.any { it.active } ?: false
            if (!stillActive) {
                topicDelegate.unsubscribe(topic)
                logger.d(TAG, "Unsubscribed from topic: $topic (no active subscribers)")
            }
        }

        typesBySubscriber.remove(subscriber)
    }

    /**
     * Invokes the subscriber's method with the given message and thread mode.
     *
     * @param subscription The subscription that holds the subscriber method.
     * @param message The message to pass to the subscriber's method.
     * @param wildcards Any wildcards extracted from the topic.
     * @throws Exception if an error occurs during method invocation.
     */
    fun invoke(subscription: Subscription, message: ByteArray, wildcards: List<String>? = null) {
        if (!subscription.active) return

        val method = subscription.subscriberMethod.method
        val threadMode = subscription.subscriberMethod.threadMode
        val subscriber = subscription.subscriber

        var adapter = payloadAdapter
        val adapterClass = subscription.subscriberMethod.adapterClass

        if (adapterClass != DefaultPayloadAdapter::class) {
            adapter = adapterCache.getOrPut(adapterClass) {
                adapterClass.objectInstance ?: adapterClass.java.getDeclaredConstructor().newInstance()
            }
        }

        val invokeLambda = {
            try {
                val paramTypes = method.parameterTypes
                val decoded = adapter.decode(message, paramTypes[0])
                val args = when (paramTypes.size) {
                    1 -> arrayOf(decoded)
                    2 -> arrayOf(decoded, wildcards ?: emptyList<String>())
                    else -> throw IllegalArgumentException("Method ${method.name} must have 1 or 2 parameters")
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

        when (threadMode) {
            ThreadMode.MAIN -> {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    invokeLambda()
                } else {
                    Handler(Looper.getMainLooper()).post { invokeLambda() }
                }
            }

            ThreadMode.BACKGROUND -> {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    executorService.execute { invokeLambda() }
                } else {
                    invokeLambda()
                }
            }

            ThreadMode.ASYNC -> {
                executorService.execute { invokeLambda() }
            }
        }
    }

    /**
     * Iterates over all subscriptions that match the given topic and applies the given action.
     *
     * @param topic The topic to match against.
     * @param action The action to apply to each matching subscription.
     */
    fun forEachMatchingSubscriptions(topic: String, action: (Subscription) -> Unit) {
        subscriptionsByTopic.filterKeys { it.matchesTopic(topic) }
            .values
            .flatten()
            .forEach(action)
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

    companion object {
        private const val TAG = "SubscriberManager"
    }
}
