package de.culture4life.luca.crypto;

import static de.culture4life.luca.crypto.HashProvider.TRIMMED_HASH_LENGTH;
import static de.culture4life.luca.util.SingleUtil.retryWhen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Base64;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nexenio.rxkeystore.RxKeyStore;
import com.nexenio.rxkeystore.provider.hash.RxHashProvider;
import com.nexenio.rxkeystore.util.RxBase64;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import de.culture4life.luca.Manager;
import de.culture4life.luca.checkin.CheckInManager;
import de.culture4life.luca.meeting.MeetingGuestData;
import de.culture4life.luca.network.NetworkManager;
import de.culture4life.luca.network.endpoints.LucaEndpointsV3;
import de.culture4life.luca.network.pojo.CheckInRequestData;
import de.culture4life.luca.network.pojo.DailyKeyPairIssuer;
import de.culture4life.luca.preference.PreferencesManager;
import de.culture4life.luca.ui.checkin.QrCodeData;
import de.culture4life.luca.ui.checkin.CheckInViewModel;
import de.culture4life.luca.util.SerializationUtil;
import de.culture4life.luca.util.TimeUtil;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Provides access to all cryptographic methods, handles Bouncy-Castle key store persistence.
 */
public class CryptoManager extends Manager {

    public static final String KEYSTORE_FILE_NAME = "keys.ks";
    public static final String DAILY_KEY_PAIR_PUBLIC_KEY_ID_KEY = "daily_key_pair_public_key_id";
    public static final String DAILY_KEY_PAIR_PUBLIC_KEY_POINT_KEY = "daily_key_pair_public_key";
    public static final String DATA_SECRET_KEY = "user_data_secret_2";
    public static final String TRACE_ID_WRAPPERS_KEY = "tracing_id_wrappers";
    public static final String TRACING_SECRET_KEY_PREFIX = "tracing_secret_";
    public static final String ALIAS_GUEST_KEY_PAIR = "user_master_key_pair";
    public static final String ALIAS_USER_EPHEMERAL_KEY_PAIR = "user_ephemeral_key_pair";
    public static final String ALIAS_SCANNER_EPHEMERAL_KEY_PAIR = "scanner_ephemeral_key_pair";
    public static final String ALIAS_MEETING_EPHEMERAL_KEY_PAIR = "meeting_ephemeral_key_pair";
    public static final String ALIAS_KEYSTORE_PASSWORD = "keystore_secret";
    public static final String ALIAS_SECRET_WRAPPING_KEY_PAIR = "secret_wrapping_key_pair";

    private static final byte[] DATA_ENCRYPTION_SECRET_SUFFIX = new byte[]{0x01};
    private static final byte[] DATA_AUTHENTICATION_SECRET_SUFFIX = new byte[]{0x02};

    private final PreferencesManager preferencesManager;
    private final NetworkManager networkManager;

    private final RxKeyStore androidKeyStore;
    private final RxKeyStore bouncyCastleKeyStore;

    private final WrappingCipherProvider wrappingCipherProvider;
    private final SymmetricCipherProvider symmetricCipherProvider;
    private final AsymmetricCipherProvider asymmetricCipherProvider;
    private final SignatureProvider signatureProvider;
    private final MacProvider macProvider;
    private final HashProvider hashProvider;

    private final SecureRandom secureRandom;

    private Context context;

    @Nullable
    private DailyKeyPairPublicKeyWrapper dailyKeyPairPublicKeyWrapper;

    @SuppressLint("NewApi")
    public CryptoManager(@NonNull PreferencesManager preferencesManager, @NonNull NetworkManager networkManager) {
        this.preferencesManager = preferencesManager;
        this.networkManager = networkManager;

        androidKeyStore = new RxKeyStore(RxKeyStore.TYPE_ANDROID, getAndroidKeyStoreProviderName());
        bouncyCastleKeyStore = new RxKeyStore(RxKeyStore.TYPE_BKS, RxKeyStore.PROVIDER_BOUNCY_CASTLE);

        wrappingCipherProvider = new WrappingCipherProvider(androidKeyStore);
        symmetricCipherProvider = new SymmetricCipherProvider(bouncyCastleKeyStore);
        asymmetricCipherProvider = new AsymmetricCipherProvider(bouncyCastleKeyStore);
        signatureProvider = new SignatureProvider(bouncyCastleKeyStore);
        macProvider = new MacProvider(bouncyCastleKeyStore);
        hashProvider = new HashProvider(bouncyCastleKeyStore);
        secureRandom = new SecureRandom();
    }

    /**
     * Initialize manager, setup security providers, load {@link #bouncyCastleKeyStore} and migrate
     * insecure tracing secret of older app versions (if any).
     */
    @Override
    protected Completable doInitialize(@NonNull Context context) {
        return Completable.mergeArray(
                preferencesManager.initialize(context),
                networkManager.initialize(context)
        ).andThen(Completable.fromAction(() -> this.context = context))
                .andThen(setupSecurityProviders())
                .andThen(loadKeyStoreFromFile().onErrorComplete());
    }

