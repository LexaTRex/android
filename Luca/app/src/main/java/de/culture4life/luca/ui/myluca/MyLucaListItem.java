package de.culture4life.luca.ui.myluca;

import com.google.zxing.EncodeHintType;

import android.content.Context;
import android.graphics.Bitmap;

import de.culture4life.luca.R;
import de.culture4life.luca.testing.TestResult;

import net.glxn.qrgen.android.QRCode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import io.reactivex.rxjava3.core.Single;

public class MyLucaListItem {

    private final String testResultId;
    protected String title;
    protected String description;
    protected String time;
    protected Bitmap barcode;
    protected int color;
    protected long timestamp;
    private boolean isExpanded = false;
    private String type;
    private String testLabDoctorName;

    private MyLucaListItem(String testResultId) {
        this.testResultId = testResultId;
    }

    public static MyLucaListItem from(Context context, TestResult testResult) {
        MyLucaListItem item = new MyLucaListItem(testResult.getId());
        SimpleDateFormat readableDateFormat = new SimpleDateFormat(context.getString(R.string.venue_checked_in_time_format), Locale.GERMANY);

        String type;
        switch (testResult.getType()) {
            case TestResult.TYPE_FAST: {
                type = context.getString(R.string.test_type_fast);
                break;
            }
            case TestResult.TYPE_PCR: {
                type = context.getString(R.string.test_type_pcr);
                break;
            }
            default: {
                type = context.getString(R.string.test_type_unknown);
                break;
            }
        }

        String outcome;
        int color;
        switch (testResult.getOutcome()) {
            case TestResult.OUTCOME_POSITIVE: {
                outcome = context.getString(R.string.test_outcome_positive);
                color = ContextCompat.getColor(context, R.color.test_outcome_positive);
                break;
            }
            case TestResult.OUTCOME_NEGATIVE: {
                outcome = context.getString(R.string.test_outcome_negative);
                color = ContextCompat.getColor(context, R.color.test_outcome_negative);
                break;
            }
            default: {
                outcome = context.getString(R.string.test_outcome_unknown);
                color = ContextCompat.getColor(context, R.color.test_outcome_unknown);
                break;
            }
        }

        item.setTitle(outcome);
        item.setColor(color);
        item.setDescription(testResult.getLabName());
        item.setBarcode(generateQrCode(testResult.getEncodedData()).blockingGet());
        item.setTime(" " + context.getString(R.string.test_result_time, getReadableTime(readableDateFormat, testResult.getResultTimestamp())));
        item.setTimestamp(testResult.getImportTimestamp());
        item.setType(type);
        item.setTestLabDoctorName(testResult.getLabDoctorName());
        return item;
    }

    private static Single<Bitmap> generateQrCode(@NonNull String data) {
        return Single.fromCallable(() -> QRCode.from(data)
                .withSize(500, 500)
                .withHint(EncodeHintType.MARGIN, 0)
                .bitmap());
    }

    private static String getReadableTime(SimpleDateFormat readableDateFormat, long timestamp) {
        return readableDateFormat.format(new Date(timestamp));
    }

    public String getTestResultId() {
        return testResultId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Bitmap getBarcode() {
        return barcode;
    }

    public void setBarcode(Bitmap barcode) {
        this.barcode = barcode;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void toggleExpanded() {
        this.isExpanded = !isExpanded;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setTestLabDoctorName(String testLabDoctorName) {
        this.testLabDoctorName = testLabDoctorName;
    }

    public String getTestLabDoctorName() {
        return testLabDoctorName;
    }

}
