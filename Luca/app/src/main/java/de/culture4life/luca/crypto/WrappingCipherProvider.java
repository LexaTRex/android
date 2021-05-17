package de.culture4life.luca.crypto;

import com.nexenio.rxkeystore.RxKeyStore;
import com.nexenio.rxkeystore.provider.cipher.asymmetric.rsa.RsaCipherProvider;

/**
 * Cipher provider for encrypting {@link WrappedSecret}s. Uses the Android key store.
 */
public class WrappingCipherProvider extends RsaCipherProvider {

    public WrappingCipherProvider(RxKeyStore rxKeyStore) {
        super(rxKeyStore);
        this.useStrongBoxIfAvailable = true;
    }

}
