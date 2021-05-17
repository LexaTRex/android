package de.culture4life.luca.testing.provider.ubirch;

import de.culture4life.luca.testing.TestResult;
import de.culture4life.luca.testing.provider.ProvidedTestResult;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import androidx.annotation.NonNull;

import static de.culture4life.luca.testing.provider.ubirch.UbirchTestResultProvider.URL_PREFIX;

public class UbirchTestResult extends ProvidedTestResult {

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

    public UbirchTestResult(@NonNull String url) {
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

        lucaTestResult.setFirstName(g);
        lucaTestResult.setLastName(f);

        if ("p".equals(r)) {
            lucaTestResult.setOutcome(TestResult.OUTCOME_POSITIVE);
        } else if ("n".equals(r)) {
            lucaTestResult.setOutcome(TestResult.OUTCOME_NEGATIVE);
        } else {
            lucaTestResult.setOutcome(TestResult.OUTCOME_UNKNOWN);
        }

        if ("PCR".equals(t)) {
            lucaTestResult.setType(TestResult.TYPE_PCR);
        } else {
            lucaTestResult.setOutcome(TestResult.TYPE_UNKNOWN);
        }

        try {
            Date testDate = DATE_FORMAT.parse(d);
            lucaTestResult.setTestingTimestamp(testDate.getTime());
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unable to parse test date: " + d);
        }

        lucaTestResult.setResultTimestamp(lucaTestResult.getTestingTimestamp());
        lucaTestResult.setImportTimestamp(System.currentTimeMillis());
        lucaTestResult.setId(UUID.nameUUIDFromBytes(toCompactJson().getBytes()).toString());
        lucaTestResult.setEncodedData(url);
        lucaTestResult.setHashableEncodedData(toCompactJson());
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
