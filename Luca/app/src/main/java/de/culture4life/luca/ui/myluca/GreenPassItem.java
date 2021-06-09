package de.culture4life.luca.ui.myluca;

import android.content.Context;

import de.culture4life.luca.R;
import de.culture4life.luca.testing.TestResult;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class GreenPassItem extends MyLucaListItem {

    protected final TestResult testResult;

    public GreenPassItem(@NonNull Context context, @NonNull TestResult testResult) {
        super(TYPE_GREEN_PASS);
        this.testResult = testResult;

        this.title = context.getString(R.string.em_green_pass);
        this.description = testResult.getLabDoctorName();
        this.time = context.getString(R.string.test_result_time, getReadableTime(context, testResult.getResultTimestamp()));
        this.timestamp = testResult.getImportTimestamp();
        this.barcode = generateQrCode(testResult.getEncodedData()).blockingGet();
        this.color = ContextCompat.getColor(context, R.color.green_pass);
        this.imageResource = R.drawable.ic_dfb;
    }

    public TestResult getTestResult() {
        return testResult;
    }

}
