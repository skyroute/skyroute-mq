package com.skyroute.service.mqtt.socket.cert

import java.io.InputStream

/**
 * Certificate loader interface for resolving certificate or key files
 * based on their path scheme (e.g., `asset://ca.crt`, `file://...`, etc.).
 *
 * Implementation of this interface are responsible for loading and returning
 * an [InputStream] for a given certificate or key path.
 *
 * @author Andre Suryana
 */
interface CertLoader {

    /**
     * Loads a certificate or key from the given [path].
     *
     * @param path the file path pointing to the certificate or key (e.g., `asset://ca.crt`).
     * @return An [InputStream] to read the contents of the specified file.
     * @throws java.io.IOException if the file cannot be loaded.
     */
    fun load(path: String): InputStream
}
