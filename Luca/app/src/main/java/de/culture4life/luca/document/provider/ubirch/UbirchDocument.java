package de.culture4life.luca.document.provider.ubirch;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import de.culture4life.luca.document.Document;
import de.culture4life.luca.document.provider.ProvidedDocument;

import static de.culture4life.luca.document.provider.ubirch.UbirchDocumentProvider.URL_PREFIX;

public class UbirchDocument extends ProvidedDocument {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmm", Locale.GERMANY);

    String b;
    String d;
    String f;
    String g;
    String i;
    String p;
    String r;
    String s;
    String t;

    public UbirchDocument(@NonNull String url) {
        if (!url.startsWith(URL_PREFIX)) {
            throw new IllegalArgumentException("Invalid encoded data");
        }

        for (String element : url.substring(URL_PREFIX.length()).split(";")) {
            String key = element.substring(0, 1);
            String value = element.substring(2);
            try {
                setField(key, value);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalArgumentException("Unable to set field: " + key);
            }
        }

        document.setFirstName(g);
        document.setLastName(f);

        if ("p".equals(r)) {
            document.setOutcome(Document.OUTCOME_POSITIVE);
        } else if ("n".equals(r)) {
            document.setOutcome(Document.OUTCOME_NEGATIVE);
        } else {
            document.setOutcome(Document.OUTCOME_UNKNOWN);
        }

        if ("PCR".equals(t)) {
            document.setType(Document.TYPE_PCR);
        } else {
            document.setOutcome(Document.TYPE_UNKNOWN);
        }

        try {
            Date testDate = DATE_FORMAT.parse(d);
            document.setTestingTimestamp(testDate.getTime());
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unable to parse test date: " + d);
        }

        document.setResultTimestamp(document.getTestingTimestamp());
        document.setImportTimestamp(System.currentTimeMillis());
        document.setId(UUID.nameUUIDFromBytes(toCompactJson().getBytes()).toString());
        document.setProvider("Ubirch");
        document.setEncodedData(url);
        document.setHashableEncodedData(toCompactJson());
    }

    void setField(@NonNull String key, String value) throws NoSuchFieldException, IllegalAccessException {
        Field field = this.getClass().getDeclaredField(key);
        field.setAccessible(true);
        field.set(this, value);
    }

    String toCompactJson() {
        return "{" +
                "\"b\":\"" + b + '\"' +
                ",\"d\":\"" + d + '\"' +
                ",\"f\":\"" + f + '\"' +
                ",\"g\":\"" + g + '\"' +
                ",\"i\":\"" + i + '\"' +
                ",\"p\":\"" + p + '\"' +
                ",\"r\":\"" + r + '\"' +
                ",\"s\":\"" + s + '\"' +
                ",\"t\":\"" + t + '\"' +
                '}';
    }

}
