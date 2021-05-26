package de.culture4life.luca.preference;

import com.google.gson.GsonBuilder;

import android.content.Context;

import com.nexenio.rxpreferences.provider.BasePreferencesProvider;
import com.nexenio.rxpreferences.provider.InMemoryPreferencesProvider;
import com.nexenio.rxpreferences.provider.PreferencesProvider;
import com.nexenio.rxpreferences.provider.TrayPreferencesProvider;
import com.nexenio.rxpreferences.serializer.GsonSerializer;

import de.culture4life.luca.BuildConfig;
import de.culture4life.luca.LucaApplication;
import de.culture4life.luca.Manager;
import de.culture4life.luca.crypto.TraceIdWrapper;
import de.culture4life.luca.history.HistoryItem;

import net.grandcentrix.tray.TrayPreferences;
import net.grandcentrix.tray.core.TrayStorage;

import java.io.IOException;
import java.security.GeneralSecurityException;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class PreferencesManager extends Manager implements PreferencesProvider {

    private static final int VERSION = 1;
    public static final String LAST_MIGRATION_VERSION_CODE_KEY = "last_migration_version_code";
    public static final GsonSerializer SERIALIZER = new GsonSerializer(new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(TraceIdWrapper.class, new TraceIdWrapper.TypeAdapter())
            .registerTypeAdapter(HistoryItem.class, new HistoryItem.TypeAdapter())
            .create());

    private PreferencesProvider provider;

    @Override
    public Completable doInitialize(@NonNull Context context) {
        return Completable.fromAction(() -> {
            BasePreferencesProvider preferencesProvider;
            if (LucaApplication.isRunningUnitTests()) {
                preferencesProvider = new InMemoryPreferencesProvider();
            } else {
                try {
                    preferencesProvider = new EncryptedSharedPreferencesProvider(context);
                } catch (GeneralSecurityException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
            preferencesProvider.setSerializer(SERIALIZER);
            this.provider = preferencesProvider;
        }).andThen(migratePreferencesIfRequired());
    }

    private Completable migratePreferencesIfRequired() {
        return Single.defer(() -> getInitializedField(provider))
                .flatMapCompletable(preferencesProvider -> preferencesProvider.restoreOrDefault(LAST_MIGRATION_VERSION_CODE_KEY, 0)
                        .filter(lastMigrationVersionCode -> lastMigrationVersionCode < 58 && !LucaApplication.isRunningUnitTests())
                        .flatMapCompletable(this::migratePreferences));
    }

    private Completable migratePreferences(int lastMigrationVersionCode) {
        return Single.fromCallable(() -> {
            TrayPreferences trayPreferences = new TrayPreferences(context, context.getPackageName(), VERSION, TrayStorage.Type.DEVICE);
            TrayPreferencesProvider trayPreferencesProvider = new TrayPreferencesProvider(trayPreferences);
            trayPreferencesProvider.setSerializer(SERIALIZER);
            return trayPreferencesProvider;
        }).flatMapCompletable(trayPreferencesProvider -> PreferencesMigrationUtil.migrate(trayPreferencesProvider, provider))
                .andThen(provider.persist(LAST_MIGRATION_VERSION_CODE_KEY, BuildConfig.VERSION_CODE));
    }

    private Single<PreferencesProvider> getInitializedProvider() {
        return Single.defer(() -> getInitializedField(provider));
    }

    @Override
    public Observable<String> getKeys() {
        return getInitializedProvider()
                .flatMapObservable(PreferencesProvider::getKeys);
    }

    @Override
    public Single<Boolean> containsKey(@NonNull String key) {
        return getInitializedProvider()
                .flatMap(provider -> provider.containsKey(key));
    }

    @Override
    public <Type> Single<Type> restore(@NonNull String key, @NonNull Class<Type> typeClass) {
        return getInitializedProvider()
                .flatMap(provider -> provider.restore(key, typeClass));
    }

    @Override
    public <Type> Single<Type> restoreOrDefault(@NonNull String key, @NonNull Type defaultValue) {
        return getInitializedProvider()
                .flatMap(provider -> provider.restoreOrDefault(key, defaultValue));
    }

    @Override
    public <Type> Observable<Type> restoreOrDefaultAndGetChanges(@NonNull String key, @NonNull Type defaultValue) {
        return getInitializedProvider()
                .flatMapObservable(provider -> provider.restoreOrDefaultAndGetChanges(key, defaultValue));
    }

    @Override
    public <Type> Maybe<Type> restoreIfAvailable(@NonNull String key, @NonNull Class<Type> typeClass) {
        return getInitializedProvider()
                .flatMapMaybe(provider -> provider.restoreIfAvailable(key, typeClass));
    }

    @Override
    public <Type> Observable<Type> restoreIfAvailableAndGetChanges(@NonNull String key, @NonNull Class<Type> typeClass) {
        return getInitializedProvider()
                .flatMapObservable(provider -> provider.restoreIfAvailableAndGetChanges(key, typeClass));
    }

    @Override
    public <Type> Completable persist(@NonNull String key, @NonNull Type value) {
        return getInitializedProvider()
                .flatMapCompletable(provider -> provider.persist(key, value));
    }

    @Override
    public <Type> Completable persistIfNotYetAvailable(@NonNull String key, @NonNull Type value) {
        return getInitializedProvider()
                .flatMapCompletable(provider -> provider.persistIfNotYetAvailable(key, value));
    }

    @Override
    public <Type> Observable<Type> getChanges(@NonNull String key, @NonNull Class<Type> typeClass) {
        return getInitializedProvider()
                .flatMapObservable(provider -> provider.getChanges(key, typeClass));
    }

    @Override
    public Completable delete(@NonNull String key) {
        return getInitializedProvider()
                .flatMapCompletable(provider -> provider.delete(key));
    }

    @Override
    public Completable deleteAll() {
        return getInitializedProvider()
                .flatMapCompletable(PreferencesProvider::deleteAll);
    }

}
