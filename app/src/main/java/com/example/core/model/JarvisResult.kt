package com.example.core.model

/**
 * Standard Result wrapper for Jarvis operations.
 */
sealed class JarvisResult<out T> {
    data class Success<out T>(val data: T) : JarvisResult<T>()
    data class Error(val message: String, val cause: Throwable? = null, val isKeyMissing: Boolean = false) : JarvisResult<Nothing>()
    object Loading : JarvisResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
}
