package de.culture4life.luca.preference;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.GsonBuilder;
import com.nexenio.rxpreferences.provider.BasePreferencesProvider;
import com.nexenio.rxpreferences.provider.InMemoryPreferencesProvider;
import com.nexenio.rxpreferences.provider.PreferencesProvider;
import com.nexenio.rxpreferences.serializer.GsonSerializer;

import de.culture4life.luca.LucaApplication;
import de.culture4life.luca.Manager;
import de.culture4life.luca.crypto.TraceIdWrapper;
import de.culture4life.luca.history.HistoryItem;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class PreferencesManager extends Manager implements PreferencesProvider {

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
                preferencesProvider = new EncryptedSharedPreferencesProvider(context);
            }
            preferencesProvider.setSerializer(SERIALIZER);
            this.provider = preferencesProvider;
        });
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
                .flatMapCompletable(preferencesProvider -> preferencesProvider.deleteAll()
                        .andThen(Completable.defer(() -> {
                            if (preferencesProvider instanceof EncryptedSharedPreferencesProvider) {
                                return ((EncryptedSharedPreferencesProvider) preferencesProvider).deletePreferencesFile(context);
                            } else {
                                return Completable.complete();
                            }
                        })));
    }

}
