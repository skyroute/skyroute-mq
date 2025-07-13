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
