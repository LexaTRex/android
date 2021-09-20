package de.culture4life.luca.preference;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.security.crypto.EncryptedSharedPreferences;

import java.io.File;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/**
 * Provider that uses Androids {@link EncryptedSharedPreferences} for storing keys and values encrypted.
 * TODO: move this to rxpreferences library
 */
public class EncryptedSharedPreferencesProvider extends com.nexenio.rxpreferences.provider.EncryptedSharedPreferencesProvider {

    public EncryptedSharedPreferencesProvider(@NonNull Context context) {
        super(context);
    }

    @Override
    protected Single<SharedPreferences> createEncryptedSharedPreferences(@NonNull Context context) {
        return super.createEncryptedSharedPreferences(context)
                .retry(1, throwable -> deletePreferencesFile(context)
                        .andThen(Single.just(true))
                        .onErrorReturnItem(false)
                        .blockingGet());
    }

    /**
     * If the Android KeyStore gets in a corrupted state or entries required for the {@link EncryptedSharedPreferences} get deleted,
     * the shared preferences file needs to be deleted manually before a new {@link EncryptedSharedPreferences} instance can be created.
     *
     * @see <a href="https://issuetracker.google.com/issues/164901843">InvalidProtocolBufferException on initialization on some devices</a>
     */
    public Completable deletePreferencesFile(@NonNull Context context) {
        return Completable.fromAction(() -> {
            File file = new File(context.getApplicationInfo().dataDir + "/shared_prefs/" + SHARED_PREFERENCES_NAME + ".xml");
            if (file.exists()) {
                if (!file.delete()) {
                    throw new IllegalStateException("Unable to delete preferences file");
                }
            }
        });
    }

}
