package de.culture4life.luca.history;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import de.culture4life.luca.document.Document;

import androidx.annotation.NonNull;

public class DocumentImportedItem extends HistoryItem {

    @SerializedName("testResult")
    @Expose
    private Document document;

    public DocumentImportedItem() {
        super(HistoryItem.TYPE_TEST_RESULT_IMPORTED);
    }

    public DocumentImportedItem(@NonNull Document document) {
        this.document = document;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    @Override
    public String toString() {
        return "DocumentImportedItem{" +
                "document=" + document +
                ", type=" + type +
                ", relatedId='" + relatedId + '\'' +
                ", timestamp=" + timestamp +
                ", displayName='" + displayName + '\'' +
                "} " + super.toString();
    }

}
