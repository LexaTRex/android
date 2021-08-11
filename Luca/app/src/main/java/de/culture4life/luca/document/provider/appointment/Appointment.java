package de.culture4life.luca.document.provider.appointment;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.UUID;

import de.culture4life.luca.document.Document;
import de.culture4life.luca.document.provider.ProvidedDocument;

public class Appointment extends ProvidedDocument {

    protected String type;
    protected String lab;
    protected String address;
    protected String timestamp;
    protected String qrCode;

    public Appointment(@NonNull String url) {
        Uri uri = Uri.parse(url);
        type = uri.getQueryParameter("type");
        if (type == null) {
            throw new IllegalArgumentException("Invalid type parameter");
        }

        lab = uri.getQueryParameter("lab");
        if (lab == null) {
            throw new IllegalArgumentException("Invalid lab parameter");
        }

        address = uri.getQueryParameter("address");
        if (address == null) {
            throw new IllegalArgumentException("Invalid address parameter");
        }

        timestamp = uri.getQueryParameter("timestamp");
        if (timestamp == null) {
            throw new IllegalArgumentException("Invalid timestamp parameter");
        }

        qrCode = uri.getQueryParameter("qrCode");
        if (qrCode == null) {
            throw new IllegalArgumentException("Invalid qrCode parameter");
        }

        document.setType(Document.TYPE_APPOINTMENT);
        document.setFirstName(type);
        document.setLastName(address);
        document.setLabName(lab);
        document.setTestingTimestamp(Long.parseLong(timestamp));
        document.setResultTimestamp(document.getTestingTimestamp());
        document.setImportTimestamp(System.currentTimeMillis());
        document.setId(UUID.nameUUIDFromBytes(qrCode.getBytes()).toString());
        document.setProvider(lab);
        document.setEncodedData(url);
        document.setHashableEncodedData(qrCode);
    }

}
