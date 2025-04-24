package com.skyroute.api

import java.lang.reflect.Method

/**
 * Represents a method that subscribes to a specific topic in the MQTT system.
 * This data class holds information about the method to be invoked, its associated topic,
 * its description, and the thread mode in which the method should be executed.
 *
 * @param method The method to be invoked when a message is received on the associated topic.
 * @param description A human-readable description of the method.
 * @param threadMode The thread mode in which the method should be executed. Defaults to [ThreadMode.MAIN].
 * @param topic The topic that this method subscribed to.
 * @param qos The Quality of Service (QoS) level associated with this method.
 *
 * @author Andre Suryana
 */
internal data class SubscriberMethod(
    val method: Method,
    val description: String,
    val threadMode: ThreadMode = ThreadMode.MAIN,
    val topic: String,
    val qos: Int,
)
