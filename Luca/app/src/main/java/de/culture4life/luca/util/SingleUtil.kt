package de.culture4life.luca.util

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleTransformer

object SingleUtil {

    @JvmStatic
    fun <T> retryWhen(
        throwableClass: Class<out Throwable>,
        maximumRetries: Int = 1
    ): SingleTransformer<T, T> {
        return SingleTransformer<T, T> { single ->
            single.retryWhen { errors ->
                errors.zipWith(
                    Flowable.range(0, maximumRetries + 1),
                    { error, attempt -> Pair(error, attempt) }
                ).flatMap { errorAndAttempt ->
                    val isExpectedError = errorAndAttempt.first.isCause(throwableClass)
                    val hasAttempts = errorAndAttempt.second < maximumRetries
                    if (isExpectedError && hasAttempts) {
                        Flowable.just(errorAndAttempt.first) // retry
                    } else {
                        Flowable.error(errorAndAttempt.first) // don't retry
                    }
                }
            }
        }
    }

}

fun <T> Single<T>.retryWhen(
    throwableClass: Class<out Throwable>,
    maximumRetries: Int = 1
): Single<T> {
    return this.compose(SingleUtil.retryWhen(throwableClass, maximumRetries))
}