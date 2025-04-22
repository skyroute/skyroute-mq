package com.skyroute.service

interface TopicMessenger {

    /**
     * Subscribe to a topic with a given QoS.
     *
     * @param topic The topic to subscribe to.
     * @param qos The quality of service level (0, 1, or 2).
     */
    fun subscribe(topic: String, qos: Int = 0)

    /**
     * Unsubscribe from a topic.
     *
     * @param topic The topic to unsubscribe from.
     */
    fun unsubscribe(topic: String)

    /**
     * Publish a message to a topic with a given QoS.
     *
     * @param topic The topic to publish to.
     * @param message The message to publish.
     * @param qos The quality of service level (0, 1, or 2).
     * @param retain Whether the message should be retained.
     */
    fun publish(topic: String, message: Any, qos: Int = 0, retain: Boolean = false)

    /**
     * Register a callback to receive messages on a specific topic.
     *
     * @param callback The callback function to be called when a message is received.
     */
    fun onMessageArrival(callback: (topic: String, message: Any) -> Unit)
}