package de.culture4life.luca.ui.myluca

import android.content.Context
import android.util.Pair
import androidx.core.content.ContextCompat
import de.culture4life.luca.R
import de.culture4life.luca.testing.TestResult
import de.culture4life.luca.util.TimeUtil
import org.joda.time.DateTime

/**
 * Item shown on UI for a Recovery certificate
 */
class RecoveryItem(context: Context, testResult: TestResult) :
    VaccinationItem(context, testResult) {

    init {
        setTitleAndColor(context, testResult)
        deleteButtonText = context.getString(R.string.certificate_delete_action)
        val time = getReadableTime(context, testResult.expirationTimestamp)

        topContent.clear()
        addTopContent(context.getString(R.string.test_valid_until), time)

        collapsedContent.clear()
        addCollapsedContent(context.getString(R.string.test_issued_by), testResult.labName)
        testResult.dateOfBirth?.let {
            val date = MyLucaListItem.getReadableDate(context, it)
            addCollapsedContent(context.getString(R.string.birthday_label), date)
        }
    }

    private fun setTitleAndColor(context: Context, testResult: TestResult) {
        when (testResult.outcome) {
            TestResult.OUTCOME_PARTIALLY_IMMUNE -> {
                title = context.getString(R.string.test_outcome_partially_immune)
                color = ContextCompat.getColor(context, R.color.test_outcome_partially_vaccinated)
            }
            TestResult.OUTCOME_FULLY_IMMUNE -> {
                val timeUntilValid = testResult.validityStartTimestamp - System.currentTimeMillis()
                if (timeUntilValid <= 0) {
                    title = context.getString(R.string.test_outcome_fully_recovered)
                    color = ContextCompat.getColor(context, R.color.test_outcome_fully_recovered)
                } else {
                    val readableDuration = TimeUtil.getReadableDurationWithPlural(timeUntilValid, context).blockingGet()
                    title = context.getString(R.string.test_outcome_fully_recovered_in, readableDuration)
                    color = ContextCompat.getColor(context, R.color.test_outcome_fully_vaccinated_but_not_yet_valid)
                }
            }
            else -> {
                title = context.getString(R.string.test_outcome_unknown)
                color = ContextCompat.getColor(context, R.color.test_outcome_unknown)
            }
        }
    }
}