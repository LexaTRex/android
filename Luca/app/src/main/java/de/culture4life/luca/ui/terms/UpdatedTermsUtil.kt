package de.culture4life.luca.ui.terms

import de.culture4life.luca.LucaApplication
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class UpdatedTermsUtil {

    companion object {
        private const val TERMS_ACCEPTED_KEY = "terms_accepted_07_2021"

        private fun initializedPreferences(application: LucaApplication) =
            application.preferencesManager.initialize(application)
                .andThen(Single.just(application.preferencesManager))

        /**
         * Check if the terms and conditions have been accepted
         */
        fun areTermsAccepted(application: LucaApplication): Single<Boolean> {
            return initializedPreferences(application)
                .flatMap { it.restoreOrDefault(TERMS_ACCEPTED_KEY, false) }
        }

        /**
         * Call when user has accepted the terms and conditions
         */
        fun markTermsAsAccepted(application: LucaApplication): Completable {
            return initializedPreferences(application)
                .flatMapCompletable { it.persist(TERMS_ACCEPTED_KEY, true) }
                .onErrorComplete()
        }
    }

}
