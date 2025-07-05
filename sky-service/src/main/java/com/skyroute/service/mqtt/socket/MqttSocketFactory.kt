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
package com.skyroute.service.mqtt.socket

import com.skyroute.core.mqtt.TlsConfig
import javax.net.SocketFactory

/**
 * Factory interface for creating [SocketFactory] instances
 * using the provided [TlsConfig] for TLS or mutual TLS connections.
 *
 * Allows custom or testable implementations.
 *
 * @author Andre Suryana
 */
interface MqttSocketFactory {

    /**
     * Creates an [SocketFactory] based on the given [config].
     *
     * @param config TLS/mTLS configuration including certs and keys.
     * @return a configured [SocketFactory].
     */
    fun create(config: TlsConfig): SocketFactory
}
