package com.stagic.phantm

import platform.Foundation.NSLog

actual object PhantmLogger {
    actual fun d(tag: String, message: String) {
        // No-op in release; debug logging only during development
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        // No-op in release
    }

    actual fun w(tag: String, message: String) {
        // No-op in release
    }
}
