package com.skyroute.example

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.skyroute.api.SkyRoute
import com.skyroute.api.Subscribe
import com.skyroute.api.ThreadMode
import com.skyroute.example.databinding.ActivitySkyRouteSampleBinding
import com.skyroute.example.model.RandomNames

class SkyRouteSampleActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkyRouteSampleBinding
    private lateinit var viewModel: MainViewModel
    private var isFirstAppendLog = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySkyRouteSampleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SkyRoute.getDefault().register(this)
        setupPublishButton()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.countDown.observe(this) { seconds ->
            if (seconds == 0) appendLogs("Countdown finished!")
            else if (seconds > 0) appendLogs("Countdown: $seconds seconds remaining")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (SkyRoute.getDefault().isRegistered(this)) {
            SkyRoute.getDefault().unregister(this)
        }
    }

    private fun setupPublishButton() {
        binding.btnPublish.setOnClickListener {
            val topic = binding.etTopic.text.toString()
            val message = binding.etMessage.text.toString()
            publishToTopic(topic, message)
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

        SkyRoute.getDefault().publish(topic, message, 0, false)
        appendLogs("Published message on topic '$topic': $message")
    }

    private fun appendLogs(message: String) {
        if (isFirstAppendLog) {
            clearLogs()
            isFirstAppendLog = false
        }
        binding.tvLogs.append("$message\n")
    }

    private fun clearLogs() {
        binding.tvLogs.text = ""
    }

    @Subscribe(topic = "test/abc", threadMode = ThreadMode.MAIN)
    fun subscribeToAbc(message: String) {
        appendLogs("Received message on topic 'topic/abc': $message")
    }

    @Subscribe(topic = "xyz/+", threadMode = ThreadMode.MAIN)
    fun subscribeToXyz(message: String) {
        appendLogs("Received message on topic 'topic/xyz': $message")
    }

    @Subscribe(topic = "test/random-names", threadMode = ThreadMode.MAIN)
    fun subscribeToNames(data: RandomNames) {
        appendLogs("Received message on topic 'test/random-names': $data")
    }
}