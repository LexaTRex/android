package de.culture4life.luca.crypto;

import androidx.annotation.NonNull;

import com.nexenio.rxkeystore.RxKeyStore;
import com.nexenio.rxkeystore.provider.hash.Sha256HashProvider;

/**
 * Provides hashes using SHA256.
 */
public class HashProvider extends Sha256HashProvider {

    public static final int TRIMMED_HASH_LENGTH = 16;

    public HashProvider(@NonNull RxKeyStore rxKeyStore) {
        super(rxKeyStore);
    }

}
