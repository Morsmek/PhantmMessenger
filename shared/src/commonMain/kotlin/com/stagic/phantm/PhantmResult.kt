package com.stagic.phantm

/** Two-parameter result type used throughout Phantm. Avoids clash with kotlin.Result. */
sealed class PhantmResult<out V, out E> {
    data class Ok<out V>(val value: V) : PhantmResult<V, Nothing>()
    data class Err<out E>(val error: E) : PhantmResult<Nothing, E>()

    fun isOk(): Boolean = this is Ok
    fun isErr(): Boolean = this is Err

    val valueOrNull: V? get() = if (this is Ok) value else null
    val errorOrNull: E? get() = if (this is Err) error else null

    inline fun <T> map(transform: (V) -> T): PhantmResult<T, E> = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> this
    }

    inline fun <T> flatMap(transform: (V) -> PhantmResult<T, E>): PhantmResult<T, E> = when (this) {
        is Ok -> transform(value)
        is Err -> this
    }
}

fun <V> V.ok(): PhantmResult<V, Nothing> = PhantmResult.Ok(this)
fun <E> E.err(): PhantmResult<Nothing, E> = PhantmResult.Err(this)
