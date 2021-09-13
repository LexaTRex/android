package de.culture4life.luca.document.provider

import de.culture4life.luca.document.DocumentVerificationException
import de.culture4life.luca.children.Child
import de.culture4life.luca.registration.Person
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.joda.time.DateTime
import org.junit.Test

class DocumentProviderTest {

    private val provider = object : DocumentProvider<ProvidedDocument>() {
        var document: ProvidedDocument? = null

        override fun canParse(encodedData: String): Single<Boolean> {
            return Single.just(true)
        }

        override fun parse(encodedData: String): Single<ProvidedDocument> {
            return Single.just(document)
        }
    }

    private fun setDocumentForName(firstName: String, lastName: String) {
        provider.document = object : ProvidedDocument() {}.apply {
            document.firstName = firstName
            document.lastName = lastName
            document.dateOfBirth = DateTime().minusYears(5).millis
        }
    }

    private fun validateName(
        firstName: String,
        lastName: String,
        registeredFirstName: String,
        registeredLastName: String
    ): Completable {
        setDocumentForName(firstName, lastName)
        val person = Person(registeredFirstName, registeredLastName)
        return provider.validate(provider.document!!, person)
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

    @Test
    fun verifyParseAndValidate_forAdult_completes() {
        setDocumentForName("Adult", "Name")
        provider.verifyParseAndValidate("", Person("Adult", "Name"), listOf(Child("Child", "Name")))
            .test().assertComplete()
    }

    @Test
    fun verifyParseAndValidate_forChild_completes() {
        setDocumentForName("Child", "Name")
        provider.verifyParseAndValidate("", Person("Adult", "Name"), listOf(Child("Child", "Name")))
            .test().assertComplete()
    }

    @Test
    fun verifyParseAndValidate_forSecondChild_completes() {
        setDocumentForName("Child2", "Name2")
        provider.verifyParseAndValidate(
            "",
            Person("Adult", "Name"),
            listOf(Child("Child", "Name"), Child("Child2", "Name2"))
        )
            .test().assertComplete()
    }

    @Test
    fun verifyParseAndValidate_forChildWithLastNameOfAdult_completes() {
        setDocumentForName("Child", "AdultLastName")
        provider.verifyParseAndValidate(
            "",
            Person("Adult", "AdultLastName"),
            listOf(Child("Child", "AnotherLastName"))
        )
    }

    private fun validateTime(
        time: Long,
    ): Completable {
        val document = object : ProvidedDocument() {}.apply {
            document.firstName = "firstName"
            document.lastName = "lastName"
            document.testingTimestamp = time
        }
        return provider.validate(document, Person(document.document.firstName, document.document.lastName))
    }

    @Test
    fun validateTime_inThePast_succeeds() {
        validateTime(System.currentTimeMillis() - 1000)
            .test().assertComplete()
    }

    @Test
    fun verifyParseAndValidate_forUnknownName_fails() {
        setDocumentForName("Unknown", "Person")
        provider.verifyParseAndValidate("", Person("Adult", "Name"), listOf(Child("Child", "Name")))
            .test().assertError(DocumentVerificationException::class.java)
    }

    @Test
    fun verifyParseAndValidate_forEmptyName_fails() {
        setDocumentForName("Any", "Person")
        provider.verifyParseAndValidate("", Person("", ""), listOf())
            .test().assertError(DocumentVerificationException::class.java)
    }

    @Test
    fun verifyParseAndValidate_forTooOldChild_fails() {
        provider.document = object : ProvidedDocument() {}.apply {
            document.firstName = "Any"
            document.lastName = "Person"
            document.dateOfBirth = DateTime().minusYears(15).millis
        }
        provider.verifyParseAndValidate("", Person("", ""), listOf(Child("Any", "Person")))
            .test().assertError(DocumentVerificationException::class.java)
    }

    @Test(expected = DocumentVerificationException::class)
    fun validateChildAge_over14years_fails() {
        DocumentProvider.validateChildAge(DateTime().minusYears(15).millis)
    }

    @Test(expected = DocumentVerificationException::class)
    fun validateChildAge_inFuture_fails() {
        DocumentProvider.validateChildAge(DateTime().plusDays(1).millis)
    }

    @Test
    fun validateChildAge_inAcceptedRange_succeeds() {
        DocumentProvider.validateChildAge(DateTime().minusDays(1).millis)
        DocumentProvider.validateChildAge(DateTime().minusYears(13).minusMonths(11).millis)
    }

    @Test
    fun validateTime_inTheFuture_fails() {
        validateTime(System.currentTimeMillis() + 1000)
            .test().assertError(DocumentVerificationException::class.java)
    }
}