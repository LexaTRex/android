package de.culture4life.luca.ui.myluca

import android.content.Context
import androidx.core.content.ContextCompat
import de.culture4life.luca.R
import de.culture4life.luca.testing.TestResult
import de.culture4life.luca.util.TimeUtil
import java.text.SimpleDateFormat

/**
 * Item shown on UI for a vaccination certificate
 */
open class VaccinationItem(context: Context, testResult: TestResult) :
    TestResultItem(context, testResult) {

    init {
        title = context.getString(R.string.test_type_vaccination)
        deleteButtonText = context.getString(R.string.certificate_delete_action)
        topContent.clear()
        setDescriptionAndColor(context, testResult)
        val time = context.getString(R.string.test_result_time, getReadableTime(getDateFormatFor(context, testResult), testResult.resultTimestamp))
        addTopContent(context.getString(R.string.vaccination_date_label), time)

        collapsedContent.clear()
        addCollapsedContent(context.getString(R.string.test_issued_by), testResult.labName)
        for (testProcedure in getTestProcedures(context, testResult)) {
            addCollapsedContent(testProcedure.name, testProcedure.date)
        }
        testResult.dateOfBirth?.let {
            val date = MyLucaListItem.getReadableDate(context, it)
            addCollapsedContent(context.getString(R.string.birthday_label), date)
        }
    }

    private fun setDescriptionAndColor(context: Context, testResult: TestResult) {
        var descriptionLabel = context.getString(R.string.test_outcome_unknown)
        val firstProcedure = testResult.procedures.first()
        val procedureNumber = String.format("%d/%d", firstProcedure.doseNumber, firstProcedure.totalSeriesOfDoses)
        when (testResult.outcome) {
            TestResult.OUTCOME_PARTIALLY_IMMUNE -> {
                descriptionLabel = context.getString(R.string.test_outcome_partially_immune, procedureNumber)
                color = ContextCompat.getColor(context, R.color.test_outcome_partially_vaccinated)
            }
            TestResult.OUTCOME_FULLY_IMMUNE -> {
                val timeUntilValid = testResult.validityStartTimestamp - System.currentTimeMillis()
                if (timeUntilValid <= 0) {
                    descriptionLabel = context.getString(R.string.test_outcome_fully_vaccinated, procedureNumber)
                    color = ContextCompat.getColor(context, R.color.test_outcome_fully_vaccinated)
                } else {
                    val readableDuration = TimeUtil.getReadableDurationWithPlural(timeUntilValid, context).blockingGet()
                    descriptionLabel = context.getString(R.string.test_outcome_fully_vaccinated_in, readableDuration)
                    color = ContextCompat.getColor(context, R.color.test_outcome_fully_vaccinated_but_not_yet_valid)
                }
            }
            else -> {
                color = ContextCompat.getColor(context, R.color.test_outcome_unknown)
            }
        }
        addTopContent(descriptionLabel, "")
    }

    private fun getTestProcedures(context: Context, testResult: TestResult): List<TestProcedure> {
        val testProcedures = arrayListOf<TestProcedure>()
        testResult.procedures?.let { procedures ->
            val dateFormat = getDateFormatFor(context, testResult)
            for (procedure in procedures) {
                val label = context.getString(R.string.test_vaccination_procedure, procedure.doseNumber.toString())
                val description = getProcedureDescription(context, procedure, dateFormat)
                testProcedures.add(TestProcedure(label, description))
            }
        }
        return testProcedures
    }

    private fun getProcedureDescription(context: Context, procedure: TestResult.Procedure, dateFormat: SimpleDateFormat): String {
        val procedureName = context.getString(
            when (procedure.type) {
                TestResult.Procedure.Type.VACCINATION_COMIRNATY -> R.string.vaccine_comirnaty
                TestResult.Procedure.Type.VACCINATION_JANNSEN -> R.string.vaccine_jannsen
                TestResult.Procedure.Type.VACCINATION_MODERNA -> R.string.vaccine_moderna
                TestResult.Procedure.Type.VACCINATION_VAXZEVRIA -> R.string.vaccine_vaxzevria
                TestResult.Procedure.Type.VACCINATION_SPUTNIK_V -> R.string.vaccine_sputnik
                TestResult.Procedure.Type.RECOVERY -> R.string.procedure_recovery
                else -> R.string.unknown
            }
        )
        val time = context.getString(R.string.test_result_time, getReadableTime(dateFormat, procedure.timestamp))
        return time + "\n" + procedureName
    }
}