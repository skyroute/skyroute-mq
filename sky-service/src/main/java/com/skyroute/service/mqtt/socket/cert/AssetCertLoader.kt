package com.skyroute.service.mqtt.socket.cert

import android.content.Context
import android.content.res.AssetManager
import com.skyroute.core.mqtt.TlsConfig
import java.io.IOException
import java.io.InputStream

/**
 * A [CertLoader] that loads certificates from the application's assets.
 *
 * @author Andre Suryana
 */
class AssetCertLoader(context: Context) : CertLoader {

    private val assetManager = context.applicationContext.assets

    override fun load(path: String): InputStream = try {
        val cleanPath = path.removePrefix(TlsConfig.PREFIX_ASSET)
        assetManager.open(cleanPath, AssetManager.ACCESS_BUFFER)
    } catch (e: IOException) {
        throw IOException("Unable to open $path from assets.", e)
    }
}
