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
package com.skyroute.service.mqtt

import com.skyroute.core.mqtt.TlsConfig
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMEncryptedKeyPair
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder
import java.io.InputStream
import java.io.InputStreamReader
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

/**
 * Default MQTT socket factory to create [SSLSocketFactory] instance
 * based on the given [TlsConfig] configuration class.
 *
 * @author Andre Suryana
 */
class DefaultMqttSocketFactory : MqttSocketFactory {

    init {
        // Add BouncyCastle security provider if not already present
        Security.addProvider(BouncyCastleProvider())
    }

    override fun createSocketFactory(config: TlsConfig): SSLSocketFactory {
        val tmf = createTrustManagerFactory(config.caCertInput)

        // Create KeyManagerFactory only if mutual TLS is enabled
        val kmf = if (config.isMutualTls()) {
            createKeyManagerFactory(
                config.clientCertInput!!,
                config.clientKeyInput!!,
                config.clientKeyPassword,
            )
        } else {
            null
        }

        // Initialize SSLContext with trust and (optional) key managers
        return SSLContext.getInstance("TLSv1.2").apply {
            init(kmf?.keyManagers, tmf.trustManagers, SecureRandom())
        }.socketFactory
    }

    /**
     * Loads the CA certificate and initializes a TrustManagerFactory.
     */
    private fun createTrustManagerFactory(caCertInput: InputStream): TrustManagerFactory {
        val caCert = CertificateFactory.getInstance("X.509")
            .generateCertificate(caCertInput) as X509Certificate

        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null) // Initialize an empty key store
            setCertificateEntry("ca", caCert)
        }

        return TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(trustStore)
        }
    }

    /**
     * Loads the client certificate and private key, builds a KeyManagerFactory
     */
    private fun createKeyManagerFactory(
        certInput: InputStream,
        keyInput: InputStream,
        password: String?,
    ): KeyManagerFactory {
        val cert = CertificateFactory.getInstance("X.509")
            .generateCertificate(certInput) as X509Certificate

        val privateKey = loadPrivateKey(keyInput, password)
        val keyStorePassword = password?.toCharArray() ?: CharArray(0)

        val keyStore = KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry("client", privateKey, keyStorePassword, arrayOf(cert))
        }

        return KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, password?.toCharArray())
        }
    }

    /**
     * Reads a PEM-encoded private key (encrypted or unencrypted).
     * Supports:
     * - Encrypted PEM: requires password
     * - Unencrypted PEM: password is ignored
     * - PKCS#8 Private Key Info
     */
    private fun loadPrivateKey(input: InputStream, password: String?): PrivateKey {
        PEMParser(InputStreamReader(input)).use { parser ->
            val obj = parser.readObject()
            val converter = JcaPEMKeyConverter().setProvider("BC")

            return when (obj) {
                is PEMEncryptedKeyPair -> {
                    if (password == null) {
                        throw IllegalArgumentException("Encrypted key requires a password")
                    }

                    // Decrypt using the provided password
                    val decryptor = JcePEMDecryptorProviderBuilder().build(password.toCharArray())
                    converter.getKeyPair(obj.decryptKeyPair(decryptor)).private
                }

                is PEMKeyPair -> {
                    // Unencrypted PEM (BEGIN RSA PRIVATE KEY or similar)
                    converter.getKeyPair(obj).private
                }

                is PrivateKeyInfo -> {
                    // PKCS#8 private key format
                    converter.getPrivateKey(obj)
                }

                else -> throw IllegalArgumentException("Unsupported PEM format of '${obj.javaClass}'")
            }
        }
    }
}
