package com.stagic.phantm

import android.util.Log

actual object PhantmLogger {
    actual fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        if (BuildConfig.DEBUG) Log.e(tag, message, throwable)
    }

    actual fun w(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.w(tag, message)
    }
}
