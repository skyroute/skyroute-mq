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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.skyroute.example.R
import com.skyroute.example.SampleApplication.Companion.skyRoute
import com.skyroute.example.databinding.ActivitySettingsBinding

/**
 * @author Andre Suryana
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }

        binding.appBar.setNavigationOnClickListener {
            val fragment = supportFragmentManager.findFragmentById(R.id.settings) as? SettingsFragment
            if (fragment?.hasChanges() == true) {
                confirmChanges()
            } else {
                finish()
            }
        }

        binding.appBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_save -> {
                    confirmChanges()
                    true
                }
                else -> false
            }
        }
    }

    private fun confirmChanges() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_save_settings_title))
            .setMessage(getString(R.string.dialog_save_settings_msg))
            .setPositiveButton(getString(R.string.action_apply)) { _, _ -> applyChanges() }
            .setNegativeButton(getString(R.string.action_cancel)) { _, _ -> revertChanges() }
            .show()
    }

    private fun applyChanges() {
        SettingsUtils.readConfig(this)?.let { config ->
            skyRoute.applyConfig(config)
        }
        finish()
    }

    private fun revertChanges() {
        recreate()
    }
}
