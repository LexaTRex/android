package de.culture4life.luca.document.provider.baercode;

import com.fasterxml.jackson.databind.JsonNode;

import de.culture4life.luca.document.Document;
import de.culture4life.luca.document.DocumentParsingException;
import de.culture4life.luca.document.provider.ProvidedDocument;
import de.culture4life.luca.util.SerializationUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

public class BaercodeDocument extends ProvidedDocument {

    public static final int PROTOCOL_VERSION = 1;

    private int diseaseType;
    private String base64KeyId;
    private boolean result;
    private ArrayList<Procedure> procedures = new ArrayList<>();
    protected CoseMessage coseMessage;

    public BaercodeDocument(@NonNull byte[] data) throws DocumentParsingException {
        try {
            parse(data);
        } catch (IOException e) {
            throw new DocumentParsingException("Error while parsing Baercode data", e);
        }
    }

    private void parse(@NonNull byte[] data) throws DocumentParsingException, IOException {
        int version = CoseMessage.MAPPER.readValue(data, Integer.class);
        verifyValue(version, PROTOCOL_VERSION);
        byte[] cborEncodedData = Arrays.copyOfRange(data, 2, data.length);

        coseMessage = new CoseMessage(cborEncodedData);
        JsonNode coseSignMessage = coseMessage.getCoseSignMessage();
        JsonNode coseEncrypt0 = CoseMessage.MAPPER.readTree(coseSignMessage.get(2).binaryValue());
        base64KeyId = coseEncrypt0.get(1).get("4").asText();
        document.setEncodedData(SerializationUtil.serializeToBase64(data).blockingGet());
    }

    /**
     * Update user data with the decrypted data that matches the encrypted cypherText.
     *
     * @param decrypted decrypted cbor message of the user data
     */
    private void updateUserData(@NonNull byte[] decrypted) throws DocumentParsingException, IOException {
        JsonNode userData = CoseMessage.MAPPER.readTree(decrypted);
        document.setHashableEncodedData(new String(decrypted));
        document.setId(UUID.nameUUIDFromBytes(decrypted).toString());
        document.setFirstName(userData.get(0).asText());
        document.setLastName(userData.get(1).asText());
        document.setDateOfBirth(TimeUnit.SECONDS.toMillis(userData.get(2).longValue()));
        diseaseType = userData.get(3).asInt();
        document.setLabName(userData.get(5).asText());
        result = userData.get(6).asBoolean();

        procedures.clear();
        for (Iterator<JsonNode> it = userData.get(4).elements(); it.hasNext(); ) {
            procedures.add(Procedure.from(it.next()));
        }
        Collections.sort(procedures, (o1, o2) -> (int) (o1.getTimestamp() - o2.getTimestamp()));
        check(procedures);
        ArrayList<Document.Procedure> testResultProcedures = new ArrayList<>();
        for (int i = 0; i < procedures.size(); i++) {
            Procedure procedure = procedures.get(i);
            testResultProcedures.add(new Document.Procedure(procedure.getType().toTestResultType(), procedure.getTimestamp(),
                    procedures.size() - i, procedures.size()));
        }
        document.setProcedures(testResultProcedures);

        Procedure procedure = procedures.get(0);
        document.setType(getType(procedure));
        document.setOutcome(getOutcome(procedures, result));
        document.setTestingTimestamp(procedure.getTimestamp());
        document.setImportTimestamp(System.currentTimeMillis());
        // We don't have a correct result time stamp, use the procedure time instead
        document.setResultTimestamp(procedure.getTimestamp());
    }

    public void verifyAndDecryptPersonalData(@NonNull BaercodeKey baercodeKey) throws DocumentParsingException {
        try {
            ECPublicKey publicKey = BaercodeDocumentProvider.createPublicKey(baercodeKey);
            if (!coseMessage.verify(publicKey)) {
                throw new DocumentParsingException("Baercode signature is not valid:");
            }
            byte[] decoded = coseMessage.decodeCypherText(baercodeKey);
            updateUserData(decoded);
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            throw new DocumentParsingException("Exception while parsing Baercode", e);
        }
    }

    protected static void check(ArrayList<Procedure> procedures) throws DocumentParsingException {
        if (procedures.isEmpty()) {
            throw new DocumentParsingException("Procedures size in baercode is empty");
        } else {
            Procedure firstProcedure = procedures.get(0);
            for (Procedure procedure : procedures) {
                if (firstProcedure.isVaccination() != procedure.isVaccination()) {
                    throw new DocumentParsingException("Vaccination and non-vaccination types are mixed in procedures");
                }
            }
        }
    }

    private static int getType(@NonNull Procedure procedure) {
        if (procedure.getType() == Procedure.Type.ANTIGEN_FAST_TEST) {
            return Document.TYPE_FAST;
        } else if (procedure.getType() == Procedure.Type.PCR_TEST) {
            return Document.TYPE_PCR;
        } else if (procedure.isVaccination()) {
            return Document.TYPE_VACCINATION;
        } else {
            throw new IllegalArgumentException(String.format("Procedure type is not a test result: %d", procedure.getType().getValue()));
        }
    }

    protected static int getOutcome(@NonNull ArrayList<Procedure> procedures, boolean result) {
        if (procedures.isEmpty()) {
            return Document.OUTCOME_UNKNOWN;
        }
        Procedure procedure = procedures.get(0);
        if (procedure.isVaccination()) {
            if (result) {
                if (procedures.size() >= procedure.getRequiredCount()) {
                    return Document.OUTCOME_FULLY_IMMUNE;
                } else {
                    return Document.OUTCOME_PARTIALLY_IMMUNE;
                }
            } else {
                return Document.OUTCOME_UNKNOWN;
            }
        } else {
            return result ? Document.OUTCOME_POSITIVE : Document.OUTCOME_NEGATIVE;
        }
    }

    private static void verifyValue(Object receivedValue, Object expectedValue) throws DocumentParsingException {
        if (!expectedValue.equals(receivedValue)) {
            throw new DocumentParsingException(String.format("Unexpected value %s received", receivedValue));
        }
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
