package de.culture4life.luca.util

import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ThrowableUtil {

    @JvmStatic
    fun isCause(throwableClass: Class<out Throwable>, throwable: Throwable?): Boolean {
        return throwableClass.isInstance(throwable) || (throwable != null && isCause(
            throwableClass,
            throwable.cause
        ))
    }

    @JvmStatic
    fun isNetworkError(throwable: Throwable?): Boolean {
        return when {
            isCause(UnknownHostException::class.java, throwable) ||
                    isCause(ConnectException::class.java, throwable) ||
                    isCause(SocketTimeoutException::class.java, throwable) ||
                    isCause(SocketException::class.java, throwable) -> true
            else -> false
        }
    }
}

fun Throwable.isCause(throwableClass: Class<out Throwable>): Boolean {
    return ThrowableUtil.isCause(throwableClass, this)
}