    @Nullable
    private String getAndroidKeyStoreProviderName() {
        Set<String> availableProviders = new HashSet<>();
        for (Provider provider : Security.getProviders()) {
            availableProviders.add(provider.getName());
        }
        Timber.i("Available security providers: %s", availableProviders);
        boolean hasWorkaroundProvider = availableProviders.contains("AndroidKeyStoreBCWorkaround");
        if (hasWorkaroundProvider && Build.VERSION.SDK_INT != Build.VERSION_CODES.M) {
            // Not using the default provider (null), will cause a NoSuchAlgorithmException.
            // See https://stackoverflow.com/questions/36111452/androidkeystore-nosuchalgorithm-exception
            Timber.d("BC workaround provider present, using default provider");
            return null;
        } else {
            return RxKeyStore.PROVIDER_ANDROID_KEY_STORE;
        }
    }

    /**
     * Substitute outdated Android-provided Bouncy Castle with bundled one.
     */
    public static Completable setupSecurityProviders() {
        return Completable.fromAction(() -> {
            final Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
            if (!(provider instanceof BouncyCastleProvider)) {
                // Android registers its own BC provider. As it might be outdated and might not include
                // all needed ciphers, we substitute it with a known BC bundled in the app.
                // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
                // of that it's possible to have another BC implementation loaded in VM.
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
                Security.addProvider(new BouncyCastleProvider());
                Timber.i("Inserted bouncy castle provider");
            }
        });
    }

    /**
     * Load {@link #bouncyCastleKeyStore} from internal file, decrypt it using {@link
     * #androidKeyStore}-backed password.
     *
     * @see WrappedSecret
     */
    private Completable loadKeyStoreFromFile() {
        return getKeyStorePasswordOrHardcodedValue()
                .flatMapCompletable(passwordChars -> Single.fromCallable(() -> context.openFileInput(KEYSTORE_FILE_NAME))
                                .flatMapCompletable(inputStream -> bouncyCastleKeyStore.load(inputStream, passwordChars)
                                        .doFinally(() -> inputStream.close())))
                .doOnSubscribe(disposable -> Timber.d("Loading keystore from file"))
                .doOnError(throwable -> Timber.w("Unable to load keystore from file: %s", throwable.toString()));
    }

    /**
     * Store BC keystore using {@link #androidKeyStore}-backed password.
     *
     * @see WrappedSecret
     */
    private Completable persistKeyStoreToFile() {
        return getKeyStorePassword()
                .flatMapCompletable(passwordChars -> Single.fromCallable(() -> context.openFileOutput(KEYSTORE_FILE_NAME, Context.MODE_PRIVATE))
                        .flatMapCompletable(outputStream -> bouncyCastleKeyStore.save(outputStream, passwordChars)
                                .doFinally(() -> outputStream.close())))
                .doOnSubscribe(disposable -> Timber.d("Persisting keystore to file"))
                .doOnError(throwable -> Timber.e("Unable to persist keystore to file: %s", throwable.toString()));
    }

    /**
     * In app versions before 1.2.4, the {@link #bouncyCastleKeyStore} was protected with a
     * hardcoded password. For migration purposes, this method either emits that hardcoded password
     * or the result of {@link #getKeyStorePassword()}. Should only be used when loading the key
     * store.
     */
    private Single<char[]> getKeyStorePasswordOrHardcodedValue() {
        return preferencesManager.containsKey(ALIAS_KEYSTORE_PASSWORD)
                .flatMap(hasPassword -> hasPassword ? getKeyStorePassword() : Single.just("luca".toCharArray()));
    }

    /**
     * Fetch {@link #bouncyCastleKeyStore} password if available, otherwise generate new random
     * password and persist it using {@link #androidKeyStore}-backed {@link WrappedSecret}}.
     */
    private Single<char[]> getKeyStorePassword() {
        return restoreWrappedSecretIfAvailable(ALIAS_KEYSTORE_PASSWORD)
                .switchIfEmpty(generateSecureRandomData(128)
                        .doOnSuccess(bytes -> Timber.d("Generated new random key store password"))
                        .flatMap(randomData -> persistWrappedSecret(ALIAS_KEYSTORE_PASSWORD, randomData)
                                .andThen(Single.just(randomData))))
                .map(CryptoManager::convertToCharArray);
    }

