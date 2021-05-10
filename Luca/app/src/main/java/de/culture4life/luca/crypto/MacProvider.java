package de.culture4life.luca.crypto;

import com.nexenio.rxkeystore.RxKeyStore;
import com.nexenio.rxkeystore.provider.mac.HmacProvider;

import androidx.annotation.NonNull;

/**
 * Provides message authentication codes using HMAC-SHA256.
 */
public class MacProvider extends HmacProvider {

    public MacProvider(@NonNull RxKeyStore rxKeyStore) {
        super(rxKeyStore, HASH_ALGORITHM_SHA256);
    }

}
