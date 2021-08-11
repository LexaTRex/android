package de.culture4life.luca.ui.account.healtdepartmentkey

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import de.culture4life.luca.network.pojo.DailyKeyPairIssuer
import de.culture4life.luca.ui.BaseViewModel
import de.culture4life.luca.ui.ViewError
import io.reactivex.rxjava3.core.Completable

class HealthDepartmentKeyViewModel(app: Application) : BaseViewModel(app) {

    val dailyKeyPairLiveData: MutableLiveData<DailyKeyPairIssuer> = MutableLiveData()
    val hasVerifiedDailyKeyPair: MutableLiveData<Boolean> = MutableLiveData()

    private var currentDailyKeyPairIssuer: DailyKeyPairIssuer? = null

    fun getDailyKeyPairAndVerify(): Completable {
        return Completable.defer {
            if (dailyKeyPairLiveData.value == null) {
                application.cryptoManager.dailyKeyPair
                    .doOnSuccess { setDailyKeyPairIssuer(it) }
                    .flatMapCompletable { application.cryptoManager.verifyDailyKeyPair(it) }
                    .doOnComplete { hasVerifiedDailyKeyPair.postValue(true) }
                    .doOnSubscribe { isLoading.postValue(true) }
                    .doFinally { isLoading.postValue(false) }
                    .doOnError {
                        hasVerifiedDailyKeyPair.postValue(false)
                        addError(
                            ViewError.Builder(application)
                                .withCause(it)
                                .removeWhenShown()
                                .build()
                        )
                    }
            } else {
                Completable.complete()
            }
        }
    }

    fun writeContentToUri(target: Uri): Completable {
        return currentDailyKeyPairIssuer?.let { dailyKeyPairIssuer ->
            application.cryptoManager.generateDailyKeyPairString(dailyKeyPairIssuer)
                .flatMapCompletable { writeTextCompletable(target, it) }
                .doOnError {
                    addError(
                        ViewError.Builder(application)
                            .withCause(it)
                            .removeWhenShown()
                            .build()
                    )
                }
        } ?: Completable.error(IllegalStateException("dailyKeyPair can not be null"))
    }

    private fun writeTextCompletable(target: Uri, string: StringBuilder): Completable {
        return Completable.fromAction {
            application.contentResolver?.openOutputStream(target)?.use {
                it.write(string.toString().toByteArray())
            }
        }
    }

    private fun setDailyKeyPairIssuer(dailyKeyPairIssuer: DailyKeyPairIssuer) {
        currentDailyKeyPairIssuer = dailyKeyPairIssuer
        dailyKeyPairLiveData.postValue(currentDailyKeyPairIssuer)
    }
}