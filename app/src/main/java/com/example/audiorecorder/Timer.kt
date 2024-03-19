package com.example.audiorecorder

import android.os.Handler
import android.os.Looper

class Timer(listener: OnTimerTickListener) {
    interface OnTimerTickListener {
        fun onTimerTick(duration: String)
    }


    private var handler: Handler? = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private  var duration: Long = 0
    private  var delay: Long = 100

    init {
        runnable = kotlinx.coroutines.Runnable {
            duration += delay
            handler?.postDelayed(runnable, delay)
            listener.onTimerTick(format())
        }
    }

    fun start() {
        handler?.postDelayed(runnable, delay)
    }
    fun pause() {
        handler?.removeCallbacks(runnable)
    }
    fun stop() {
        handler?.removeCallbacks(runnable)
        duration = 0
    }

    fun format(): String {
        val seconds = (duration / 1000) % 60
        val minutes = (duration / (1000 * 60)) % 60
        val hours = (duration / (1000 * 60 * 60))
        var formatted: String = if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
        return formatted
    }

}//end class