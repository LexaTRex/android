package de.culture4life.luca.preference;

import android.content.Context;
import android.content.SharedPreferences;

import com.nexenio.rxpreferences.provider.SharedPreferencesProvider;

import java.io.IOException;
import java.security.GeneralSecurityException;

import androidx.annotation.NonNull;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Provider that uses Androids EncryptedSharedPreferences for storing keys and values encrypted.
 */
public class EncryptedSharedPreferencesProvider extends SharedPreferencesProvider {

    public static final String SHARED_PREFERENCES_NAME = "encrypted_shared_preferences";

    public EncryptedSharedPreferencesProvider(@NonNull Context context) throws GeneralSecurityException, IOException {
        super(context);
        setSharedPreferences(createEncryptedSharedPreferences(context)); // TODO: 30.04.21 make sharedPreferences protected
    }

    public EncryptedSharedPreferencesProvider(@NonNull SharedPreferences sharedPreferences) {
        super(sharedPreferences);
    }

    private SharedPreferences createEncryptedSharedPreferences(@NonNull Context context) throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(true)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                SHARED_PREFERENCES_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

}
