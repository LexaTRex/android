package de.culture4life.luca.testing.provider.baercode;

import com.fasterxml.jackson.databind.JsonNode;

import de.culture4life.luca.testing.TestResult;
import de.culture4life.luca.testing.TestResultParsingException;
import de.culture4life.luca.testing.provider.ProvidedTestResult;
import de.culture4life.luca.util.SerializationUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import androidx.annotation.NonNull;

public class BaercodeTestResult extends ProvidedTestResult {

    public static final int PROTOCOL_VERSION = 1;

    private long dateOfBirth;
    private int diseaseType;
    private String base64KeyId;
    private boolean result;
    private ArrayList<Procedure> procedures = new ArrayList<>();
    protected CoseMessage coseMessage;

    public BaercodeTestResult(@NonNull byte[] data) throws TestResultParsingException {
        try {
            parse(data);
        } catch (IOException e) {
            throw new TestResultParsingException("Error while parsing Baercode data", e);
        }
    }

    private void parse(@NonNull byte[] data) throws TestResultParsingException, IOException {
        int version = CoseMessage.MAPPER.readValue(data, Integer.class);
        verifyValue(version, PROTOCOL_VERSION);
        byte[] cborEncodedData = Arrays.copyOfRange(data, 2, data.length);

        coseMessage = new CoseMessage(cborEncodedData);
        JsonNode coseSignMessage = coseMessage.getCoseSignMessage();
        JsonNode coseEncrypt0 = CoseMessage.MAPPER.readTree(coseSignMessage.get(2).binaryValue());
        base64KeyId = coseEncrypt0.get(1).get("4").asText();
        lucaTestResult.setEncodedData(SerializationUtil.serializeToBase64(data).blockingGet());
    }

    /**
     * Update user data with the decrypted data that matches the encrypted cypherText.
     *
     * @param decrypted decrypted cbor message of the user data
     */
    private void updateUserData(@NonNull byte[] decrypted) throws TestResultParsingException, IOException {
        JsonNode userData = CoseMessage.MAPPER.readTree(decrypted);
        lucaTestResult.setId(UUID.nameUUIDFromBytes(decrypted).toString());
        lucaTestResult.setFirstName(userData.get(0).asText());
        lucaTestResult.setLastName(userData.get(1).asText());
        dateOfBirth = userData.get(2).longValue();
        diseaseType = userData.get(3).asInt();
        lucaTestResult.setLabName(userData.get(5).asText());
        result = userData.get(6).asBoolean();

        procedures.clear();
        for (Iterator<JsonNode> it = userData.get(4).elements(); it.hasNext(); ) {
            procedures.add(Procedure.from(it.next()));
        }
        Collections.sort(procedures, (o1, o2) -> (int) (o1.getTimestamp() - o2.getTimestamp()));
        check(procedures);
        ArrayList<TestResult.Procedure> testResultProcedures = new ArrayList<>();
        for (Procedure procedure : procedures) {
            testResultProcedures.add(new TestResult.Procedure(procedure.getType(), procedure.getTimestamp()));
        }
        lucaTestResult.setProcedures(testResultProcedures);

        Procedure procedure = procedures.get(0);
        lucaTestResult.setType(getTestResultType(procedure));
        lucaTestResult.setOutcome(getOutcome(procedures, result));
        lucaTestResult.setTestingTimestamp(procedure.getTimestamp());
        lucaTestResult.setImportTimestamp(System.currentTimeMillis());
        // We don't have a correct result time stamp, use the procedure time instead
        lucaTestResult.setResultTimestamp(procedure.getTimestamp());
    }

    public void verifyAndDecryptPersonalData(@NonNull BaercodeKey baercodeKey) throws TestResultParsingException {
        try {
            ECPublicKey publicKey = BaercodeTestResultProvider.createPublicKey(baercodeKey);
            if (!coseMessage.verify(publicKey)) {
                throw new TestResultParsingException("Baercode signature is not valid:");
            }
            byte[] decoded = coseMessage.decodeCypherText(baercodeKey);
            updateUserData(decoded);
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            throw new TestResultParsingException("Exception while parsing Baercode", e);
        }
    }

    protected static void check(ArrayList<Procedure> procedures) throws TestResultParsingException {
        if (procedures.isEmpty()) {
            throw new TestResultParsingException("Procedures size in baercode is empty");
        } else {
            Procedure firstProcedure = procedures.get(0);
            for (Procedure procedure : procedures) {
                if (firstProcedure.isVaccination() != procedure.isVaccination()) {
                    throw new TestResultParsingException("Vaccination and non-vaccination types are mixed in procedures");
                }
            }
        }
    }

    private static int getTestResultType(@NonNull Procedure procedure) {
        if (procedure.getType() == Procedure.Type.ANTIGEN_FAST_TEST) {
            return TestResult.TYPE_FAST;
        } else if (procedure.getType() == Procedure.Type.PCR_TEST) {
            return TestResult.TYPE_PCR;
        } else if (procedure.isVaccination()) {
            return TestResult.TYPE_VACCINATION;
        } else {
            throw new IllegalArgumentException(String.format("Procedure type is not a test result: %d", procedure.getType().getValue()));
        }
    }

    protected static int getOutcome(@NonNull ArrayList<Procedure> procedures, boolean result) {
        if (procedures.isEmpty()) {
            return TestResult.OUTCOME_UNKNOWN;
        }
        Procedure procedure = procedures.get(0);
        if (procedure.isVaccination()) {
            if (result) {
                if (procedures.size() >= procedure.getRequiredCount()) {
                    return TestResult.OUTCOME_FULLY_VACCINATED;
                } else {
                    return TestResult.OUTCOME_PARTIALLY_VACCINATED;
                }
            } else {
                return TestResult.OUTCOME_UNKNOWN;
            }
        } else {
            return result ? TestResult.OUTCOME_POSITIVE : TestResult.OUTCOME_NEGATIVE;
        }
    }

    private static void verifyValue(Object receivedValue, Object expectedValue) throws TestResultParsingException {
        if (!expectedValue.equals(receivedValue)) {
            throw new TestResultParsingException(String.format("Unexpected value %s received", receivedValue));
        }
    }

    public long getDateOfBirth() {
        return dateOfBirth;
    }

    public int getDiseaseType() {
        return diseaseType;
    }

    public boolean isResult() {
        return result;
    }

    public ArrayList<Procedure> getProcedures() {
        return procedures;
    }

    public String getBase64KeyId() {
        return base64KeyId;
    }

}
