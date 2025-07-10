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
