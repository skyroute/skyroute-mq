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
package com.skyroute.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.skyroute.api.Subscribe
import com.skyroute.api.ThreadMode
import com.skyroute.example.SampleApplication.Companion.skyRoute
import com.skyroute.example.databinding.ActivitySampleBinding
import com.skyroute.example.setting.SettingsActivity
import com.skyroute.example.viewmodel.CountDownViewModel

/**
 * An example activity that demonstrates the usage of SkyRouteMQ.
 *
 * @author Andre Suryana
 */
class SampleActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySampleBinding
    private lateinit var viewModel: CountDownViewModel
    private var isFirstAppendLog = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySampleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupButton()

        skyRoute.register(this)

        viewModel = ViewModelProvider(this)[CountDownViewModel::class.java]
        viewModel.countDown.observe(this) { seconds ->
            if (seconds == 0) {
                appendLogs("Countdown finished!")
            } else if (seconds > 0) appendLogs("Countdown: $seconds seconds remaining")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (skyRoute.isRegistered(this)) {
            skyRoute.unregister(this)
        }
    }

    private fun setupToolbar() {
        binding.appBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupButton() {
        binding.btnPublish.setOnClickListener {
            val topic = binding.etTopic.text.toString()
            val message = binding.etMessage.text.toString()
            publishToTopic(topic, message)
        }
        binding.btnClear.setOnClickListener {
            binding.tvLogs.text = getString(R.string.placeholder_logs)
            binding.tvLogs.alpha = 0.5f
            isFirstAppendLog = true
        }
    }

    private fun publishToTopic(topic: String, message: String) {
        if (topic.isEmpty()) {
            Toast.makeText(this, "Topic should not be empty!", Toast.LENGTH_SHORT).show()
            return
        }
        if (message.isEmpty()) {
            Toast.makeText(this, "Message should not be empty!", Toast.LENGTH_SHORT).show()
            return
        }

        skyRoute.publish(topic, message, 0, false)
        appendLogs("Published message on topic '$topic': $message")
    }

    private fun appendLogs(message: String) {
        if (isFirstAppendLog) {
            binding.tvLogs.text = "" // Clear logs
            binding.tvLogs.alpha = 1f
            isFirstAppendLog = false
        }
        binding.tvLogs.append("$message\n")
    }

    @Subscribe(topic = "skyroute/abc", threadMode = ThreadMode.MAIN)
    fun subscribeToAbc(message: String) {
        appendLogs("Received message on topic 'topic/abc': $message")
    }

    @Subscribe(topic = "skyroute/+/temperature", threadMode = ThreadMode.MAIN)
    fun subscribeToTemperature(temperature: Int, wildcards: List<String>) {
        // Sample: Single-level wildcard subscription
        appendLogs("Received temperature for '${wildcards.joinToString()}': $temperature°C")
    }

    @Subscribe(topic = "skyroute/topic/#", qos = 1, threadMode = ThreadMode.MAIN)
    fun subscribeToXyz(message: String, wildcards: List<String>) {
        // Sample: Multi-level wildcard subscription
        appendLogs("Received message on topic '${wildcards.joinToString()}': $message")
    }
}
