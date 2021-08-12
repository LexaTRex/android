package de.culture4life.luca.document.provider

import de.culture4life.luca.document.DocumentVerificationException
import de.culture4life.luca.registration.RegistrationData
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.junit.Test

class DocumentProviderTest {

    private val provider = object : DocumentProvider<ProvidedDocument>() {
        override fun canParse(encodedData: String): Single<Boolean> {
            return Single.just(true)
        }

        override fun parse(encodedData: String): Single<ProvidedDocument> {
            throw NotImplementedError()
        }
    }

    private fun validateName(
        firstName: String,
        lastName: String,
        registeredFirstName: String,
        registeredLastName: String
    ): Completable {
        val document = object : ProvidedDocument() {}.apply {
            this.document.firstName = firstName
            this.document.lastName = lastName
        }
        val registration = RegistrationData().apply {
            this.firstName = registeredFirstName
            this.lastName = registeredLastName
        }
        return provider.validate(document, registration)
    }

    private fun validateSucceeds(
        firstName: String,
        lastName: String,
        registeredFirstName: String,
        registeredLastName: String
    ) {
        validateName(firstName, lastName, registeredFirstName, registeredLastName)
            .test().assertComplete()
    }

    private fun validateFails(
        firstName: String,
        lastName: String,
        registeredFirstName: String,
        registeredLastName: String
    ) {
        validateName(firstName, lastName, registeredFirstName, registeredLastName)
            .test().assertError(DocumentVerificationException::class.java)
    }

    @Test
    fun validate_sameName_succeeds() {
        validateSucceeds("Erika", "Mustermann", "Erika", "Mustermann")
    }

    @Test
    fun validate_nameWithTrimmableSpaces_succeeds() {
        validateSucceeds("Erika ", "Mustermann ", "Erika", "Mustermann")
    }

    @Test
    fun validate_nameWithCaseDifference_succeeds() {
        validateSucceeds("eRiKA ", "mUSterManN ", "ErIka", "MuStermann")
    }

    @Test
    fun validate_nameWithDifferentSpecialCharacters_succeeds() {
        validateSucceeds("Desireé", "Mustermann", "Desireè", "Mustermann")
    }

    @Test
    fun validate_nameWithAcademicTitle_succeeds() {
        validateSucceeds("Dr. Max", "Mustermann", "Max", "Mustermann")
    }

    @Test
    fun validate_nameWithAcademicTitles_succeeds() {
        validateSucceeds("Prof. Dr. Max", "Mustermann", "Dr. Max", "Mustermann")
    }

    @Test
    fun validate_differentName_fails() {
        validateFails("Erika", "Mustermann", "Max", "Mustermann")
    }

    @Test
    fun validate_mixedFirstAndLastName_fails() {
        validateFails("", "Erika Mustermann", "Erika", "Mustermann")
    }

    @Test
    fun validate_emptyName_fails() {
        validateFails("", "", "Max", "Mustermann")
    }

    @Test
    fun validate_emptyRegistrationName_fails() {
        validateFails("Erika", "Mustermann", "", "")
    }
}