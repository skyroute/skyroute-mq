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

import android.annotation.SuppressLint
import android.content.Context
import com.skyroute.core.mqtt.TlsConfig
import java.io.IOException
import java.io.InputStream

/**
 * A [CertLoader] that loads certificates from the application's raw resources.
 *
 * @author Andre Suryana
 */
class RawResCertLoader(private val context: Context) : CertLoader {

    @SuppressLint("DiscouragedApi")
    override fun load(path: String): InputStream = try {
        val resName = path.removePrefix(TlsConfig.PREFIX_RAW)
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
        if (resId == 0) throw IllegalArgumentException("Raw resource not found: $resName")
        context.resources.openRawResource(resId)
    } catch (e: Exception) {
        throw IOException("Unable to open $path from raw resources.", e)
    }
}