    private static char[] convertToCharArray(@NonNull byte[] bytes) {
        char[] chars = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            chars[i] = (char) bytes[i];
        }
        return chars;
    }

    /*
        Secret wrapping
     */

    /**
     * Will get or generate a key pair using the {@link #wrappingCipherProvider} which may be used
     * for restoring or persisting {@link WrappedSecret}s.
     */
    protected Single<KeyPair> getSecretWrappingKeyPair() {
        return wrappingCipherProvider.getKeyPairIfAvailable(ALIAS_SECRET_WRAPPING_KEY_PAIR)
                .switchIfEmpty(wrappingCipherProvider.generateKeyPair(ALIAS_SECRET_WRAPPING_KEY_PAIR, context)
                        .doOnSubscribe(disposable -> Timber.d("Generating new secret wrapping key pair"))
                        .doOnError(throwable -> Timber.e("Unable to generate secret wrapping key pair: %s", throwable.toString())))
                .compose(retryWhen(KeyStoreException.class, 3));
    }

    /**
     * Will restore the {@link WrappedSecret} using the {@link #preferencesManager} and decrypt it
     * using the {@link #wrappingCipherProvider}.
     * <p>
     * {@link WrappedSecret}s are encrypted using an AndroidKeyStore-backed key.
     */
    private Maybe<byte[]> restoreWrappedSecretIfAvailable(@NonNull String alias) {
        return preferencesManager.restoreIfAvailable(alias, WrappedSecret.class)
                .flatMapSingle(wrappedSecret -> getSecretWrappingKeyPair()
                        .flatMap(keyPair -> wrappingCipherProvider.decrypt(wrappedSecret.getDeserializedEncryptedSecret(), wrappedSecret.getDeserializedIv(), keyPair.getPrivate())
                                .compose(retryWhen(KeyStoreException.class, 3))))
                .doOnError(throwable -> Timber.e("Unable to restore wrapped secret: %s", throwable.toString()));
    }

    /**
     * Will encrypt the specified secret using the {@link #wrappingCipherProvider} and persist it as
     * a {@link WrappedSecret} using the {@link #preferencesManager}.
     */
    private Completable persistWrappedSecret(@NonNull String alias, @NonNull byte[] secret) {
        return getSecretWrappingKeyPair()
                .flatMap(keyPair -> wrappingCipherProvider.encrypt(secret, keyPair.getPublic())
                        .compose(retryWhen(KeyStoreException.class, 3)))
                .map(WrappedSecret::new)
                .flatMapCompletable(wrappedSecret -> preferencesManager.persist(alias, wrappedSecret))
                .doOnError(throwable -> Timber.e("Unable to persist wrapped secret: %s", throwable.toString()));
    }

    /*
        Check-in
     */

    /**
     * Generate and persist a trace ID from given user ID.
     */
    public Single<TraceIdWrapper> getTraceIdWrapper(@NonNull UUID userId) {
        return generateTraceIdWrapper(userId)
                .flatMap(traceIdWrapper -> persistTraceIdWrapper(traceIdWrapper)
                        .andThen(Single.just(traceIdWrapper)));
    }

    private Single<TraceIdWrapper> generateTraceIdWrapper(@NonNull UUID userId) {
        return TimeUtil.getCurrentUnixTimestamp()
                .flatMap(TimeUtil::roundUnixTimestampDownToMinute)
                .flatMap(roundedUnixTimestamp -> generateTraceId(userId, roundedUnixTimestamp)
                        .map(traceId -> new TraceIdWrapper(roundedUnixTimestamp, traceId)));
    }

    public Single<byte[]> generateTraceId(@NonNull UUID userId, long roundedUnixTimestamp) {
        return Single.zip(encode(userId), TimeUtil.encodeUnixTimestamp(roundedUnixTimestamp), Pair::new)
                .flatMap(encodedDataPair -> concatenate(encodedDataPair.first, encodedDataPair.second))
                .flatMap(encodedData -> getCurrentTracingSecret()
                        .flatMap(CryptoManager::createKeyFromSecret)
                        .flatMap(traceKey -> macProvider.sign(encodedData, traceKey)))
                .flatMap(traceId -> trim(traceId, TRIMMED_HASH_LENGTH))
                .doOnSuccess(traceId -> Timber.d("Generated new trace ID: %s", SerializationUtil.serializeToBase64(traceId).blockingGet()));
    }

    /**
     * Get current {@link TraceIdWrapper}s ordered by timestamp.
     */
    public Observable<TraceIdWrapper> getTraceIdWrappers() {
        return restoreTraceIdWrappers();
    }

    /**
     * Restore {@link TraceIdWrapperList} from {@link PreferencesManager} and sort by timestamp.
     */
    private Observable<TraceIdWrapper> restoreTraceIdWrappers() {
        return preferencesManager.restoreIfAvailable(TRACE_ID_WRAPPERS_KEY, TraceIdWrapperList.class)
                .flatMapObservable(Observable::fromIterable)
                .sorted((first, second) -> Long.compare(first.getTimestamp(), second.getTimestamp()));
    }

    /**
     * Persist given {@link TraceIdWrapper}, appending it to the list of stored ones.
     */
    private Completable persistTraceIdWrapper(@NonNull TraceIdWrapper traceIdWrapper) {
        return getTraceIdWrappers()
                .mergeWith(Observable.just(traceIdWrapper))
                .toList()
                .map(TraceIdWrapperList::new)
                .flatMapCompletable(traceIdWrappers -> preferencesManager.persist(TRACE_ID_WRAPPERS_KEY, traceIdWrappers));
    }

    /**
     * Delete all trace IDs and their associated user ephemeral key pair.
     *
     * @see CryptoManager#deleteUserEphemeralKeyPair
     */
    public Completable deleteTraceData() {
        return getTraceIdWrappers()
                .map(TraceIdWrapper::getTraceId)
                .flatMapCompletable(this::deleteUserEphemeralKeyPair)
                .andThen(preferencesManager.delete(TRACE_ID_WRAPPERS_KEY));
    }

    public Single<byte[]> generateEphemeralDiffieHellmanSecret(@NonNull PrivateKey ephemeralUserPrivateKey) {
        return generateSharedDiffieHellmanSecret(ephemeralUserPrivateKey);
    }

    /*
        Daily key pair public key
     */

    /**
     * Will fetch the latest daily key pair public key from the API, update {@link
     * #dailyKeyPairPublicKeyWrapper} and persist it in the preferences.
     * <p>
     * This should be done on each app start, but not required for a successful initialization (e.g.
     * because the user may be offline).
     */
    public Completable updateDailyKeyPairPublicKey() {
        return fetchDailyKeyPairPublicKeyWrapperFromBackend()
                .doOnSuccess(wrapper -> this.dailyKeyPairPublicKeyWrapper = wrapper)
                .flatMapCompletable(this::persistDailyKeyPairPublicKeyWrapper)
                .doOnSubscribe(disposable -> Timber.d("Updating daily key pair public key"));
    }

    /**
     * Get {@link DailyKeyPairPublicKeyWrapper}, restore it if available, attempt fetching it from
     * server otherwise using {@link #updateDailyKeyPairPublicKey()}.
     */
    public Single<DailyKeyPairPublicKeyWrapper> getDailyKeyPairPublicKeyWrapper() {
        return Maybe.fromCallable(() -> dailyKeyPairPublicKeyWrapper)
                .switchIfEmpty(restoreDailyKeyPairPublicKeyWrapper()
                        .doOnSuccess(restoredKey -> dailyKeyPairPublicKeyWrapper = restoredKey)
                        .switchIfEmpty(updateDailyKeyPairPublicKey()
                                .andThen(Single.fromCallable(() -> dailyKeyPairPublicKeyWrapper))));
    }

    public Single<DailyKeyPairIssuer> getDailyKeyPair() {
        return networkManager.getLucaEndpointsV3()
                .flatMap(LucaEndpointsV3::getDailyKeyPair)
                .flatMap(dailyKeyPair -> networkManager.getLucaEndpointsV3()
                        .flatMap(lucaEndpointsV3 -> lucaEndpointsV3.getIssuer(dailyKeyPair.getIssuerId()))
                        .map(issuer -> new DailyKeyPairIssuer(dailyKeyPair, issuer))
                );
    }

    public Completable verifyDailyKeyPair(@NonNull DailyKeyPairIssuer dailyKeyPairIssuer) {
        return Completable.defer(() -> {
            PublicKey issuerSigningKey = dailyKeyPairIssuer.getIssuer().publicHDSKPToPublicKey();
            byte[] signedData = concatenate(
                    dailyKeyPairIssuer.getDailyKeyPair().getEncodedKeyId(),
                    dailyKeyPairIssuer.getDailyKeyPair().getEncodedCreatedAt(),
                    decodeFromString(dailyKeyPairIssuer.getDailyKeyPair().getPublicKey()).blockingGet()
            ).blockingGet();

            byte[] signature = decodeFromString(dailyKeyPairIssuer.getDailyKeyPair().getSignature()).blockingGet();

            return signatureProvider.verify(signedData, signature, issuerSigningKey);
        });
    }

    /**
     * Fetch {@link DailyKeyPairPublicKeyWrapper} from server, verify its authenticity and ensure
     * its less than 7 days old.
     */
    private Single<DailyKeyPairPublicKeyWrapper> fetchDailyKeyPairPublicKeyWrapperFromBackend() {
        return getDailyKeyPair()
                .flatMap(dailyKeyPairIssuer -> Single.fromCallable(() -> {
                    byte[] encodedPublicKey = decodeFromString(dailyKeyPairIssuer.getDailyKeyPair().getPublicKey()).blockingGet();
                    ECPublicKey publicKey = AsymmetricCipherProvider.decodePublicKey(encodedPublicKey).blockingGet();

                    long creationUnixTimestamp = dailyKeyPairIssuer.getDailyKeyPair().getCreatedAt();
                    byte[] encodedCreationTimestamp = TimeUtil.encodeUnixTimestamp(creationUnixTimestamp).blockingGet();

                    int id = dailyKeyPairIssuer.getDailyKeyPair().getKeyId();
                    byte[] encodedId = ByteBuffer.allocate(4)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(id)
                            .array();

                    PublicKey issuerSigningKey = dailyKeyPairIssuer.getIssuer().publicHDSKPToPublicKey();
                    byte[] signature = decodeFromString(dailyKeyPairIssuer.getDailyKeyPair().getSignature()).blockingGet();
                    byte[] signedData = concatenate(encodedId, encodedCreationTimestamp, encodedPublicKey).blockingGet();

                    signatureProvider.verify(signedData, signature, issuerSigningKey).blockingAwait();

                    long keyAge = TimeUtil.convertFromUnixTimestamp(creationUnixTimestamp)
                            .map(creationTimestamp -> System.currentTimeMillis() - creationTimestamp)
                            .blockingGet();

                    if (keyAge > TimeUnit.DAYS.toMillis(7)) {
                        throw new IllegalStateException("Daily key pair public key is older than 7 days");
                    }

                    DailyKeyPairPublicKeyWrapper wrapper = new DailyKeyPairPublicKeyWrapper();
                    wrapper.setId(id);
                    wrapper.setPublicKey(publicKey);
                    return wrapper;
                })).doOnSuccess(wrapper -> Timber.d("Fetched daily key pair public key from backend: %s", wrapper));
    }

    /**
     * Restore {@link DailyKeyPairPublicKeyWrapper} if available.
     */
    private Maybe<DailyKeyPairPublicKeyWrapper> restoreDailyKeyPairPublicKeyWrapper() {
        Maybe<Integer> restoreId = preferencesManager.restoreIfAvailable(DAILY_KEY_PAIR_PUBLIC_KEY_ID_KEY, Integer.class);
        Maybe<ECPublicKey> restoreKey = preferencesManager.restoreIfAvailable(DAILY_KEY_PAIR_PUBLIC_KEY_POINT_KEY, String.class)
                .flatMapSingle(CryptoManager::decodeFromString)
                .flatMapSingle(AsymmetricCipherProvider::decodePublicKey);
        return Maybe.zip(restoreId, restoreKey, DailyKeyPairPublicKeyWrapper::new);
    }

    /**
     * Persist given daily public key wrapper to preferences.
     *
     * @param wrapper {@link DailyKeyPairPublicKeyWrapper}
     */
    protected Completable persistDailyKeyPairPublicKeyWrapper(@NonNull DailyKeyPairPublicKeyWrapper wrapper) {
        Completable persistId = preferencesManager.persist(DAILY_KEY_PAIR_PUBLIC_KEY_ID_KEY, wrapper.getId());
        Completable persistKey = AsymmetricCipherProvider.encode(wrapper.getPublicKey(), false)
                .flatMap(CryptoManager::encodeToString)
                .flatMapCompletable(encodedPublicKey -> preferencesManager.persist(DAILY_KEY_PAIR_PUBLIC_KEY_POINT_KEY, encodedPublicKey));
        return Completable.mergeArray(persistId, persistKey);
    }

    /*
        Guest key pair
     */

    /**
     * Get guest key pair or create if not yet generated. The keypair’s private key is used to sign
     * the encrypted guest data and guest data transfer object. The public key is uploaded to the
     * luca Server.
     */
    public Single<KeyPair> getGuestKeyPair() {
        return restoreGuestKeyPair()
                .switchIfEmpty(generateGuestKeyPair()
                        .observeOn(Schedulers.io())
                        .flatMap(guestKeyPair -> persistGuestKeyPair(guestKeyPair)
                                .andThen(Single.just(guestKeyPair))));
    }

    public Single<ECPrivateKey> getGuestKeyPairPrivateKey() {
        return getGuestKeyPair().map(KeyPair::getPrivate)
                .cast(ECPrivateKey.class);
    }

    public Single<ECPublicKey> getGuestKeyPairPublicKey() {
        return getGuestKeyPair().map(KeyPair::getPublic)
                .cast(ECPublicKey.class);
    }

    /**
     * Generate guest key pair, used to sign encrypted {@link MeetingGuestData}.
     *
     * @see <a href="https://luca-app.de/securityoverview/properties/secrets.html#term-guest-keypair">Security
     * Overview: Secrets</a>
     */
    private Single<KeyPair> generateGuestKeyPair() {
        return asymmetricCipherProvider.generateKeyPair(ALIAS_GUEST_KEY_PAIR, context)
                .doOnSuccess(guestKeyPair -> Timber.d("Generated new guest key pair: %s", guestKeyPair.getPublic()));
    }

    /**
     * Restore Guest key pair from {@link #bouncyCastleKeyStore} if available.
     */
    private Maybe<KeyPair> restoreGuestKeyPair() {
        return asymmetricCipherProvider.getKeyPairIfAvailable(ALIAS_GUEST_KEY_PAIR);
    }

    /**
     * Persist given guest keypair to {@link #bouncyCastleKeyStore}.
     */
    private Completable persistGuestKeyPair(@NonNull KeyPair keyPair) {
        return asymmetricCipherProvider.setKeyPair(ALIAS_GUEST_KEY_PAIR, keyPair)
                .andThen(persistKeyStoreToFile());
    }

    /*
        User ephemeral key pair
     */

    /**
     * Get or generate user ephemeral keypair used to encrypt user ID and secret during generation
     * of QR-codes.
     *
     * @see CheckInViewModel#generateQrCodeData()
     */
    public Single<KeyPair> getUserEphemeralKeyPair(@NonNull byte[] traceId) {
        return restoreUserEphemeralKeyPair(traceId)
                .switchIfEmpty(generateUserEphemeralKeyPair(traceId)
                        .observeOn(Schedulers.io())
                        .flatMap(keyPair -> persistUserEphemeralKeyPair(traceId, keyPair)
                                .andThen(Single.just(keyPair))));
    }

    public Single<PrivateKey> getUserEphemeralPrivateKey(@NonNull byte[] traceId) {
        return getUserEphemeralKeyPair(traceId).map(KeyPair::getPrivate);
    }

    public Single<PublicKey> getUserEphemeralPublicKey(@NonNull byte[] traceId) {
        return getUserEphemeralKeyPair(traceId).map(KeyPair::getPublic);
    }

    /**
     * Generate keypair of given traceId.
     */
    private Single<KeyPair> generateUserEphemeralKeyPair(@NonNull byte[] traceId) {
        return getUserEphemeralKeyPairAlias(traceId)
                .flatMap(alias -> asymmetricCipherProvider.generateKeyPair(alias, context))
                .doOnSuccess(keyPair -> Timber.d("Generated new user ephemeral key pair for trace ID %s: %s", SerializationUtil.serializeToBase64(traceId).blockingGet(), keyPair.getPublic()));
    }

    private Maybe<KeyPair> restoreUserEphemeralKeyPair(@NonNull byte[] traceId) {
        return getUserEphemeralKeyPairAlias(traceId)
                .flatMapMaybe(asymmetricCipherProvider::getKeyPairIfAvailable);
    }

    /**
     * Persist given keypair to BC keystore.
     */
    private Completable persistUserEphemeralKeyPair(@NonNull byte[] traceId,
                                                    @NonNull KeyPair keyPair) {
        return getUserEphemeralKeyPairAlias(traceId)
                .flatMapCompletable(alias -> asymmetricCipherProvider.setKeyPair(alias, keyPair))
                .andThen(persistKeyStoreToFile());
    }

    private Completable deleteUserEphemeralKeyPair(@NonNull byte[] traceId) {
        return getUserEphemeralKeyPairAlias(traceId)
                .flatMapCompletable(bouncyCastleKeyStore::deleteEntry);
    }

    private static Single<String> getUserEphemeralKeyPairAlias(@NonNull byte[] traceId) {
        return SerializationUtil.serializeToBase64(traceId)
                .map(serializedTraceId -> ALIAS_USER_EPHEMERAL_KEY_PAIR + "-" + serializedTraceId);
    }

    /*
        Tracing secrets
     */

    /**
     * Get or create tracing secret - a randomly generated seed used to derive trace IDs when
     * checking in using the Guest App. It is stored locally on the Guest App until it is shared
     * with the Health Department during contact tracing. Moreover, the tracing secret is rotated on
     * a regular basis in order to limit the number of trace IDs that can be reconstructed when the
     * secret is shared
     * <br>
     * Rotation of the daily tracing secret is ensured by the method either restoring today's secret
     * or generating (and persisting) a new one for today.
     *
     * @see <a href="https://www.luca-app.de/securityoverview/properties/secrets.html#term-tracing-secret">Security
     * Overview: Secrets</a>
     * @see <a href="https://luca-app.de/securityoverview/processes/guest_registration.html#rotating-the-tracing-secret">Security
     * Overview: Rotating the Tracing Secret</a>
     */
    public Single<byte[]> getCurrentTracingSecret() {
        return restoreCurrentTracingSecret()
                .switchIfEmpty(generateTracingSecret()
                        .observeOn(Schedulers.io())
                        .flatMap(secret -> persistCurrentTracingSecret(secret)
                                .andThen(Single.just(secret))));
    }

    private Single<byte[]> generateTracingSecret() {
        return generateSecureRandomData(TRIMMED_HASH_LENGTH)
                .doOnSuccess(bytes -> Timber.d("Generated new tracing secret"));
    }

    /**
     * Restore only the recent most tracing secret, meaning today's secret.
     *
     * @return today's tracing secret if available
     */
    private Maybe<byte[]> restoreCurrentTracingSecret() {
        return restoreRecentTracingSecrets(0)
                .map(pair -> pair.second)
                .firstElement();
    }

    /**
     * Persist given tracing secret to preferences, encrypted as a {@link WrappedSecret}.
     */
    private Completable persistCurrentTracingSecret(@NonNull byte[] secret) {
        return TimeUtil.getStartOfCurrentDayTimestamp()
                .map(startOfDayTimestamp -> TRACING_SECRET_KEY_PREFIX + startOfDayTimestamp)
                .flatMapCompletable(preferenceKey -> persistWrappedSecret(preferenceKey, secret));
    }

    public Observable<Pair<Long, byte[]>> restoreRecentTracingSecrets(long duration) {
        return generateRecentStartOfDayTimestamps(duration)
                .flatMapMaybe(startOfDayTimestamp -> restoreWrappedSecretIfAvailable(TRACING_SECRET_KEY_PREFIX + startOfDayTimestamp)
                        .map(secret -> new Pair<>(startOfDayTimestamp, secret)));
    }

    public Observable<Long> generateRecentStartOfDayTimestamps(long duration) {
        return TimeUtil.getStartOfCurrentDayTimestamp()
                .flatMapObservable(firstStartOfDayTimestamp -> Observable.range(0, (int) TimeUnit.MILLISECONDS.toDays(duration) + 1)
                        .map(dayIndex -> firstStartOfDayTimestamp - TimeUnit.DAYS.toMillis(dayIndex)));
    }

    /*
        User data secret
     */

    /**
     * Get or generate data secret - a cryptographic seed which is used to derive both the data
     * encryption key and the data authentication key. This seed is encrypted twice before being
     * sent to the Luca Server during Check-In and ultimately protects the Guest’s Contact Data.
     *
     * @see <a href="https://www.luca-app.de/securityoverview/properties/secrets.html#term-data-secret">Security
     * Overview: Secrets</a>
     */
    public Single<byte[]> getDataSecret() {
        return restoreDataSecret()
                .switchIfEmpty(generateDataSecret()
                        .observeOn(Schedulers.io())
                        .flatMap(secret -> persistDataSecret(secret)
                                .andThen(Single.just(secret))));
    }

    public Single<byte[]> generateDataSecret() {
        return generateSecureRandomData(TRIMMED_HASH_LENGTH)
                .doOnSuccess(bytes -> Timber.d("Generated new user data secret"));
    }

    private Maybe<byte[]> restoreDataSecret() {
        return restoreWrappedSecretIfAvailable(DATA_SECRET_KEY);
    }

    /**
     * Persist data secret to preferences, encrypted as a {@link WrappedSecret}.
     */
    private Completable persistDataSecret(@NonNull byte[] secret) {
        return persistWrappedSecret(DATA_SECRET_KEY, secret);
    }

    /*
        Shared Diffie-Hellman secret
     */

    /**
     * Convenience method for {@link #generateSharedDiffieHellmanSecret()}.
     */
    public Single<byte[]> getSharedDiffieHellmanSecret() {
        return generateSharedDiffieHellmanSecret();
    }

    /**
     * Compute shared DH secret of {@link DailyKeyPairPublicKeyWrapper} and the guest private key.
     */
    public Single<byte[]> generateSharedDiffieHellmanSecret() {
        return getGuestKeyPairPrivateKey()
                .flatMap(this::generateSharedDiffieHellmanSecret);
    }

    /**
     * Compute shared DH secret of {@link DailyKeyPairPublicKeyWrapper} and a given private key.
     *
     * @return shared DH secret
     */
    public Single<byte[]> generateSharedDiffieHellmanSecret(@NonNull PrivateKey privateKey) {
        return getDailyKeyPairPublicKeyWrapper()
                .map(DailyKeyPairPublicKeyWrapper::getPublicKey)
                .flatMap(dailyKeyPairPublicKey -> asymmetricCipherProvider.generateSecret(privateKey, dailyKeyPairPublicKey))
                .doOnSuccess(bytes -> Timber.d("Generated new shared Diffie Hellman secret"));
    }

    /*
        Encryption secret
     */

    /**
     * Generate data encryption secret by appending {@link #DATA_ENCRYPTION_SECRET_SUFFIX} to given
     * baseSecret, hashing it and trimming it to a length of 16.
     */
    public Single<byte[]> generateDataEncryptionSecret(@NonNull byte[] baseSecret) {
        return concatenate(baseSecret, DATA_ENCRYPTION_SECRET_SUFFIX)
                .flatMap(hashProvider::hash)
                .flatMap(secret -> trim(secret, TRIMMED_HASH_LENGTH));
    }

    /*
        Authentication secret
     */

    /**
     * Generate data authentication secret by appending {@link #DATA_AUTHENTICATION_SECRET_SUFFIX}
     * to given baseSecret and hash it.
     */
    public Single<byte[]> generateDataAuthenticationSecret(@NonNull byte[] baseSecret) {
        return concatenate(baseSecret, DATA_AUTHENTICATION_SECRET_SUFFIX)
                .flatMap(hashProvider::hash);
    }

    /*
        Scanner
     */

    /**
     * Get previously persisted scanner ephemeral key - used during self check-in to complete
     * re-encryption of {@link CheckInRequestData} usually performed by the Scanner Frontend.
     *
     * @see <a href="https://luca-app.de/securityoverview/processes/guest_self_checkin.html">Security
     * Overview: Check-In via a Printed QR Code</a>
     */
    public Single<KeyPair> getScannerEphemeralKeyPair() {
        return asymmetricCipherProvider.getKeyPair(ALIAS_SCANNER_EPHEMERAL_KEY_PAIR);
    }

    /**
     * Generate a new scanner ephemeral key - used during self check-in to re-encrypt {@link
     * CheckInRequestData}.
     *
     * @see CheckInManager#generateCheckInData(QrCodeData, UUID)
     */
    public Single<KeyPair> generateScannerEphemeralKeyPair() {
        return asymmetricCipherProvider.generateKeyPair(ALIAS_SCANNER_EPHEMERAL_KEY_PAIR, context)
                .doOnSuccess(keyPair -> Timber.d("Generated new scanner ephemeral key pair: %s", keyPair.getPublic()));
    }

    /**
     * Persist given scanner ephemeral keypair to BC keystore.
     *
     * @param keyPair ephemeral scanner key to persist
     */
    public Completable persistScannerEphemeralKeyPair(@NonNull KeyPair keyPair) {
        return asymmetricCipherProvider.setKeyPair(ALIAS_SCANNER_EPHEMERAL_KEY_PAIR, keyPair)
                .andThen(persistKeyStoreToFile());
    }

    /*
        Meeting
     */

    public Single<KeyPair> getMeetingEphemeralKeyPair(@NonNull UUID meetingId) {
        return restoreMeetingEphemeralKeyPair(meetingId)
                .switchIfEmpty(generateMeetingEphemeralKeyPair()
                        .flatMap(keyPair -> persistMeetingEphemeralKeyPair(meetingId, keyPair)
                                .andThen(Single.just(keyPair))));
    }

    public Single<PrivateKey> getMeetingEphemeralPrivateKey(@NonNull UUID meetingId) {
        return getMeetingEphemeralKeyPair(meetingId).map(KeyPair::getPrivate);
    }

    public Single<PublicKey> getMeetingEphemeralPublicKey(@NonNull UUID meetingId) {
        return getMeetingEphemeralKeyPair(meetingId).map(KeyPair::getPublic);
    }

    public Single<KeyPair> generateMeetingEphemeralKeyPair() {
        return asymmetricCipherProvider.generateKeyPair(ALIAS_MEETING_EPHEMERAL_KEY_PAIR, context)
                .doOnSuccess(keyPair -> Timber.d("Generated new meeting ephemeral key pair: %s", keyPair.getPublic()));
    }

    public Maybe<KeyPair> restoreMeetingEphemeralKeyPair(@NonNull UUID meetingId) {
        return getMeetingEphemeralKeyPairAlias(meetingId)
                .flatMapMaybe(asymmetricCipherProvider::getKeyPairIfAvailable);
    }

    public Completable persistMeetingEphemeralKeyPair(@NonNull UUID meetingId, @NonNull KeyPair
            keyPair) {
        return getMeetingEphemeralKeyPairAlias(meetingId)
                .flatMapCompletable(alias -> asymmetricCipherProvider.setKeyPair(alias, keyPair))
                .andThen(persistKeyStoreToFile());
    }

    public Completable deleteMeetingEphemeralKeyPair(@NonNull UUID meetingId) {
        return getMeetingEphemeralKeyPairAlias(meetingId)
                .flatMapCompletable(bouncyCastleKeyStore::deleteEntry);
    }

    public static Single<String> getMeetingEphemeralKeyPairAlias(@NonNull UUID meetingId) {
        return Single.just(ALIAS_MEETING_EPHEMERAL_KEY_PAIR + "-" + meetingId.toString());
    }

    /*
        Utilities
     */

    public Single<byte[]> generateSecureRandomData(int length) {
        return Single.fromCallable(() -> {
            byte[] randomBytes = new byte[length];
            secureRandom.nextBytes(randomBytes);
            return randomBytes;
        });
    }

    public static Single<SecretKey> createKeyFromSecret(@NonNull byte[] secret) {
        return Single.just(new SecretKeySpec(secret, 0, secret.length, "AES"));
    }

    public static Single<byte[]> encode(@NonNull UUID uuid) {
        return Single.fromCallable(() -> {
            ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
            byteBuffer.putLong(uuid.getMostSignificantBits());
            byteBuffer.putLong(uuid.getLeastSignificantBits());
            return byteBuffer.array();
        });
    }

    public static Single<String> encodeToString(@NonNull byte[] data) {
        return RxBase64.encode(data, Base64.NO_WRAP);
    }

    public static Single<byte[]> decodeFromString(@NonNull String data) {
        return RxBase64.decode(data, Base64.NO_WRAP);
    }

    public static Single<byte[]> concatenate(byte[]... dataArray) {
        return Single.fromCallable(() -> {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (byte[] data : dataArray) {
                outputStream.write(data);
            }
            return outputStream.toByteArray();
        });
    }

    public static Single<byte[]> trim(byte[] data, int length) {
        return Single.fromCallable(() -> {
            byte[] trimmedData = new byte[length];
            System.arraycopy(data, 0, trimmedData, 0, length);
            return trimmedData;
        });
    }

    public static byte[] toDERSignature(byte[] tokenSignature) throws IOException {
        byte[] r = Arrays.copyOfRange(tokenSignature, 0, tokenSignature.length / 2);
        byte[] s = Arrays.copyOfRange(tokenSignature, tokenSignature.length / 2, tokenSignature.length);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ASN1OutputStream derOutputStream = ASN1OutputStream.create(byteArrayOutputStream, ASN1Encoding.DER);
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(new ASN1Integer(new BigInteger(1, r)));
        v.add(new ASN1Integer(new BigInteger(1, s)));
        derOutputStream.writeObject(new DERSequence(v));

        derOutputStream.close();
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Delete data stored in the {@link #androidKeyStore} and {@link #bouncyCastleKeyStore}.
     */
    public Completable deleteAllKeyStoreEntries() {
        return androidKeyStore.deleteAllEntries()
                .andThen(bouncyCastleKeyStore.deleteAllEntries());
    }

    /*
        Getter & Setter
     */

    public RxKeyStore getBouncyCastleKeyStore() {
        return bouncyCastleKeyStore;
    }

    public SymmetricCipherProvider getSymmetricCipherProvider() {
        return symmetricCipherProvider;
    }

    public AsymmetricCipherProvider getAsymmetricCipherProvider() {
        return asymmetricCipherProvider;
    }

    public SignatureProvider getSignatureProvider() {
        return signatureProvider;
    }

    public MacProvider getMacProvider() {
        return macProvider;
    }

    public RxHashProvider getHashProvider() {
        return hashProvider;
    }

}