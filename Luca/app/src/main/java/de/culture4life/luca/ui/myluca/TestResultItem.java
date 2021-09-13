package de.culture4life.luca.ui.myluca;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import de.culture4life.luca.R;
import de.culture4life.luca.document.Document;
import de.culture4life.luca.util.TimeUtil;

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

    public TestResultItem(@NonNull Context context, @NonNull Document document) {
        super(TYPE_TEST_RESULT, document);

        this.title = getReadableOutcome(context, document);
        this.provider = getReadableProvider(context, document.getProvider());
        this.barcode = generateQrCode(document.getEncodedData()).blockingGet();
        this.color = getColor(context, document);
        this.deleteButtonText = context.getString(R.string.delete_test_action);
        this.imageResource = document.isVerified() ? R.drawable.ic_verified : 0;

        String duration = TimeUtil.getReadableDurationWithPlural(System.currentTimeMillis() - document.getResultTimestamp(), context).blockingGet();
        addTopContent(context.getString(R.string.document_type_of_document_label), getReadableTestType(context, document));
        addTopContent(context.getString(R.string.document_created_before), duration);

        String time = context.getString(R.string.document_result_time, TimeUtil.getReadableTime(context, document.getResultTimestamp()));
        addCollapsedContent(context.getString(R.string.document_issued_by), time + "\n" + document.getLabName());
        addCollapsedContent(context.getString(R.string.document_lab_issuer), document.getLabDoctorName());
    }

    public static String getReadableTestType(@NonNull Context context, @NonNull Document document) {
        switch (document.getType()) {
            case Document.TYPE_FAST: {
                return context.getString(R.string.document_type_fast);
            }
            case Document.TYPE_PCR: {
                return context.getString(R.string.document_type_pcr);
            }
            default: {
                return context.getString(R.string.document_type_unknown);
            }
        }
    }

    public static String getReadableOutcome(@NonNull Context context, @NonNull Document document) {
        switch (document.getOutcome()) {
            case Document.OUTCOME_POSITIVE: {
                return context.getString(R.string.document_outcome_positive);
            }
            case Document.OUTCOME_NEGATIVE: {
                return context.getString(R.string.document_outcome_negative);
            }
            default: {
                return context.getString(R.string.document_outcome_unknown);
            }
        }
    }

    @ColorInt
    private static int getColor(@NonNull Context context, @NonNull Document document) {
        switch (document.getOutcome()) {
            case Document.OUTCOME_POSITIVE: {
                if (document.isValidRecovery()) {
                    return ContextCompat.getColor(context, R.color.document_outcome_partially_vaccinated);
                } else {
                    return ContextCompat.getColor(context, R.color.document_outcome_positive);
                }
            }
            case Document.OUTCOME_NEGATIVE: {
                return ContextCompat.getColor(context, R.color.document_outcome_negative);
            }
            default: {
                return ContextCompat.getColor(context, R.color.document_outcome_unknown);
            }
        }
    }

}
