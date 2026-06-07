package cz.obchodnik.core

sealed interface Result<out T> {
    data object Loading : Result<Nothing>
    data class Success<T>(val data: T, val notice: String? = null) : Result<T>
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>
}
