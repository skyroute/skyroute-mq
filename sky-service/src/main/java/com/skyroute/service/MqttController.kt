package com.skyroute.service

import com.skyroute.service.config.MqttConfig

/**
 * Defines the basic MQTT operations for connecting, disconnecting,
 * and checking connection status.
 *
 * @author Andre Suryana
 */
interface MqttController {
    /**
     * Connects to the MQTT broker using with the given configuration.
     * Replaces any existing connection.
     *
     * @param config The configuration used for connecting to the broker.
     */
    fun connect(config: MqttConfig)

    /**
     * Disconnects from the MQTT broker connection.
     */
    fun disconnect()

    /**
     * Checks whether the client is currently connected to the MQTT broker.
     *
     * @return `true` if the client is connected, `false` otherwise.
     */
    fun isConnected(): Boolean
}