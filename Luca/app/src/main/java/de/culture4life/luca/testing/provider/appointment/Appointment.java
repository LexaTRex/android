package de.culture4life.luca.testing.provider.appointment;

import android.net.Uri;

import de.culture4life.luca.testing.TestResult;
import de.culture4life.luca.testing.provider.ProvidedTestResult;

import java.util.UUID;

import androidx.annotation.NonNull;

public class Appointment extends ProvidedTestResult {

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

        lucaTestResult.setType(TestResult.TYPE_APPOINTMENT);
        lucaTestResult.setFirstName(type);
        lucaTestResult.setLastName(address);
        lucaTestResult.setLabName(lab);
        lucaTestResult.setTestingTimestamp(Long.parseLong(timestamp));
        lucaTestResult.setResultTimestamp(lucaTestResult.getTestingTimestamp());
        lucaTestResult.setImportTimestamp(System.currentTimeMillis());
        lucaTestResult.setEncodedData(url);
        lucaTestResult.setHashableEncodedData(qrCode);
        lucaTestResult.setId(UUID.nameUUIDFromBytes(qrCode.getBytes()).toString());
    }

}
