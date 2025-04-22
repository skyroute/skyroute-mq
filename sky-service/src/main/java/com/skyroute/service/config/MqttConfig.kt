package com.skyroute.service.config

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
