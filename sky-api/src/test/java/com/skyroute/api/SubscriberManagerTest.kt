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

import com.skyroute.api.adapter.DefaultPayloadAdapter
import com.skyroute.core.adapter.PayloadAdapter
import com.skyroute.core.util.Logger
import com.skyroute.core.util.TestLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class SubscriberManagerTest {

    private lateinit var logger: Logger
    private lateinit var adapter: PayloadAdapter
    private lateinit var executorService: ExecutorService
    private lateinit var topicDelegate: RecordingTopicDelegate
    private lateinit var subscriberManager: SubscriberManager

    private val testTopic = "test/topic"

    class TestSubscriber {
        @Subscribe(topic = "test/topic", qos = 1, threadMode = ThreadMode.ASYNC)
        fun onMessage(msg: String) {
            receivedMessage.set(msg)
        }

        companion object {
            var receivedMessage = AtomicReference<String?>()
        }
    }

    class RecordingTopicDelegate : TopicSubscriptionDelegate {
        val subscribedTopics = mutableListOf<Pair<String, Int>>()
        val unsubscribedTopics = mutableListOf<String>()

        override fun subscribe(topic: String, qos: Int) {
            subscribedTopics.add(topic to qos)
        }

        override fun unsubscribe(topic: String) {
            unsubscribedTopics.add(topic)
        }
    }

    @Before
    fun setUp() {
        logger = TestLogger()
        adapter = DefaultPayloadAdapter()
        executorService = Executors.newSingleThreadExecutor()
        topicDelegate = RecordingTopicDelegate()

        subscriberManager = SubscriberManager(
            logger = logger,
            payloadAdapter = adapter,
            executorService = executorService,
            topicDelegate = topicDelegate,
        )
    }

    @Test
    fun `registerSubscriber should register methods with Subscribe annotation`() {
        val subscriber = TestSubscriber()
        subscriberManager.registerSubscriber(subscriber)

        assertTrue(subscriberManager.isRegistered(subscriber))
        assertEquals(listOf("test/topic" to 1), topicDelegate.subscribedTopics)
    }

    @Test
    fun `unregisterSubscriber should remove subscriptions and call unsubscribe`() {
        val subscriber = TestSubscriber()
        subscriberManager.registerSubscriber(subscriber)
        assertTrue(subscriberManager.isRegistered(subscriber))

        subscriberManager.unregisterSubscriber(subscriber)
        assertFalse(subscriberManager.isRegistered(subscriber))

        assertEquals(listOf("test/topic"), topicDelegate.unsubscribedTopics)
    }

    @Test
    fun `invoke should decode payload and call method one param`() {
        val subscriber = TestSubscriber()
        subscriberManager.registerSubscriber(subscriber)

        val subscription = Subscription(
            subscriber,
            SubscriberMethod(
                method = TestSubscriber::class.java.getDeclaredMethod("onMessage", String::class.java),
                description = "TestSubscriber#onMessage",
                threadMode = ThreadMode.ASYNC,
                topic = testTopic,
                qos = 1,
                adapterClass = DefaultPayloadAdapter::class,
            ),
        )

        val payload = "Hello".toByteArray()
        subscriberManager.invoke(subscription, payload)

        // Give time for async execution
        executorService.awaitTermination(100, TimeUnit.MILLISECONDS)

        assertEquals("Hello", TestSubscriber.receivedMessage.get())
    }

    @Test
    fun `forEachMatchingSubscriptions should apply action to matching subscriptions`() {
        val subscriber = TestSubscriber()
        subscriberManager.registerSubscriber(subscriber)

        var count = 0
        subscriberManager.forEachMatchingSubscriptions(testTopic) {
            count++
        }

        assertEquals(1, count)
    }

    @Test
    fun `isRegistered should return false for unregistered subscriber`() {
        val subscriber = TestSubscriber()
        assertFalse(subscriberManager.isRegistered(subscriber))
    }
}
