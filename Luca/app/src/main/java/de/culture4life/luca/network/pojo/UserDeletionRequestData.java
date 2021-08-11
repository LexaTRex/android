package de.culture4life.luca.network.pojo;

import com.google.gson.annotations.SerializedName;

import java.nio.charset.StandardCharsets;

/**
 * Request body for deleting an account.
 * <p>
 * Example:
 * <pre>
 * {
 *   "signature": "...",
 * }
 * </pre>
 */
public class UserDeletionRequestData {

    public static final byte[] DELETE_USER = "DELETE_USER".getBytes(StandardCharsets.UTF_8);

    @SerializedName("signature")
    private String signature;

    public UserDeletionRequestData(String signature) {
        this.signature = signature;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return "UserDeletionRequestData{" +
                ", signature='" + signature + '\'' +
                '}';
    }

}
