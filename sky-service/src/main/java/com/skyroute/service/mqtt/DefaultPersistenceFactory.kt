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
package com.skyroute.service.mqtt

import android.content.Context
import org.eclipse.paho.mqttv5.client.MqttClientPersistence
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence
import java.io.File

/**
 * Default implementation of [PersistenceFactory] that uses a local file-based
 * persistence directory under the app's cache directory.
 *
 * @author Andre Suryana
 */
internal class DefaultPersistenceFactory(
    private val context: Context,
) : PersistenceFactory {

    override fun create(): MqttClientPersistence {
        val persistenceDir = File(context.cacheDir, DIR_NAME).apply {
            mkdirs() // Create directory if not exists
        }
        return MqttDefaultFilePersistence(persistenceDir.absolutePath)
    }

    companion object {
        private const val DIR_NAME = "skyroute-persistence"
    }
}
