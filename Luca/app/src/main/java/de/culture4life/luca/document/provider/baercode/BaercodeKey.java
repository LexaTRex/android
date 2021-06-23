package de.culture4life.luca.document.provider.baercode;

import com.fasterxml.jackson.databind.JsonNode;

import de.culture4life.luca.util.SerializationUtil;

import java.io.IOException;
import java.util.Arrays;

import androidx.annotation.NonNull;

/**
 * Key object as stored in the Baercode bundle file
 */
class BaercodeKey {

    private int credType;
    private byte[] aesKey;
    private byte[] xCoordinate;
    private byte[] yCoordinate;
    private final String keyId;

    public BaercodeKey(int credType, @NonNull byte[] aesKey, @NonNull byte[] xCoordinate, @NonNull byte[] yCoordinate) {
        this.credType = credType;
        this.aesKey = aesKey;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        byte[] keyId = Arrays.copyOfRange(xCoordinate, xCoordinate.length - 16, xCoordinate.length);
        this.keyId = SerializationUtil.serializeToBase64(keyId).blockingGet();
    }

    public static BaercodeKey from(@NonNull JsonNode dataItems) throws IOException {
        return new BaercodeKey(
                dataItems.get(0).asInt(),
                dataItems.get(1).binaryValue(),
                dataItems.get(2).binaryValue(),
                dataItems.get(3).binaryValue()
        );
    }

    public int getCredType() {
        return credType;
    }

    public byte[] getAesKey() {
        return aesKey;
    }

    public byte[] getxCoordinate() {
        return xCoordinate;
    }

    public byte[] getyCoordinate() {
        return yCoordinate;
    }

    public String getKeyId() {
        return keyId;
    }

}
