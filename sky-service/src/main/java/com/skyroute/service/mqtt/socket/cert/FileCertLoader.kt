package com.skyroute.service.mqtt.socket.cert

import com.skyroute.core.mqtt.TlsConfig
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * A [CertLoader] that loads certificates from the application's file system.
 *
 * @author Andre Suryana
 */
class FileCertLoader : CertLoader {

    override fun load(path: String): InputStream = try {
        val cleanPath = path.removePrefix(TlsConfig.PREFIX_FILE)
        File(cleanPath).inputStream()
    } catch (e: IOException) {
        throw IOException("Unable to open $path from file system.", e)
    }
}
