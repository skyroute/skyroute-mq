package com.skyroute.service

/**
 * Type alias for the callback function to handle message arrivals.
 */
typealias MessageArrival = (topic: String, message: Any) -> Unit

/**
 * Interface for handling MQTT topic-based messaging operations.
 *
 * @author Andre Suryana
 */
interface TopicMessenger {

    /**
     * Subscribes to a topic with a specified QoS.
     *
     * @param topic The topic to subscribe to.
     * @param qos The Quality of Service level (0, 1, or 2, default is 0).
     */
    fun subscribe(topic: String, qos: Int = 0)

    /**
     * Unsubscribes from a topic.
     *
     * @param topic The topic to unsubscribe from.
     */
    fun unsubscribe(topic: String)

    /**
     * Publish a message to a topic with a specified QoS.
     *
     * @param topic The topic to publish to.
     * @param message The message to publish.
     * @param qos The Quality of Service level (0, 1, or 2, default is 0).
     * @param retain Whether to retain the message (default is `false`).
     */
    fun publish(topic: String, message: Any, qos: Int = 0, retain: Boolean = false)

    /**
     * Register a callback to handle incoming messages for a specific topic.
     *
     * @param callback A function that processes the received message.
     */
    fun onMessageArrival(callback: MessageArrival)

}