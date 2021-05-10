package de.culture4life.luca.crypto;

import com.nexenio.rxkeystore.RxKeyStore;
import com.nexenio.rxkeystore.provider.signature.BaseSignatureProvider;

import androidx.annotation.NonNull;

/**
 * Signature provider used for signing contact data and verifying daily key.
 */
public class SignatureProvider extends BaseSignatureProvider {

    public SignatureProvider(@NonNull RxKeyStore rxKeyStore) {
        super(rxKeyStore, "SHA256withECDSA");
    }

}
