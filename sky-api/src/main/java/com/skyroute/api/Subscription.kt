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

import kotlin.concurrent.Volatile

/**
 * Represents a subscription to a specific topic for a subscriber.
 * Holds the subscriber instance and the method to be invoked when a message for the subscribed topic is received.
 * The subscription is active by default and can be deactivated when the subscriber is unregistered.
 *
 * @param subscriber The subscriber instance that will handle the incoming messages.
 * @param subscriberMethod The method of the subscriber to be invoked when a message matching the topic is received.
 *
 * @author Andre Suryana
 */
internal class Subscription(
    val subscriber: Any,
    val subscriberMethod: SubscriberMethod,
) {
    /**
     * Active state of the subscription. Set to `false` when the subscription is unregistered.
     * This is a volatile field to ensure visibility across threads.
     */
    @Volatile
    var active: Boolean = true // Active state, will be set to false when unregistered

    override fun equals(other: Any?): Boolean {
        return if (other is Subscription) {
            this.subscriber == other.subscriber && this.subscriberMethod == other.subscriberMethod
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return subscriber.hashCode() + subscriberMethod.description.hashCode()
    }
}
