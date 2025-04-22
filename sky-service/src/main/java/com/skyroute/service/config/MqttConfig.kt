package com.skyroute.service.config

/**
 * Configuration for establishing an MQTT connection.
 *
 * @param brokerUrl The MQTT broker URL (host and port).
 * @param clientId Unique identifier for the MQTT client.
 * @param cleanSession If true, a clean session will be used (defaults to true).
 * @param connectionTimeout Connection timeout in seconds (defaults to 10).
 * @param keepAliveInterval Interval in seconds for PING requests (defaults to 30).
 * @param maxInFlight Maximum number of in-flight messages (defaults to 10).
 * @param automaticReconnect If true, the client will automatically reconnect (defaults to true).
 * @param username Username for broker authentication (optional).
 * @param password Password for broker authentication (optional).
 * @param isEnableLog If true, enables logging (defaults to true).
 *
 * @author Andre Suryana
 */
data class MqttConfig(
    val brokerUrl: String,
    val clientId: String,
    val cleanSession: Boolean = true,
    val connectionTimeout: Int = 10,
    val keepAliveInterval: Int = 30,
    val maxInFlight: Int = 10,
    val automaticReconnect: Boolean = true,
    val username: String? = null,
    val password: String? = null,
    val isEnableLog: Boolean = true,
)
