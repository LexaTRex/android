package de.culture4life.luca.ui.account

import android.app.Application
import android.content.ActivityNotFoundException
import de.culture4life.luca.R
import de.culture4life.luca.ui.BaseViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber

class AccountViewModel(application: Application) : BaseViewModel(application) {

    fun openHealthDepartmentKeyView() {
        navigationController.navigate(R.id.action_accountFragment_to_healthDepartmentKeyFragment)
    }

    fun requestSupportMail() {
        try {
            application.openSupportMailIntent()
        } catch (exception: ActivityNotFoundException) {
            val viewError = createErrorBuilder(exception)
                .withTitle(R.string.menu_support_error_title)
                .withDescription(R.string.menu_support_error_description)
                .removeWhenShown()
                .build()
            addError(viewError)
        }
    }

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