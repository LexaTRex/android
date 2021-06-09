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
        this.description = testResult.getFirstName();
        this.color = ContextCompat.getColor(context, R.color.appointment);
    }

}
