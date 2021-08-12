package de.culture4life.luca.network.pojo;

import com.google.gson.annotations.SerializedName;

/**
 * Holds encrypted and authenticated {@link TransferData} to be uploaded to the luca server,
 * allowing health departments to reconstruct an infected guest's check-in history.
 *
 * @see <a href="https://www.luca-app.de/securityoverview/properties/secrets.html#term-guest-data-transfer-object">Security
 * Overview: Glossary</a>
 * @see <a href="https://www.luca-app.de/securityoverview/processes/tracing_access_to_history.html#reconstructing-the-infected-guest-s-check-in-history">Security
 * Overview: Reconstructing the Infected Guest’s Check-In History</a>
 */
public class DataTransferRequestData {

    @SerializedName("data")
    private String encryptedContactData;

    @SerializedName("iv")
    private String iv;

    @SerializedName("publicKey")
    private String guestKeyPairPublicKey;

    @SerializedName("mac")
    private String mac;

    @SerializedName("keyId")
    private int dailyKeyPairPublicKeyId;

    public String getEncryptedContactData() {
        return encryptedContactData;
    }

    public void setEncryptedContactData(String encryptedPersonalData) {
        this.encryptedContactData = encryptedPersonalData;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getGuestKeyPairPublicKey() {
        return guestKeyPairPublicKey;
    }

    public void setGuestKeyPairPublicKey(String guestKeyPairPublicKey) {
        this.guestKeyPairPublicKey = guestKeyPairPublicKey;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public int getDailyKeyPairPublicKeyId() {
        return dailyKeyPairPublicKeyId;
    }

    public void setDailyPublicKeyId(int rotatingBackendKeyId) {
        this.dailyKeyPairPublicKeyId = rotatingBackendKeyId;
    }

    @Override
    public String toString() {
        return "DataTransferRequestData{" +
                "encryptedContactData='" + encryptedContactData + '\'' +
                ", iv='" + iv + '\'' +
                ", guestKeyPairPublicKey='" + guestKeyPairPublicKey + '\'' +
                ", mac='" + mac + '\'' +
                ", dailyKeyPairPublicKeyId=" + dailyKeyPairPublicKeyId +
                '}';
    }

}
