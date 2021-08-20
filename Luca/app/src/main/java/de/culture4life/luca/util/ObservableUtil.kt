package de.culture4life.luca.util

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableTransformer

object ObservableUtil {

    @JvmStatic
    fun <T> retryWhen(
        throwableClass: Class<out Throwable>,
        maximumRetries: Int = 1
    ): ObservableTransformer<T, T> {
        return ObservableTransformer<T, T> { observable ->
            observable.retryWhen { errors ->
                errors.zipWith(
                    Observable.range(0, maximumRetries + 1),
                    { error, attempt -> Pair(error, attempt) }
                ).flatMap { errorAndAttempt ->
                    val isExpectedError = errorAndAttempt.first.isCause(throwableClass)
                    val hasAttempts = errorAndAttempt.second < maximumRetries
                    if (isExpectedError && hasAttempts) {
                        Observable.just(errorAndAttempt.first) // retry
                    } else {
                        Observable.error(errorAndAttempt.first) // don't retry
                    }
                }
            }
        }
    }

}

fun <T> Observable<T>.retryWhen(
    throwableClass: Class<out Throwable>,
    maximumRetries: Int = 1
): Observable<T> {
    return this.compose(ObservableUtil.retryWhen(throwableClass, maximumRetries))
}