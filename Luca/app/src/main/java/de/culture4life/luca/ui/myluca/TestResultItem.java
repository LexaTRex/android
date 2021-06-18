package de.culture4life.luca.ui.myluca;

import android.content.Context;

import de.culture4life.luca.R;
import de.culture4life.luca.testing.TestResult;

import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class TestResultItem extends MyLucaListItem {

    public static class TestProcedure {

        private String name;
        private String date;

        public TestProcedure(@NonNull String name, @NonNull String date) {
            this.name = name;
            this.date = date;
        }

        public String getName() {
            return name;
        }

        public String getDate() {
            return date;
        }

    }

    protected final TestResult testResult;

    public TestResultItem(@NonNull Context context, @NonNull TestResult testResult) {
        super(TYPE_TEST_RESULT);
        this.testResult = testResult;

        this.title = getReadableOutcome(context, testResult);
        this.barcode = generateQrCode(testResult.getEncodedData()).blockingGet();
        this.color = getColor(context, testResult);
        this.deleteButtonText = context.getString(R.string.test_delete_action);
        String time = context.getString(R.string.test_result_time, getReadableTime(getDateFormatFor(context, testResult), testResult.getResultTimestamp()));

        addTopContent(context.getString(R.string.test_type_of_test_label), getReadableTestType(context, testResult));
        addTopContent(context.getString(R.string.test_issued_at), time);

        addCollapsedContent(context.getString(R.string.test_issued_by), testResult.getLabName());
        addCollapsedContent(context.getString(R.string.test_lab_doctor_name), testResult.getLabDoctorName());
    }

    private static String getReadableTestType(@NonNull Context context, @NonNull TestResult testResult) {
        switch (testResult.getType()) {
            case TestResult.TYPE_FAST: {
                return context.getString(R.string.test_type_fast);
            }
            case TestResult.TYPE_PCR: {
                return context.getString(R.string.test_type_pcr);
            }
            default: {
                return context.getString(R.string.test_type_unknown);
            }
        }
    }

    private static String getReadableOutcome(@NonNull Context context, @NonNull TestResult testResult) {
        switch (testResult.getOutcome()) {
            case TestResult.OUTCOME_POSITIVE: {
                return context.getString(R.string.test_outcome_positive);
            }
            case TestResult.OUTCOME_NEGATIVE: {
                return context.getString(R.string.test_outcome_negative);
            }
            default: {
                return context.getString(R.string.test_outcome_unknown);
            }
        }
    }

    @ColorInt
    private static int getColor(@NonNull Context context, @NonNull TestResult testResult) {
        switch (testResult.getOutcome()) {
            case TestResult.OUTCOME_POSITIVE: {
                return ContextCompat.getColor(context, R.color.test_outcome_positive);
            }
            case TestResult.OUTCOME_NEGATIVE: {
                return ContextCompat.getColor(context, R.color.test_outcome_negative);
            }
            default: {
                return ContextCompat.getColor(context, R.color.test_outcome_unknown);
            }
        }
    }

    protected static SimpleDateFormat getDateFormatFor(@NonNull Context context, TestResult testResult) {
        if (testResult.getType() == TestResult.TYPE_VACCINATION || testResult.getType() == TestResult.TYPE_RECOVERY) {
            return new SimpleDateFormat("dd.MM.yyyy");
        } else {
            return new SimpleDateFormat(context.getString(R.string.time_format), Locale.GERMANY);
        }
    }

    public TestResult getTestResult() {
        return testResult;
    }

}
