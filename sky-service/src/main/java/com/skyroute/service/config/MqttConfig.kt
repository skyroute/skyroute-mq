package com.skyroute.service.config

/**
 * Configuration for establishing an MQTT connection.
 *
 * @param brokerUrl The MQTT broker URL (host and port).
 * @param clientPrefix Client prefix for the MQTT client identifier.
 * @param cleanSession If true, a clean session will be used (defaults to true).
 * @param connectionTimeout Connection timeout in seconds (defaults to 10).
 * @param keepAliveInterval Interval in seconds for PING requests (defaults to 30).
 * @param maxInFlight Maximum number of in-flight messages (defaults to 10).
 * @param automaticReconnect If true, the client will automatically reconnect (defaults to true).
 * @param username Username for broker authentication (optional).
 * @param password Password for broker authentication (optional).
 *
 * @author Andre Suryana
 * @todo Since this data model used in both `sky-api` and `sky-service`, we should extract it to a `common` module
 */
data class MqttConfig(
    val brokerUrl: String,
    val clientPrefix: String,
    val cleanSession: Boolean = true,
    val connectionTimeout: Int = 10,
    val keepAliveInterval: Int = 30,
    val maxInFlight: Int = 10,
    val automaticReconnect: Boolean = true,
    val username: String? = null,
    val password: String? = null,
) {
    fun generateClientId(): String {
        val suffix = System.currentTimeMillis()
        val separator = if (clientPrefix.endsWith('-')) "" else "-"
        return "$clientPrefix$separator$suffix"
    }
}
