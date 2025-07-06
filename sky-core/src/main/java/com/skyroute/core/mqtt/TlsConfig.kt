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
 * TLS configuration for MQTT connections.
 *
 * This sealed class represents the available transport layer security (TLS) configurations used
 * to establish a secure connection with an MQTT broker.
 *
 * - [Disabled] disables TLS entirely.
 * - [ServerAuth] enables one-way TLS using a CA certificate to authenticate the server.
 * - [MutualAuth] enables mutual TLS (mTLS) where both the client and server are authenticated.
 *
 * All certificates and keys are expected to be provided as [InputStream]s. These streams must remain
 * open while creating the MQTT connection but may be closed afterward.
 *
 * @see Disabled
 * @see ServerAuth
 * @see MutualAuth
 *
 * @author Andre Suryana
 */
sealed class TlsConfig {

    /**
     * Disables TLS. Connections will be made in plaintext.
     */
    data object Disabled : TlsConfig()

    /**
     * Enables TLS with server authentication using a CA certificate.
     *
     * This configuration verifies the MQTT broker's certificate using the provided CA certificate.
     *
     * @property caInput Input stream of the Certificate Authority (CA) certificate used to verify the server's identity.
     * @property skipVerify If true, skips verification of the server's certificate chain and hostname.
     *                      This allows connections to servers with self-signed or untrusted certificates.
     */
    data class ServerAuth(
        val caInput: InputStream,
        val skipVerify: Boolean = false,
    ) : TlsConfig()

    /**
     * Enables mutual TLS (mTLS) with both server and client authentication.
     *
     * This configuration verifies the server using a CA certificate and also presents a client certificate
     * and private key to authenticate the client to the server.
     *
     * @property caInput Input stream of the Certificate Authority (CA) certificate used to verify the server's certificate.
     * @property clientCertInput Input stream of the client's certificate for mutual TLS authentication.
     * @property clientKeyInput Input stream of the client's private key corresponding to the client certificate.
     * @property clientKeyPassword Optional password for the client private key, if it is encrypted.
     * @property skipVerify If true, skips verification of the server's certificate chain and hostname.
     *                      This allows connections to servers with self-signed or untrusted certificates.
     */
    data class MutualAuth(
        val caInput: InputStream,
        val clientCertInput: InputStream,
        val clientKeyInput: InputStream,
        val clientKeyPassword: String? = null,
        val skipVerify: Boolean = false,
    ) : TlsConfig()
}
