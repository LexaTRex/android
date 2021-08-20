package de.culture4life.luca.util

import io.reactivex.rxjava3.core.Observable
import junit.framework.Assert.assertEquals
import org.junit.Test

class ObservableUtilTest {

    @Test
    fun retryWhen_expectedAndRecoverableError_retries() {
        var recoverFromError = false
        val firstSubscriptionFails = Observable.defer {
            if (recoverFromError) {
                Observable.just(true)
            } else {
                Observable.error(IllegalStateException())
            }
        }.doOnError { recoverFromError = true }

        firstSubscriptionFails
            .compose(ObservableUtil.retryWhen(IllegalStateException::class.java))
            .test()
            .assertResult(true)
    }

    @Test
    fun retryWhen_expectedButUnrecoverableError_stopsRetrying() {
        val retries = 3
        var subscriptions = 0

        Observable.error<Boolean>(IllegalStateException())
            .doOnSubscribe { subscriptions++ }
            .compose(ObservableUtil.retryWhen(IllegalStateException::class.java, retries))
            .test()
            .assertError(IllegalStateException::class.java)

        assertEquals(1 + retries, subscriptions);
    }

    @Test
    fun retryWhen_unexpectedError_doesNotRetry() {
        Observable.error<Boolean>(NullPointerException())
            .compose(ObservableUtil.retryWhen(IllegalStateException::class.java))
            .test()
            .assertError(NullPointerException::class.java)
    }

}