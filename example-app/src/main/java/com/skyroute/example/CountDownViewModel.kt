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

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyroute.api.SkyRoute
import com.skyroute.api.Subscribe
import com.skyroute.api.ThreadMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A ViewModel class that manages the countdown functionality using SkyRouteMQ.
 *
 * @author Andre Suryana
 */
class CountDownViewModel : ViewModel() {

    private var countDownJob: Job? = null

    private val _countDown = MutableLiveData(-1)
    val countDown: LiveData<Int> = _countDown

    init {
        SkyRoute.getDefault().register(this)
    }

    override fun onCleared() {
        SkyRoute.getDefault().unregister(this)
        countDownJob?.cancel()
        Log.i(TAG, "ViewModel cleared and timer cancelled")
    }

    @Subscribe(topic = "countdown/start", threadMode = ThreadMode.BACKGROUND)
    fun subscribeToStartCountdown(seconds: Int) {
        countDownJob?.cancel() // Cancel previous countdown if it exists

        countDownJob = viewModelScope.launch {
            Log.i(TAG, "Starting countdown for $seconds seconds")
            for (timeLeft in seconds downTo 0) {
                _countDown.postValue(timeLeft)
                Log.i(TAG, "Countdown: $timeLeft seconds remaining")
                delay(1000L) // Delay for 1 second
            }

            Log.i(TAG, "Countdown finished!")
            SkyRoute.getDefault().publish("countdown/finished", true, 0, false)
            _countDown.postValue(-1)
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
