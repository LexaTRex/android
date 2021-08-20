package de.culture4life.luca.ui.terms

import android.app.Application
import de.culture4life.luca.R
import de.culture4life.luca.ui.BaseViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber

/**
 * Simple ViewModel to access deletion from the BaseViewModel
 */
class UpdatedTermsViewModel(application: Application) : BaseViewModel(application) {

    /**
     * Delete the account data on backend and clear data locally. Restart the app from scratch when
     * successful, show error dialog when an error occurred.
     */
    fun deleteAccount() {
        modelDisposable.add(application.deleteAccount()
            .doOnSubscribe { updateAsSideEffect(isLoading, true) }
            .doFinally { updateAsSideEffect(isLoading, false) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.i("Account deleted")
                application.restart()
            }) { throwable: Throwable ->
                Timber.w("Unable to delete account: %s", throwable.toString())
                val viewError = createErrorBuilder(throwable)
                    .withTitle(R.string.error_request_failed_title)
                    .removeWhenShown()
                    .build()
                addError(viewError)
            })
    }

}
