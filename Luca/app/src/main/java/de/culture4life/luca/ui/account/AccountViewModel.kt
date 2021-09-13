package de.culture4life.luca.ui.account

import android.app.Application
import android.content.ActivityNotFoundException
import android.net.Uri
import android.text.TextUtils
import androidx.annotation.StringRes
import de.culture4life.luca.R
import de.culture4life.luca.checkin.CheckInData
import de.culture4life.luca.document.Document
import de.culture4life.luca.document.provider.ProvidedDocument
import de.culture4life.luca.document.provider.appointment.Appointment
import de.culture4life.luca.document.provider.eudcc.EudccDocument
import de.culture4life.luca.document.provider.opentestcheck.OpenTestCheckDocument
import de.culture4life.luca.history.CheckOutItem
import de.culture4life.luca.history.HistoryItem
import de.culture4life.luca.ui.BaseViewModel
import de.culture4life.luca.ui.myluca.TestResultItem
import de.culture4life.luca.util.TimeUtil
import de.culture4life.luca.util.getReadableTime
import dgca.verifier.app.decoder.model.RecoveryStatement
import dgca.verifier.app.decoder.model.Test
import dgca.verifier.app.decoder.model.Vaccination
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
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

    /*
        Data requests
     */

    fun exportTracingDataRequest(uri: Single<Uri>) {
        val content = Single.mergeArray(
            serializeContactData(),
            serializeCheckInData()
        ).collectInto(
            StringBuilder(),
            { builder, content -> builder.appendLine(content) }
        ).map { content ->
            """
                ${application.getString(R.string.data_request_tracing_title)}
                
                ${application.getString(R.string.data_request_tracing_content_prefix)}
                
                $content
                
                ${application.getString(R.string.data_request_tracing_content_suffix)}
            """.trimIndent()
        }

        export(uri, content)
    }

    fun exportDocumentsDataRequest(uri: Single<Uri>) {
        val content = serializeDocuments()
            .map { content ->
                """
                    ${application.getString(R.string.data_request_documents_title)}
                    
                    ${application.getString(R.string.data_request_documents_content_prefix)}
                    
                    $content
                    
                    ${application.getString(R.string.data_request_documents_content_suffix)}
                """.trimIndent()
            }

        export(uri, content)
    }

    private fun serializeContactData(): Single<String> {
        return application.registrationManager.orCreateRegistrationData
            .map { registrationData ->
                """
                    ${application.getString(R.string.data_request_contact_data_title)}
                    
                    ${printProperty(R.string.data_request_contact_data_name, registrationData.fullName)}
                    ${printProperty(R.string.data_request_contact_data_address, registrationData.address)}
                    ${printProperty(R.string.data_request_contact_data_phone_number, registrationData.phoneNumber)}
                    ${printProperty(R.string.data_request_contact_data_mail, registrationData.email)}
                    ${printProperty(R.string.data_request_storage_location, R.string.data_request_storage_location_local_and_server)}
                    ${printProperty(R.string.data_request_storage_period, R.string.data_request_storage_period_contact_data)}
                    
                """.trimIndent()
            }
    }

    private fun serializeCheckInData(): Single<String> {
        return application.checkInManager.archivedCheckInData
            .flatMapSingle { checkIn ->
                application.historyManager.items
                    .filter { it.relatedId.equals(checkIn.traceId) }
                    .filter { it.type == HistoryItem.TYPE_CHECK_OUT }
                    .firstElement()
                    .cast(CheckOutItem::class.java)
                    .flatMapSingle { checkOut -> serializeCheckInData(checkIn, checkOut) }
                    .switchIfEmpty(serializeCheckInData(checkIn, null))
            }
            .switchIfEmpty(Observable.just(application.getString(R.string.history_empty_title)))
            .collectInto(
                StringBuilder(),
                { builder, checkIn -> builder.append(checkIn) }
            )
            .map { checkIns ->
                """
                    ${application.getString(R.string.data_request_location_data_title)}
                    
                    $checkIns
                """.trimIndent()
            }
    }

    private fun serializeCheckInData(
        checkInData: CheckInData,
        checkOutItem: CheckOutItem?
    ): Single<String> {
        return Single.fromCallable {
            var builder = StringBuilder()
                .appendLine(R.string.data_request_check_in_name, checkInData.locationDisplayName)
                .appendLine(R.string.data_request_check_in_trace_id, checkInData.traceId)
                .appendLine(R.string.data_request_check_in_time, TimeUtil.getReadableTime(application, checkInData.timestamp))

            checkOutItem?.let {
                builder.appendLine(
                    R.string.data_request_check_in_duration,
                    TimeUtil.getReadableDurationWithPlural(it.timestamp - checkInData.timestamp, application).blockingGet()
                )
                if (!it.children.isNullOrEmpty()) {
                    builder.appendLine(R.string.data_request_check_in_children, it.children.joinToString())
                }
            }

            builder.appendLine(R.string.data_request_storage_location, R.string.data_request_storage_location_local_and_server)
                .appendLine(R.string.data_request_storage_period, R.string.data_request_storage_period_location_data)
                .appendLine()

            val accessedData = application.dataAccessManager
                .getPreviouslyAccessedTraceData(checkInData.traceId)
                .map {
                    if (TextUtils.isEmpty(it.healthDepartment.name)) {
                        application.getString(R.string.unknown)
                    } else {
                        it.healthDepartment.name
                    }
                }
                .distinct()
                .toList()
                .map { it.joinToString() }
                .onErrorReturnItem(application.getString(R.string.unknown))
                .blockingGet()

            if (!TextUtils.isEmpty(accessedData)) {
                builder.appendLine(R.string.data_request_check_in_data_accessed, accessedData)
            }

            builder.toString()
        }
    }

    private fun serializeDocuments(): Single<String> {
        return application.documentManager.orRestoreDocuments
            .flatMapSingle(this::serializeDocument)
            .switchIfEmpty(Observable.just(application.getString(R.string.my_luca_empty_title)))
            .collectInto(
                StringBuilder(),
                { builder, document -> builder.append(document) }
            )
            .map { documents ->
                """
                    ${application.getString(R.string.data_request_documents_title)}
                    
                    $documents
                """.trimIndent()
            }
    }

    private fun serializeDocument(document: Document): Single<String> {
        return application.documentManager.parseEncodedDocument(document.encodedData)
            .flatMap { document ->
                when (document) {
                    is EudccDocument -> serializeGreenCertificate(document)
                    is OpenTestCheckDocument -> serializeOpenTestCheckDocument(document)
                    is Appointment -> serializeAppointment(document)
                    else -> serializeGenericDocument(document)
                }
            }
            .onErrorReturnItem(document.javaClass.simpleName)
            .map { serializedDocument ->
                """
                    $serializedDocument
                    ${printProperty(R.string.data_request_document_data, document.encodedData)}
                    ${printProperty(R.string.data_request_storage_location, R.string.data_request_storage_location_local)}
                    ${printProperty(R.string.data_request_storage_period, R.string.data_request_storage_period_document_data)}
                    
                """.trimIndent()
            }
    }

    private fun serializeGenericDocument(document: ProvidedDocument): Single<String> {
        return Single.just(document.javaClass.simpleName)
    }

    private fun serializeGreenCertificate(document: EudccDocument): Single<String> {
        return Single.fromCallable {
            val certificate = document.getCertificate()
            var builder = StringBuilder()
                .appendLine(R.string.data_request_document_type, R.string.data_request_document_type_green_certificate)

            certificate?.vaccinations?.forEach {
                builder.appendLine(serializeGreenCertificateVaccination(it).blockingGet())
            }
            certificate?.recoveryStatements?.forEach {
                builder.appendLine(serializeGreenCertificateRecovery(it).blockingGet())
            }
            certificate?.tests?.forEach {
                builder.appendLine(serializeGreenCertificateTest(it).blockingGet())
            }

            builder.toString()
        }
    }

    private fun serializeGreenCertificateVaccination(vaccination: Vaccination): Single<String> {
        return Single.fromCallable {
            """
                ${printProperty(R.string.data_request_document_type, R.string.data_request_document_type_vaccination)}
                ${printProperty(R.string.data_request_green_cert_vaccination_tg, vaccination.disease)}
                ${printProperty(R.string.data_request_green_cert_vaccination_vp, vaccination.vaccine)}
                ${printProperty(R.string.data_request_green_cert_vaccination_mp, vaccination.medicinalProduct)}
                ${printProperty(R.string.data_request_green_cert_vaccination_ma, vaccination.manufacturer)}
                ${printProperty(R.string.data_request_green_cert_vaccination_dn, vaccination.doseNumber)}
                ${printProperty(R.string.data_request_green_cert_vaccination_sd, vaccination.totalSeriesOfDoses)}
                ${printProperty(R.string.data_request_green_cert_vaccination_dt, vaccination.dateOfVaccination)}
                ${printProperty(R.string.data_request_green_cert_vaccination_co, vaccination.countryOfVaccination)}
                ${printProperty(R.string.data_request_green_cert_vaccination_is, vaccination.certificateIssuer)}
                ${printProperty(R.string.data_request_green_cert_vaccination_ci, vaccination.certificateIdentifier)}
            """.trimIndent()
        }
    }

    private fun serializeGreenCertificateRecovery(recovery: RecoveryStatement): Single<String> {
        return Single.fromCallable {
            """
                ${printProperty(R.string.data_request_document_type, R.string.data_request_document_type_recovery)}
                ${printProperty(R.string.data_request_green_cert_recovery_tg, recovery.disease)}
                ${printProperty(R.string.data_request_green_cert_recovery_fr, recovery.dateOfFirstPositiveTest)}
                ${printProperty(R.string.data_request_green_cert_recovery_co, recovery.countryOfVaccination)}
                ${printProperty(R.string.data_request_green_cert_recovery_df, recovery.certificateValidFrom)}
                ${printProperty(R.string.data_request_green_cert_recovery_du, recovery.certificateValidUntil)}
                ${printProperty(R.string.data_request_green_cert_recovery_is, recovery.certificateIssuer)}
                ${printProperty(R.string.data_request_green_cert_recovery_ci, recovery.certificateIdentifier)}
            """.trimIndent()
        }
    }

    private fun serializeGreenCertificateTest(test: Test): Single<String> {
        return Single.fromCallable {
            """
                ${printProperty(R.string.data_request_document_type, R.string.data_request_document_type_test)}
                ${printProperty(R.string.data_request_green_cert_test_tg, test.disease)}
                ${printProperty(R.string.data_request_green_cert_test_tt, test.typeOfTest)}
                ${printProperty(R.string.data_request_green_cert_test_nm, test.testName)}
                ${printProperty(R.string.data_request_green_cert_test_ma, test.testNameAndManufacturer)}
                ${printProperty(R.string.data_request_green_cert_test_sc, test.dateTimeOfCollection)}
                ${printProperty(R.string.data_request_green_cert_test_dr, test.dateTimeOfTestResult)}
                ${printProperty(R.string.data_request_green_cert_test_tr, test.testResult)}
                ${printProperty(R.string.data_request_green_cert_test_tc, test.testingCentre)}
                ${printProperty(R.string.data_request_green_cert_test_co, test.countryOfVaccination)}
                ${printProperty(R.string.data_request_green_cert_test_is, test.certificateIssuer)}
                ${printProperty(R.string.data_request_green_cert_test_ci, test.certificateIdentifier)}
            """.trimIndent()
        }
    }

    private fun serializeOpenTestCheckDocument(document: OpenTestCheckDocument): Single<String> {
        return Single.fromCallable {
            """
                ${printProperty(R.string.data_request_document_type, R.string.data_request_document_type_test)}
                ${printProperty(R.string.data_request_test_type, TestResultItem.getReadableTestType(application, document.document))}
                ${printProperty(R.string.data_request_test_result, TestResultItem.getReadableOutcome(application, document.document))}
                ${printProperty(R.string.data_request_test_date, application.getReadableTime(document.document.resultTimestamp))}
                ${printProperty(R.string.data_request_test_provider, document.document.provider)}
                ${printProperty(R.string.data_request_test_lab, document.document.labName)}
                ${printProperty(R.string.data_request_test_doctor, document.document.labDoctorName)}
            """.trimIndent()
        }
    }

    private fun serializeAppointment(document: Appointment): Single<String> {
        return Single.fromCallable {
            """
                ${printProperty(R.string.data_request_document_type, R.string.data_request_document_type_appointment)}
                ${printProperty(R.string.data_request_appointment_type, document.type)}
                ${printProperty(R.string.data_request_appointment_date, application.getReadableTime(document.document.testingTimestamp))}
                ${printProperty(R.string.data_request_appointment_lab, document.lab)}
                ${printProperty(R.string.data_request_appointment_address, document.address)}
                ${printProperty(R.string.data_request_appointment_qr_code, document.qrCode)}
            """.trimIndent()
        }
    }

    private fun printProperty(
        @StringRes key: Int,
        @StringRes value: Int
    ): String {
        return printProperty(key, application.getString(value))
    }

    private fun printProperty(
        @StringRes key: Int,
        value: String?
    ): String {
        return "${application.getString(key)}: ${
            if (TextUtils.isEmpty(value)) application.getString(R.string.unknown)
            else value
        }"
    }

    private fun StringBuilder.appendLine(
        @StringRes key: Int,
        @StringRes value: Int
    ): StringBuilder {
        return this.appendLine(printProperty(key, value))
    }

    private fun StringBuilder.appendLine(
        @StringRes key: Int,
        value: String?
    ): StringBuilder {
        return this.append(printProperty(key, value))
    }

}