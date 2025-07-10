package com.skyroute.service.mqtt.socket

import com.skyroute.core.mqtt.TlsConfig
import com.skyroute.service.mqtt.socket.cert.CertLoader
import com.skyroute.service.mqtt.socket.util.TrustManagerUtils.getTrustManagers
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager

/**
 * Creates [SSLSocketFactory] instances configured for server-authenticated TLS.
 *
 * This factory loads the CA certificate using the provided [caCertLoader],
 * and uses it to configure a [TrustManager] that verifies the server certificate.
 *
 * @param caCertLoader Loader for the CA certificate used to trust the server.
 *
 * @author Andre Suryana
 */
class ServerAuthSocketFactory(private val caCertLoader: CertLoader) {

    /**
     * Creates an [SSLSocketFactory] configured for one-way TLS (server authentication).
     *
     * This sets up an SSL context that verifies the server's identity using the provided
     * CA certificate. No client certificate is used for authentication.
     *
     * @param config TLS configuration including the CA certificate path and skipVerify flag.
     * @return A configured [SSLSocketFactory] for secure server-authenticated connections.
     * @throws java.io.IOException if the CA certificate cannot be loaded.
     */
    fun create(config: TlsConfig.ServerAuth): SSLSocketFactory {
        val caInput = caCertLoader.load(config.caCertPath)
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            val cf = CertificateFactory.getInstance("X.509")
            val caCert = cf.generateCertificate(caInput)
            setCertificateEntry("ca", caCert)
        }

        return SSLContext.getInstance("TLS").apply {
            init(null, getTrustManagers(trustStore, config.skipVerify), SecureRandom())
        }.socketFactory
    }
}
