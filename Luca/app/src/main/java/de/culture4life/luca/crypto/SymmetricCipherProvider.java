package de.culture4life.luca.crypto;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.nexenio.rxkeystore.RxKeyStore;
import com.nexenio.rxkeystore.provider.cipher.symmetric.aes.AesCipherProvider;

import javax.crypto.SecretKey;

import de.culture4life.luca.network.pojo.ContactData;
import de.culture4life.luca.network.pojo.TransferData;
import de.culture4life.luca.ui.checkin.QrCodeData;
import io.reactivex.rxjava3.core.Single;

/**
 * Symmetric cipher provider, used to encrypt {@link ContactData}, {@link TransferData} and {@link
 * QrCodeData} using AES-CTR with 128 bit keys.
 * <p>
 * Uses Bouncy Castle due to Android limitations
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class SymmetricCipherProvider extends AesCipherProvider {

    public SymmetricCipherProvider(RxKeyStore rxKeyStore) {
        super(rxKeyStore);
    }

    @Override
    public Single<SecretKey> generateKey(@NonNull String alias, @NonNull Context context) {
        return getKeyGeneratorInstance()
                .map(keyGenerator -> {
                    keyGenerator.init(128);
                    return keyGenerator.generateKey();
                });
    }

    @Override
    protected String[] getBlockModes() {
        return new String[]{"CTR"};
    }

    @Override
    protected String[] getEncryptionPaddings() {
        return new String[]{RxKeyStore.ENCRYPTION_PADDING_NONE};
    }

    @Override
    protected String getTransformationAlgorithm() {
        return "AES/CTR/" + RxKeyStore.ENCRYPTION_PADDING_NONE;
    }

}
