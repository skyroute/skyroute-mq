package com.skyroute

internal interface TopicSubscriptionDelegate {
    fun subscribe(topic: String, qos: Int)
    fun unsubscribe(topic: String)
}
