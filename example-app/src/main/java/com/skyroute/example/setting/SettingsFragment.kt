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
package com.skyroute.example.setting

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.skyroute.core.mqtt.MqttConfig
import com.skyroute.example.R

/**
 * @author Andre Suryana
 */
class SettingsFragment : PreferenceFragmentCompat() {

    private var originalConfig: MqttConfig? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        originalConfig = SettingsUtils.readConfig(requireContext())?.copy()
        setupTlsPreferences()
        setupClientIdPreferences()
    }

    fun hasChanges(): Boolean = SettingsUtils.readConfig(requireContext()) != originalConfig

    private fun setupTlsPreferences() {
        val tlsMode = findPreference<ListPreference>("tls_mode") ?: return

        val tlsPrefs = listOf(
            "tls_ca_cert_path",
            "tls_client_cert_path",
            "tls_client_key_path",
            "tls_client_key_password",
            "tls_skip_verify",
        ).associateWith { findPreference<Preference>(it) }

        val updateTlsFields: (String) -> Unit = { mode ->
            val isServerAuth = mode == "server_auth"
            val isMutualAuth = mode == "mutual_auth"

            tlsPrefs["tls_ca_cert_path"]?.isEnabled = isServerAuth || isMutualAuth
            tlsPrefs["tls_skip_verify"]?.isEnabled = isServerAuth || isMutualAuth

            listOf("tls_client_cert_path", "tls_client_key_path", "tls_client_key_password").forEach {
                tlsPrefs[it]?.isEnabled = isMutualAuth
            }
        }

        updateTlsFields(tlsMode.value ?: "none")
        tlsMode.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            updateTlsFields(newValue as String)
            true
        }
    }

    private fun setupClientIdPreferences() {
        val useClientId = findPreference<SwitchPreferenceCompat>("generate_client_id") ?: return

        val updateClientIdFields: (Boolean) -> Unit = { generateClientId ->
            val clientIdPref = findPreference<Preference>("client_id")
            val clientPrefixPref = findPreference<Preference>("client_prefix")

            clientIdPref?.isEnabled = !generateClientId
            clientPrefixPref?.isEnabled = generateClientId
        }

        updateClientIdFields(useClientId.isChecked)
        useClientId.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            updateClientIdFields(newValue as Boolean)
            true
        }
    }
}
