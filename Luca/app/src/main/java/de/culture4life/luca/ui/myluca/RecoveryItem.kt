package de.culture4life.luca.ui.myluca

import android.content.Context
import androidx.core.content.ContextCompat
import de.culture4life.luca.R
import de.culture4life.luca.document.Document
import de.culture4life.luca.util.TimeUtil
import de.culture4life.luca.util.getReadableDate

/**
 * Item shown on UI for a Recovery certificate
 */
class RecoveryItem(context: Context, document: Document) : VaccinationItem(context, document) {

    init {
        setTitleAndColor(context, document)
        deleteButtonText = context.getString(R.string.delete_certificate_action)

        topContent.clear()
        val createdBefore = TimeUtil.getReadableDurationWithPlural(
            System.currentTimeMillis() - document.testingTimestamp,
            context
        ).blockingGet()
        addTopContent(context.getString(R.string.document_created_before), createdBefore)

        collapsedContent.clear()
        val time = context.getReadableDate(document.testingTimestamp)
        addCollapsedContent(
            context.getString(R.string.document_issued_by),
            "$time\n${document.labName}"
        )
        val validUntil = context.getReadableDate(document.expirationTimestamp)
        addCollapsedContent(context.getString(R.string.document_valid_until), validUntil)
        val date = context.getReadableDate(document.dateOfBirth)
        addCollapsedContent(context.getString(R.string.birthday_label), date)
    }

    private fun setTitleAndColor(context: Context, document: Document) {
        when (document.outcome) {
            Document.OUTCOME_PARTIALLY_IMMUNE -> {
                val firstProcedure = document.procedures.first()
                val procedureNumber = String.format(
                    "%d/%d",
                    firstProcedure.doseNumber,
                    firstProcedure.totalSeriesOfDoses
                )
                title =
                    context.getString(R.string.document_outcome_partially_immune, procedureNumber)
                color =
                    ContextCompat.getColor(context, R.color.document_outcome_partially_vaccinated)
            }
            Document.OUTCOME_FULLY_IMMUNE -> {
                val timeUntilValid = document.validityStartTimestamp - System.currentTimeMillis()
                if (timeUntilValid <= 0) {
                    title = context.getString(R.string.document_outcome_fully_recovered)
                    color =
                        ContextCompat.getColor(context, R.color.document_outcome_fully_recovered)
                } else {
                    val readableDuration =
                        TimeUtil.getReadableDurationWithPlural(timeUntilValid, context)
                            .blockingGet()
                    title = context.getString(
                        R.string.document_outcome_fully_recovered_in,
                        readableDuration
                    )
                    color = ContextCompat.getColor(
                        context,
                        R.color.document_outcome_fully_vaccinated_but_not_yet_valid
                    )
                }
            }
            else -> {
                title = context.getString(R.string.document_outcome_unknown)
                color = ContextCompat.getColor(context, R.color.document_outcome_unknown)
            }
        }
    }

}