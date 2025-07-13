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
package com.skyroute.service.mqtt.socket.util

import android.annotation.SuppressLint
import com.skyroute.service.mqtt.socket.util.TrustManagerUtils.getTrustManagers
import com.skyroute.service.mqtt.socket.util.TrustManagerUtils.unsafeTrustManagers
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Utility class for providing [TrustManager]s for TLS/SSL connections.
 *
 * Supports generating both safe and unsafe trust managers based on
 * whether server verification should be skipped.
 * - Use [unsafeTrustManagers] for development or testing environments
 *   where certificate verification should be bypassed.
 * - Use [getTrustManagers] to load proper trust managers from a given [KeyStore].
 *
 * @author Andre Suryana
 */
object TrustManagerUtils {

    /**
     * An unsafe trust manager that disables all certificate checks.
     * Use only for testing or internal development.
     */
    @SuppressLint("CustomX509TrustManager")
    val unsafeTrustManagers = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    /**
     * Returns the appropriate [TrustManager] array based on verification setting.
     *
     * @param trustStore The keystore containing trusted certificates.
     * @param unsafe Whether use unsafe trust managers and skip server certificate verification.
     * @return An array of [TrustManager]s for SSL context initialization.
     */
    fun getTrustManagers(trustStore: KeyStore, unsafe: Boolean): Array<TrustManager> {
        return if (unsafe) {
            unsafeTrustManagers
        } else {
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                .apply { init(trustStore) }.trustManagers
        }
    }
}
