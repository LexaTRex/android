package de.culture4life.luca.ui.myluca;

import android.content.Context;

import de.culture4life.luca.R;
import de.culture4life.luca.document.Document;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class GreenPassItem extends MyLucaListItem {

    protected final Document document;

    public GreenPassItem(@NonNull Context context, @NonNull Document document) {
        super(TYPE_GREEN_PASS);
        this.document = document;

        this.title = context.getString(R.string.em_green_pass);
        this.provider = getReadableProvider(context, document.getProvider());
        this.timestamp = document.getImportTimestamp();
        this.barcode = generateQrCode(document.getEncodedData()).blockingGet();
        this.color = ContextCompat.getColor(context, R.color.green_pass);
        this.imageResource = R.drawable.ic_dfb;
        this.deleteButtonText = context.getString(R.string.item_delete_action);

        String time = context.getString(R.string.document_result_time, getReadableDate(context, document.getResultTimestamp()));
        addTopContent(document.getLabDoctorName(), "");
        addTopContent(context.getString(R.string.em_green_pass_valid), time);

        addCollapsedContent(context.getString(R.string.document_issued_by), document.getLabName());
    }

    public Document getDocument() {
        return document;
    }

}
