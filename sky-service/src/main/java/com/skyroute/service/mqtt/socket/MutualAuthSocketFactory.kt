package com.skyroute.service.mqtt.socket

import com.skyroute.core.mqtt.TlsConfig
import com.skyroute.service.mqtt.socket.cert.CertLoader
import com.skyroute.service.mqtt.socket.util.TrustManagerUtils.getTrustManagers
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
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

/**
 * Creates [SSLSocketFactory] instances configured for mutual TLS (mTLS).
 *
 * It supports loading encrypted or unencrypted PEM keys using Bouncy Castle.
 *
 * @param caCertLoader Loader for the CA certificate used to verify the server.
 * @param clientCertLoader Loader for the client certificate presented to the server.
 * @param clientKeyLoader Loader for the client's private key.
 *
 * @author Andre Suryana
 */
class MutualAuthSocketFactory(
    private val caCertLoader: CertLoader,
    private val clientCertLoader: CertLoader,
    private val clientKeyLoader: CertLoader,
) {

    /**
     * Creates an [SSLSocketFactory] configured for mutual TLS (mTLS) using the provided [TlsConfig.MutualAuth].
     *
     * This sets up an SSL context that:
     * - Verifies the server certificate using the provided CA certificate.
     * - Presents the client certificate and private key to the server for authentication.
     *
     * @param config TLS configuration including paths to CA certificate, client certificate, and private key.
     * @return A configured [SSLSocketFactory] for mTLS communication.
     * @throws java.io.IOException if any certificate or key cannot be loaded.
     */
    fun create(config: TlsConfig.MutualAuth): SSLSocketFactory {
        val caInput = caCertLoader.load(config.caCertPath)
        val clientCertInput = clientCertLoader.load(config.clientCertPath)
        val clientKeyInput = clientKeyLoader.load(config.clientKeyPath)
        val keyPassword = config.clientKeyPassword?.toCharArray() ?: CharArray(0)

        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            val cf = CertificateFactory.getInstance("X.509")
            setCertificateEntry("ca", cf.generateCertificate(caInput))
        }

        // Load client certificate and private key
        val clientStore = KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            val cf = CertificateFactory.getInstance("X.509")
            val clientCert = cf.generateCertificate(clientCertInput)
            val key = loadPrivateKey(clientKeyInput, keyPassword)
            setKeyEntry("client", key, keyPassword, arrayOf(clientCert))

        }

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(clientStore, keyPassword)
        }

        return SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, getTrustManagers(trustStore, config.skipVerify), SecureRandom())
        }.socketFactory
    }

    private fun loadPrivateKey(keyInput: InputStream, password: CharArray): PrivateKey {
        val parser = PEMParser(InputStreamReader(keyInput))
        val converter = JcaPEMKeyConverter().setProvider("BC")
        val obj = parser.readObject()
        parser.close()
        return when (obj) {
            is PEMEncryptedKeyPair -> {
                // Encrypted private key (legacy format like PKCS#1)
                val keyPair = obj.decryptKeyPair(
                    JcePEMDecryptorProviderBuilder().build(password)
                )
                converter.getPrivateKey(keyPair.privateKeyInfo)
            }

            is PEMKeyPair -> {
                // Unencrypted key pair
                converter.getPrivateKey(obj.privateKeyInfo)
            }

            else -> throw IllegalArgumentException("Unsupported private key format: ${obj?.javaClass?.name}")
        }
    }
}
