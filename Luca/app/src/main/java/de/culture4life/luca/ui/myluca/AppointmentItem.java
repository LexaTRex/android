package de.culture4life.luca.ui.myluca;

import android.content.Context;

import de.culture4life.luca.R;
import de.culture4life.luca.testing.TestResult;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class AppointmentItem extends TestResultItem {

    public AppointmentItem(@NonNull Context context, @NonNull TestResult testResult) {
        super(context, testResult);

        this.title = context.getString(R.string.appointment_title);
        this.color = ContextCompat.getColor(context, R.color.appointment);
        this.deleteButtonText = context.getString(R.string.appointment_delete_action);
        String time = context.getString(R.string.test_result_time, getReadableTime(getDateFormatFor(context, testResult), testResult.getResultTimestamp()));

        topContent.clear();
        addTopContent(testResult.getFirstName(), "");
        addTopContent(context.getString(R.string.test_issued_at), time);

        collapsedContent.clear();
        addCollapsedContent(context.getString(R.string.appointment_address), testResult.getLastName());
    }

}
