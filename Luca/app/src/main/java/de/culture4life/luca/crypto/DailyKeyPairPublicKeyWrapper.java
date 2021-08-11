package de.culture4life.luca.crypto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.security.interfaces.ECPublicKey;

import de.culture4life.luca.checkin.CheckInData;

/**
 * Public part of the daily key pair, used to encrypt {@link CheckInData}.
 */
public class DailyKeyPairPublicKeyWrapper {

    @SerializedName("id")
    @Expose
    private int id;

    @SerializedName("publicKey")
    @Expose
    private ECPublicKey publicKey;

    public DailyKeyPairPublicKeyWrapper() {
    }

    public DailyKeyPairPublicKeyWrapper(int id, ECPublicKey publicKey) {
        this.id = id;
        this.publicKey = publicKey;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ECPublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(ECPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public String toString() {
        return "DailyKeyPairPublicKeyWrapper{" +
                "id=" + id +
                ", publicKey=" + publicKey +
                '}';
    }

}
