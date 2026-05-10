package com.stagic.phantm

/**
 * Central logging facade. All log output is stripped in release builds.
 * Never use println, Log.d, or print directly — use this class only.
 */
expect object PhantmLogger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String)
}
