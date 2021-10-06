package de.culture4life.luca.crypto;

import java.security.interfaces.ECPublicKey;

import de.culture4life.luca.checkin.CheckInData;

/**
 * Public part of the daily key pair, used to encrypt {@link CheckInData}.
 */
public class DailyKeyPairPublicKeyWrapper {

    private int id;
    private ECPublicKey publicKey;
    private long creationTimestamp;

    public DailyKeyPairPublicKeyWrapper(int id, ECPublicKey publicKey, long creationTimestamp) {
        this.id = id;
        this.publicKey = publicKey;
        this.creationTimestamp = creationTimestamp;
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

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    @Override
    public String toString() {
        return "DailyKeyPairPublicKeyWrapper{" +
                "id=" + id +
                ", publicKey=" + publicKey +
                ", creationTimestamp=" + creationTimestamp +
                '}';
    }

}
