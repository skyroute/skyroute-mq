package com.skyroute.service.mqtt.socket.cert

import android.content.Context
import com.skyroute.core.mqtt.TlsConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * Factory responsible for providing [CertLoader] implementations
 * based on the file scheme prefix in the provided certificate path.
 *
 * Supports caching for reuse of loaders per scheme to improve performance.
 *
 * Supported schemes:
 * - `asset://` - Loads from Android assets.
 * - `file://` - Loads from local filesystem.
 * - `raw://` - Loads from raw resources.
 *
 * Example:
 * ```
 * val loader = CertLoaderFactory(context).create("asset://certs/ca.crt")
 * val inputStream = loader.load("asset://certs/ca.crt")
 * ```
 *
 * @author Andre Suryana
 */
class CertLoaderFactory(private val context: Context) {

    private val cache = ConcurrentHashMap<String, CertLoader>()

    /**
     * Creates or retrieves a [CertLoader] based on the scheme in the provided [path].
     *
     * @param path A file path (e.g., `asset://cert.pem`, `file://...`, `raw://cert`).
     * @return A [CertLoader] instance suitable for the given scheme.
     * @throws IllegalArgumentException if the scheme is unsupported.
     */
    fun create(path: String): CertLoader {
        val scheme = TlsConfig.supportedSchemes.find { path.startsWith(it) }
            ?: throw IllegalArgumentException("Unsupported certificate path scheme: $path")

        return cache.getOrPut(scheme) {
            when (scheme) {
                TlsConfig.PREFIX_ASSET -> AssetCertLoader(context)
                TlsConfig.PREFIX_FILE -> FileCertLoader()
                TlsConfig.PREFIX_RAW -> RawResCertLoader(context)
                else -> throw IllegalStateException("Unknown scheme: $path")
            }
        }
    }
}
