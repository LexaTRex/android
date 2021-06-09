package de.culture4life.luca.ui.myluca;

import android.content.Context;

import de.culture4life.luca.R;
import de.culture4life.luca.testing.TestResult;
import de.culture4life.luca.util.TimeUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class TestResultItem extends MyLucaListItem {

    protected final TestResult testResult;

    protected String testType;
    protected List<TestProcedure> testProcedures;

    public TestResultItem(@NonNull Context context, @NonNull TestResult testResult) {
        super(TYPE_TEST_RESULT);
        this.testResult = testResult;

        this.title = getReadableOutcome(context, testResult);
        this.description = testResult.getLabName();
        this.time = context.getString(R.string.test_result_time, getReadableTime(context, testResult.getResultTimestamp()));
        this.timestamp = testResult.getImportTimestamp();
        this.barcode = generateQrCode(testResult.getEncodedData()).blockingGet();
        this.color = getColor(context, testResult);
        this.testType = getReadableTestType(context, testResult);
        this.testProcedures = getTestProcedures(context, testResult);
    }

    private static String getReadableTestType(@NonNull Context context, @NonNull TestResult testResult) {
        switch (testResult.getType()) {
            case TestResult.TYPE_FAST: {
                return context.getString(R.string.test_type_fast);
            }
            case TestResult.TYPE_PCR: {
                return context.getString(R.string.test_type_pcr);
            }
            case TestResult.TYPE_VACCINATION: {
                return context.getString(R.string.test_type_vaccination);
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
            case TestResult.OUTCOME_PARTIALLY_VACCINATED: {
                return context.getString(R.string.test_outcome_partially_vaccinated);
            }
            case TestResult.OUTCOME_FULLY_VACCINATED: {
                long timeUntilValid = testResult.getValidityStartTimestamp() - System.currentTimeMillis();
                if (timeUntilValid < 0) {
                    return context.getString(R.string.test_outcome_fully_vaccinated);
                } else {
                    String readableDuration = TimeUtil.getReadableDurationWithPlural(timeUntilValid, context).blockingGet();
                    return context.getString(R.string.test_outcome_fully_vaccinated_in, readableDuration);
                }
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
            case TestResult.OUTCOME_PARTIALLY_VACCINATED: {
                return ContextCompat.getColor(context, R.color.test_outcome_partially_vaccinated);
            }
            case TestResult.OUTCOME_FULLY_VACCINATED: {
                long timeUntilValid = testResult.getValidityStartTimestamp() - System.currentTimeMillis();
                if (timeUntilValid < 0) {
                    return ContextCompat.getColor(context, R.color.test_outcome_fully_vaccinated);
                } else {
                    return ContextCompat.getColor(context, R.color.test_outcome_fully_vaccinated_but_not_yet_valid);
                }
            }
            default: {
                return ContextCompat.getColor(context, R.color.test_outcome_unknown);
            }
        }
    }

    private static List<TestProcedure> getTestProcedures(@NonNull Context context, @NonNull TestResult testResult) {
        ArrayList<TestProcedure> testProcedures = new ArrayList<>();
        if (testResult.getProcedures() == null) {
            return testProcedures;
        }
        SimpleDateFormat dateFormat = getDateFormat(context);
        for (int i = 0; i < testResult.getProcedures().size(); i++) {
            TestResult.Procedure procedure = testResult.getProcedures().get(i);
            String description = getProcedureDescription(context, testResult.getProcedures().size() - i, procedure);
            String time = context.getString(R.string.test_result_time, getReadableTime(dateFormat, procedure.getTimestamp()));
            if (description != null) {
                testProcedures.add(new TestProcedure(description, time));
            }
        }
        return testProcedures;
    }

    @Nullable
    private static String getProcedureDescription(@NonNull Context context, int position, TestResult.Procedure procedure) {
        String vaccine;
        switch (procedure.getName()) {
            case VACCINATION_COMIRNATY:
                vaccine = context.getString(R.string.vaccine_comirnaty);
                break;
            case VACCINATION_JANNSEN:
                vaccine = context.getString(R.string.vaccine_jannsen);
                break;
            case VACCINATION_MODERNA:
                vaccine = context.getString(R.string.vaccine_moderna);
                break;
            case VACCINATION_VAXZEVRIA:
                vaccine = context.getString(R.string.vaccine_vaxzevria);
                break;
            default:
                return null;
        }
        return context.getString(R.string.test_vaccination_procedure, String.valueOf(position), vaccine);
    }

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

    public TestResult getTestResult() {
        return testResult;
    }

    public String getTestType() {
        return testType;
    }

    public List<TestProcedure> getTestProcedures() {
        return testProcedures;
    }

}
