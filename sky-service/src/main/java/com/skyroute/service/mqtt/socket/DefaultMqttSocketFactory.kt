package com.skyroute.service.mqtt.socket

import com.skyroute.core.mqtt.TlsConfig
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
import java.security.cert.CertificateFactory
import javax.net.SocketFactory
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

/**
 * Default implementations of [MqttSocketFactory] that creates [SocketFactory] instances
 * based on the provided [TlsConfig].
 *
 * This factory supports:
 * - Plain (non-secure) connections via [TlsConfig.Disabled]
 * - One-way TLS (server-auth only) via [TlsConfig.ServerAuth]
 * - Mutual TLS (client and server authentication) via [TlsConfig.MutualAuth]
 *
 * It uses Java's standard [SSLContext] and [KeyStore] APIs, and supports encrypted PEM
 * private keys via Bouncy Castle when using mutual TLS.
 *
 * @author Andre Suryana
 */
class DefaultMqttSocketFactory : MqttSocketFactory {

    override fun create(config: TlsConfig): SocketFactory {
        return when (config) {
            is TlsConfig.Disabled -> SocketFactory.getDefault()

            is TlsConfig.ServerAuth -> createSSLSocketFactory(config.caInput)

            is TlsConfig.MutualAuth -> createMutualSSLSocketFactory(
                config.caInput,
                config.clientCertInput,
                config.clientKeyInput,
                config.clientKeyPassword?.toCharArray() ?: CharArray(0),
            )
        }
    }

    private fun createSSLSocketFactory(caInput: InputStream): SSLSocketFactory {
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            val cf = CertificateFactory.getInstance("X.509")
            val caCert = cf.generateCertificate(caInput)
            setCertificateEntry("ca", caCert)
        }

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(trustStore)
        }

        return SSLContext.getInstance("TLS").apply {
            init(null, tmf.trustManagers, SecureRandom())
        }.socketFactory
    }

    private fun createMutualSSLSocketFactory(
        caInput: InputStream,
        clientCertInput: InputStream,
        clientKeyInput: InputStream,
        clientKeyPassword: CharArray,
    ): SocketFactory {
        // Load CA
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            val cf = CertificateFactory.getInstance("X.509")
            val caCert = cf.generateCertificate(caInput)
            setCertificateEntry("ca", caCert)
        }

        // Load client certificate and private key
        val clientStore = KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            val cf = CertificateFactory.getInstance("X.509")
            val clientCert = cf.generateCertificate(clientCertInput)
            val key = loadPrivateKey(clientKeyInput, clientKeyPassword)
            setKeyEntry("client", key, clientKeyPassword, arrayOf(clientCert))
        }

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(trustStore)
        }

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(clientStore, clientKeyPassword)
        }

        return SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, tmf.trustManagers, SecureRandom())
        }.socketFactory
    }

    private fun loadPrivateKey(keyInput: InputStream, password: CharArray): PrivateKey {
        val parser = PEMParser(InputStreamReader(keyInput))
        val converter = JcaPEMKeyConverter().setProvider("BC")

        val pemObject = parser.readObject()
        parser.close()

        return when (pemObject) {
            is PEMEncryptedKeyPair -> {
                // Encrypted private key (legacy format like PKCS#1)
                val keyPair = pemObject.decryptKeyPair(
                    JcePEMDecryptorProviderBuilder().build(password)
                )
                converter.getPrivateKey(keyPair.privateKeyInfo)
            }

            is PEMKeyPair -> {
                // Unencrypted key pair
                converter.getPrivateKey(pemObject.privateKeyInfo)
            }

            else -> throw IllegalArgumentException("Unsupported private key format: ${pemObject?.javaClass?.name}")
        }
    }
}
