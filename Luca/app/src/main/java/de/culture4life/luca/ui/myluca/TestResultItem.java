package de.culture4life.luca.ui.myluca;

import android.content.Context;

import de.culture4life.luca.R;
import de.culture4life.luca.document.Document;

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

    protected final Document document;

    public TestResultItem(@NonNull Context context, @NonNull Document document) {
        super(TYPE_TEST_RESULT);
        this.document = document;

        this.title = getReadableOutcome(context, document);
        this.provider = getReadableProvider(context, document.getProvider());
        this.barcode = generateQrCode(document.getEncodedData()).blockingGet();
        this.color = getColor(context, document);
        this.deleteButtonText = context.getString(R.string.delete_test_action);
        String time = context.getString(R.string.document_result_time, getReadableTime(getDateFormatFor(context, document), document.getResultTimestamp()));

        addTopContent(context.getString(R.string.document_type_of_document_label), getReadableTestType(context, document));
        addTopContent(context.getString(R.string.document_issued_at), time);

        addCollapsedContent(context.getString(R.string.document_issued_by), document.getLabName());
        addCollapsedContent(context.getString(R.string.document_lab_doctor_name), document.getLabDoctorName());
    }

    private static String getReadableTestType(@NonNull Context context, @NonNull Document document) {
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

    private static String getReadableOutcome(@NonNull Context context, @NonNull Document document) {
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
                return ContextCompat.getColor(context, R.color.document_outcome_positive);
            }
            case Document.OUTCOME_NEGATIVE: {
                return ContextCompat.getColor(context, R.color.document_outcome_negative);
            }
            default: {
                return ContextCompat.getColor(context, R.color.document_outcome_unknown);
            }
        }
    }

    protected static SimpleDateFormat getDateFormatFor(@NonNull Context context, Document document) {
        if (document.getType() == Document.TYPE_VACCINATION || document.getType() == Document.TYPE_RECOVERY) {
            return new SimpleDateFormat("dd.MM.yyyy");
        } else {
            return new SimpleDateFormat(context.getString(R.string.time_format), Locale.GERMANY);
        }
    }

    public Document getDocument() {
        return document;
    }

}
