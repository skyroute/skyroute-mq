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

import android.os.Parcelable
import com.skyroute.core.mqtt.TlsConfig.Disabled
import com.skyroute.core.mqtt.TlsConfig.MutualAuth
import com.skyroute.core.mqtt.TlsConfig.ServerAuth
import kotlinx.parcelize.Parcelize

/**
 * TLS configuration for securing MQTT connections.
 *
 * This sealed class defines different modes of TLS usage:
 *
 * - [Disabled]: No TLS; connection is made over plaintext.
 * - [ServerAuth]: One-way TLS using a CA certificate to verify the server.
 * - [MutualAuth]: Mutual TLS (mTLS), where both client and server authenticate each other using certificates.
 *
 * @see Disabled
 * @see ServerAuth
 * @see MutualAuth
 *
 * @author Andre Suryana
 */
@Parcelize
sealed class TlsConfig : Parcelable {

    /**
     * Disables TLS. Connections will be made in plaintext.
     */
    data object Disabled : TlsConfig()

    /**
     * Enables TLS with server authentication using a CA certificate.
     *
     * This configuration verifies the MQTT broker's certificate using the provided CA certificate.
     *
     * @property caCertPath Input stream of the Certificate Authority (CA) certificate used to verify the server's identity.
     * @property skipVerify If true, skips verification of the server's certificate chain and hostname.
     *                      This allows connections to servers with self-signed or untrusted certificates.
     */
    data class ServerAuth(
        val caCertPath: String,
        val skipVerify: Boolean = false,
    ) : TlsConfig()

    /**
     * Enables mutual TLS (mTLS) with both server and client authentication.
     *
     * This configuration verifies the server using a CA certificate and also presents a client certificate
     * and private key to authenticate the client to the server.
     *
     * @property caCertPath Input stream of the Certificate Authority (CA) certificate used to verify the server's certificate.
     * @property clientCertPath Input stream of the client's certificate for mutual TLS authentication.
     * @property clientKeyPath Input stream of the client's private key corresponding to the client certificate.
     * @property clientKeyPassword Optional password for the client private key, if it is encrypted.
     * @property skipVerify If true, skips verification of the server's certificate chain and hostname.
     *                      This allows connections to servers with self-signed or untrusted certificates.
     */
    data class MutualAuth(
        val caCertPath: String,
        val clientCertPath: String,
        val clientKeyPath: String,
        val clientKeyPassword: String? = null,
        val skipVerify: Boolean = false,
    ) : TlsConfig()

    fun isSameConfig(other: TlsConfig?): Boolean {
        if (other == null) return false
        if (this === other) return true
        return when {
            this is Disabled && other is Disabled -> true
            this is ServerAuth && other is ServerAuth -> this.caCertPath == other.caCertPath && this.skipVerify == other.skipVerify
            this is MutualAuth && other is MutualAuth ->
                this.caCertPath == other.caCertPath &&
                    this.clientCertPath == other.clientCertPath &&
                    this.clientKeyPath == other.clientKeyPath &&
                    this.clientKeyPassword == other.clientKeyPassword &&
                    this.skipVerify == other.skipVerify

            else -> false
        }
    }
}
