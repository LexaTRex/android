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
        this.timestamp = testResult.getImportTimestamp();
        this.barcode = generateQrCode(testResult.getEncodedData()).blockingGet();
        this.color = ContextCompat.getColor(context, R.color.green_pass);
        this.imageResource = R.drawable.ic_dfb;
        this.deleteButtonText = context.getString(R.string.item_delete_action);

        String time = context.getString(R.string.test_result_time, getReadableDate(context, testResult.getResultTimestamp()));
        addTopContent(testResult.getLabDoctorName(), "");
        addTopContent(context.getString(R.string.em_green_pass_valid), time);

        addCollapsedContent(context.getString(R.string.test_issued_by), testResult.getLabName());
    }

    public TestResult getTestResult() {
        return testResult;
    }

}
