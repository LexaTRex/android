package de.culture4life.luca.ui.myluca

import android.content.Context
import androidx.core.content.ContextCompat
import de.culture4life.luca.R
import de.culture4life.luca.document.Document
import de.culture4life.luca.util.TimeUtil
import java.text.SimpleDateFormat

/**
 * Item shown on UI for a vaccination certificate
 */
open class VaccinationItem(context: Context, document: Document) :
    TestResultItem(context, document) {

    init {
        title = context.getString(R.string.document_type_vaccination)
        deleteButtonText = context.getString(R.string.certificate_delete_action)
        topContent.clear()
        setDescriptionAndColor(context, document)
        val time = context.getString(R.string.document_result_time, getReadableTime(getDateFormatFor(context, document), document.resultTimestamp))
        addTopContent(context.getString(R.string.vaccination_date_label), time)

        collapsedContent.clear()
        addCollapsedContent(context.getString(R.string.document_issued_by), document.labName)
        for (testProcedure in getTestProcedures(context, document)) {
            addCollapsedContent(testProcedure.name, testProcedure.date)
        }
        document.dateOfBirth?.let {
            val date = MyLucaListItem.getReadableDate(context, it)
            addCollapsedContent(context.getString(R.string.birthday_label), date)
        }
    }

    private fun setDescriptionAndColor(context: Context, document: Document) {
        var descriptionLabel = context.getString(R.string.document_outcome_unknown)
        val firstProcedure = document.procedures.first()
        val procedureNumber = String.format("%d/%d", firstProcedure.doseNumber, firstProcedure.totalSeriesOfDoses)
        when (document.outcome) {
            Document.OUTCOME_PARTIALLY_IMMUNE -> {
                descriptionLabel = context.getString(R.string.document_outcome_partially_immune, procedureNumber)
                color = ContextCompat.getColor(context, R.color.document_outcome_partially_vaccinated)
            }
            Document.OUTCOME_FULLY_IMMUNE -> {
                val timeUntilValid = document.validityStartTimestamp - System.currentTimeMillis()
                if (timeUntilValid <= 0) {
                    descriptionLabel = context.getString(R.string.document_outcome_fully_vaccinated, procedureNumber)
                    color = ContextCompat.getColor(context, R.color.document_outcome_fully_vaccinated)
                } else {
                    val readableDuration = TimeUtil.getReadableDurationWithPlural(timeUntilValid, context).blockingGet()
                    descriptionLabel = context.getString(R.string.document_outcome_fully_vaccinated_in, readableDuration)
                    color = ContextCompat.getColor(context, R.color.document_outcome_fully_vaccinated_but_not_yet_valid)
                }
            }
            else -> {
                color = ContextCompat.getColor(context, R.color.document_outcome_unknown)
            }
        }
        addTopContent(descriptionLabel, "")
    }

    private fun getTestProcedures(context: Context, document: Document): List<TestProcedure> {
        val testProcedures = arrayListOf<TestProcedure>()
        document.procedures?.let { procedures ->
            val dateFormat = getDateFormatFor(context, document)
            for (procedure in procedures) {
                val label = context.getString(R.string.document_vaccination_procedure, procedure.doseNumber.toString())
                val description = getProcedureDescription(context, procedure, dateFormat)
                testProcedures.add(TestProcedure(label, description))
            }
        }
        return testProcedures
    }

    private fun getProcedureDescription(context: Context, procedure: Document.Procedure, dateFormat: SimpleDateFormat): String {
        val procedureName = context.getString(
            when (procedure.type) {
                Document.Procedure.Type.VACCINATION_COMIRNATY -> R.string.vaccine_comirnaty
                Document.Procedure.Type.VACCINATION_JANNSEN -> R.string.vaccine_jannsen
                Document.Procedure.Type.VACCINATION_MODERNA -> R.string.vaccine_moderna
                Document.Procedure.Type.VACCINATION_VAXZEVRIA -> R.string.vaccine_vaxzevria
                Document.Procedure.Type.VACCINATION_SPUTNIK_V -> R.string.vaccine_sputnik
                Document.Procedure.Type.RECOVERY -> R.string.procedure_recovery
                else -> R.string.unknown
            }
        )
        val time = context.getString(R.string.document_result_time, getReadableTime(dateFormat, procedure.timestamp))
        return time + "\n" + procedureName
    }
}