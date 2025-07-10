package com.skyroute.service.mqtt.client

import org.eclipse.paho.mqttv5.client.IMqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions

/**
 * Data class representing a bundle of MQTT client and connection options.
 *
 * @param client The MQTT client instance.
 * @param options The MQTT connection options.
 *
 * @author Andre Suryana
 */
data class MqttClientBundle(
    val client: IMqttAsyncClient,
    val options: MqttConnectionOptions,
)
