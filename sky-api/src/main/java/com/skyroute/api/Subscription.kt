package com.skyroute.api

import kotlin.concurrent.Volatile

internal data class SubscriberMethod(
    val method: (Any) -> Unit,
    val description: String,
    val threadMode: ThreadMode = ThreadMode.MAIN,
)

internal class Subscription(
    val subscriber: Any,
    val subscriberMethod: SubscriberMethod,
) {
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