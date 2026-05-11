package com.stagic.phantm

actual object PhantmLogger {
    actual fun d(tag: String, message: String) {
        System.err.println("D/$tag: $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        System.err.println("E/$tag: $message${throwable?.let { " — ${it.message}" } ?: ""}")
    }

    actual fun w(tag: String, message: String) {
        System.err.println("W/$tag: $message")
    }
}
