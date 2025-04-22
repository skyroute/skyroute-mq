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

class MainViewModel : ViewModel() {

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

    @Subscribe(topic = "countdown/start", threadMode = ThreadMode.POSTING)
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
            SkyRoute.getDefault().publish("countdown/finish", "Countdown finished!", 0, false)
            _countDown.postValue(-1)
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}