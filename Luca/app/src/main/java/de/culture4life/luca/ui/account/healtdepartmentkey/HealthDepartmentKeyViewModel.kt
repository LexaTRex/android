package de.culture4life.luca.ui.account.healtdepartmentkey

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import de.culture4life.luca.network.pojo.DailyKeyPairIssuer
import de.culture4life.luca.ui.BaseViewModel
import de.culture4life.luca.ui.ViewError
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

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

    fun exportDailyKey(uri: Single<Uri>) {
        val content = Single.fromCallable { currentDailyKeyPairIssuer!! }
            .map { dailyKeyPairIssuer ->
                StringBuilder()
                    .append("Public key:\n")
                    .append(dailyKeyPairIssuer.dailyKeyPair.publicKey + "\n\n")
                    .append("Signature:\n")
                    .append(dailyKeyPairIssuer.dailyKeyPair.signature + "\n\n")
                    .append("Issuer:\n")
                    .append(dailyKeyPairIssuer.issuer.name + "\n\n")
                    .append("Issuer signing key:\n")
                    .append(dailyKeyPairIssuer.issuer.publicHDSKP)
                    .toString()
            }

        export(uri, content)
    }

    private fun setDailyKeyPairIssuer(dailyKeyPairIssuer: DailyKeyPairIssuer) {
        currentDailyKeyPairIssuer = dailyKeyPairIssuer
        dailyKeyPairLiveData.postValue(currentDailyKeyPairIssuer)
    }

}