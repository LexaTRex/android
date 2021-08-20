package de.culture4life.luca.util

object ThrowableUtil {

    @JvmStatic
    fun isCause(throwableClass: Class<out Throwable>, throwable: Throwable?): Boolean {
        return throwableClass.isInstance(throwable) || (throwable != null && isCause(
            throwableClass,
            throwable.cause
        ))
    }

}

fun Throwable.isCause(throwableClass: Class<out Throwable>): Boolean {
    return ThrowableUtil.isCause(throwableClass, this)
}