/*
 * Copyright (C) 2025 Andre Suryana, SkyRoute (https://github.com/skyroute)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.skyroute.core.mqtt

import java.io.InputStream

/**
 * Configuration for enabling SSL/TLS or mutual TLS (mTLS) in MQTT connections.
 *
 * This data class provides input streams for the necessary certificates and private keys
 * required to establish a secure connection to an MQTT broker using TLS or mTLS.
 *
 * TLS is enabled when a CA certificate is provided. For mTLS, the client certificate and private key
 * must also be supplied.
 *
 * @property caCertInput The input stream of the Certificate Authority (CA) certificate used to verify the server's certificate.
 * @property clientCertInput Optional input stream of the client's certificate for mutual TLS authentication.
 * @property clientKeyInput Optional input stream of the client's private key corresponding to the client certificate.
 * @property clientKeyPassword Optional password for the encrypted client private key, if applicable.
 *
 * @author Andre Suryana
 */
data class TlsConfig(
    val caCertInput: InputStream,
    val clientCertInput: InputStream? = null,
    val clientKeyInput: InputStream? = null,
    val clientKeyPassword: String? = null,
) {
    /**
     * Checks whether mutual TLS (mTLS) is configured.
     *
     * @return `true` if both the client certificate and private key are provided, `false` otherwise.
     */
    fun isMutualTls(): Boolean = clientCertInput != null && clientKeyInput != null
}
