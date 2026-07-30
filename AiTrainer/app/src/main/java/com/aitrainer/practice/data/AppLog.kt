package com.aitrainer.practice.data

import android.util.Log

internal object AppLog {
    private const val TAG = "AiTrainer"

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(TAG, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
