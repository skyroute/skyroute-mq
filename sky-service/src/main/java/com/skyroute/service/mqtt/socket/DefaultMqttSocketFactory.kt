package com.skyroute.service.mqtt.socket

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import com.skyroute.core.mqtt.TlsConfig
import org.bouncycastle.openssl.PEMEncryptedKeyPair
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.SocketFactory
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

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
class DefaultMqttSocketFactory(
    context: Context,
) : MqttSocketFactory {

    // TODO: Refactor this into separate class!
    //  - `DefaultMqttSocketFactory` -> SocketFactory.getDefault()
    //  - `SecureMqttSocketFactory` -> Combine createSSLSocketFactory and createMutualSSLSocketFactory

    private val assetManager = context.applicationContext.assets

    override fun create(config: TlsConfig): SocketFactory {
        return when (config) {
            is TlsConfig.Disabled -> SocketFactory.getDefault()

            is TlsConfig.ServerAuth -> createSSLSocketFactory(
                assetManager.openOrThrow(config.caCertPath, "CA certificate"),
                config.skipVerify
            )

            is TlsConfig.MutualAuth -> createMutualSSLSocketFactory(
                assetManager.openOrThrow(config.caCertPath, "CA certificate"),
                assetManager.openOrThrow(config.clientCertPath, "client certificate"),
                assetManager.openOrThrow(config.clientKeyPath, "client key"),
                config.clientKeyPassword?.toCharArray() ?: CharArray(0),
                config.skipVerify,
            )
        }
    }

    private fun createSSLSocketFactory(caInput: InputStream, skipVerify: Boolean): SSLSocketFactory {
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            val cf = CertificateFactory.getInstance("X.509")
            val caCert = cf.generateCertificate(caInput)
            setCertificateEntry("ca", caCert)
        }

        return SSLContext.getInstance("TLS").apply {
            init(null, getTrustManagers(trustStore, skipVerify), SecureRandom())
        }.socketFactory
    }

    private fun createMutualSSLSocketFactory(
        caInput: InputStream,
        clientCertInput: InputStream,
        clientKeyInput: InputStream,
        clientKeyPassword: CharArray,
        skipVerify: Boolean,
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

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(clientStore, clientKeyPassword)
        }

        return SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, getTrustManagers(trustStore, skipVerify), SecureRandom())
        }.socketFactory
    }

    private fun getTrustManagers(trustStore: KeyStore, skipVerify: Boolean): Array<TrustManager> {
        return if (skipVerify) UNSAFE_TRUST_MANAGERS
        else TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(trustStore)
        }.trustManagers
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

    /**
     * Tries to open a file from assets. Throws a clear error if the file is missing.
     */
    private fun AssetManager.openOrThrow(path: String, description: String): InputStream =
        try {
            open(path)
        } catch (e: IOException) {
            throw IOException("Unable to open $description at '$path' from assets.", e)
        }

    companion object {
        @SuppressLint("CustomX509TrustManager")
        private val UNSAFE_TRUST_MANAGERS = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
    }
}
