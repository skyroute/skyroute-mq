package com.skyroute.api

/**
 * Identifies a method as a subscriber to a topic in the SkyRouteMQ event system.
 *
 * Methods annotated with `@Subscribe` will be automatically registered to receive
 * messages published to the specified topic, and executed according to the defined [threadMode].
 *
 * Usage example:
 * ```
 * @Subscribe(topic = "test/topic", threadMode = ThreadMode.BACKGROUND)
 * fun onMessageReceived(message: String) {
 *     // Handle the message
 * }
 * ```
 *
 * @property topic The MQTT topic to subscribe to.
 * @property threadMode The thread mode for executing the subscriber. Defaults to [ThreadMode.MAIN].
 *
 * @author Andre Suryana
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Subscribe(
    val topic: String,
    val threadMode: ThreadMode = ThreadMode.MAIN,
)
