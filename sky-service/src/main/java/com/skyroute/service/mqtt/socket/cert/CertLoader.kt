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
