package de.culture4life.luca.crypto;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import com.nexenio.rxkeystore.util.RxBase64;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.culture4life.luca.genuinity.GenuinityManager;
import de.culture4life.luca.genuinity.NoGenuineTimeException;
import de.culture4life.luca.network.NetworkManager;
import de.culture4life.luca.network.endpoints.LucaEndpointsV3;
import de.culture4life.luca.network.pojo.DailyKeyPair;
import de.culture4life.luca.network.pojo.DailyKeyPairIssuer;
import de.culture4life.luca.network.pojo.Issuer;
import de.culture4life.luca.preference.PreferencesManager;
import io.reactivex.rxjava3.core.Single;

import static de.culture4life.luca.crypto.AsymmetricCipherProviderTest.decodePrivateKey;
import static de.culture4life.luca.crypto.AsymmetricCipherProviderTest.decodePublicKey;
import static de.culture4life.luca.history.HistoryManager.SHARE_DATA_DURATION;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class CryptoManagerTest {

    private static final UUID USER_ID = UUID.fromString("02fb635c-f6a5-48eb-8379-a83d611618f2");
    private static final String ENCODED_DAILY_KEY_PAIR_PUBLIC_KEY = "BAIDQ7/zTOcV+XXX5io9XZn1t4MUOAswVfZKd6Fpup/MwlNssx4mCEPcO34AIiV0TbL2ywOP3QoHs41cfvv7uTo=";
    private static final String ENCODED_TRACE_SECRET = "dZrDSp83PCcVL5ZvsJypwA==";
    private static final String ENCODED_GUEST_KEY_PAIR_PRIVATE_KEY = "JwlHQ8w3GjM6T94PwgltA7PNvCk1xokk8HcqXG0CXBI=";
    private static final String ENCODED_GUEST_KEY_PAIR_PUBLIC_KEY = "BIMFVAOglk1B4PIlpaVspeWeFwO5eUusqxFAUUDFNJYGpbp9iu0jRHQAipDTVgFSudcm9tF5kh4+wILrAm3vHWg=";
    private static final String ENCODED_SHARED_DH_SECRET = "cSPbpq56ygtUX0TayiRw0KJpaeoNS/3dcNljtndAXaE=";
    private static final String ENCODED_TRACE_ID = "Z0aw+vjwazzQHj21PxmWTQ==";

    private static final DailyKeyPair DAILY_KEY_PAIR = new DailyKeyPair(
            1,
            1619792191,
            "f929a574-c358-43d7-b5b1-7b06c19f4ef7",
            "BCibpFVfGPAJI9yhWd9NGGYEU3//fn0Lu59yAtTIP59L9eeK8FhSJWBT8cr1I7+gLy9pykGK537joQIF4BZNmPM=",
            "MEQCICNDdj3sXL1OLvJk3Fc8/y5wiwPxZnwDF90wsaT7DRpsAiAyYfYbqCHLwxPelorvmOmJfIy/f6m8M+j67HUkHfdkYQ=="
    );

    private static final Issuer ISSUER = new Issuer(
            "f929a574-c358-43d7-b5b1-7b06c19f4ef7",
            "Gesundheitsamt Kre",
            "BCJQFrdqX5E+zud5HVK7rHUX35wxD4RjRM3FlfYQeg3qMM++C/K/rOhgR5U9mRHbg9wxipZvS0scuXNvMHkmIrs=",
            "BJGBT0vYL53gzK8WoWzg6ub2BIqvYPwquc9EnTYs+ZAabPSxlc9hL2H0M8xWM9oSepl56sTG6HpGAVQ7fChlf54="
    );

    private CryptoManager cryptoManager;
    private NetworkManager networkManager;
    private GenuinityManager genuinityManager;

    @Before
    public void setup() {
        PreferencesManager preferencesManager = new PreferencesManager();
        networkManager = spy(new NetworkManager());
        genuinityManager = spy(new GenuinityManager(preferencesManager, networkManager));
        cryptoManager = spy(new CryptoManager(preferencesManager, networkManager, genuinityManager));
        cryptoManager.initialize(ApplicationProvider.getApplicationContext()).blockingAwait();

        Single<Long> getCurrentTime = Single.fromCallable(System::currentTimeMillis);
        doReturn(getCurrentTime).when(genuinityManager).fetchServerTime();
    }

    private void mockNetworkResponses(DailyKeyPair dailyKeyPair, Issuer issuer) {
        LucaEndpointsV3 lucaEndpointsV3 = Mockito.mock(LucaEndpointsV3.class);
        Single<DailyKeyPair> response = Single.just(dailyKeyPair);
        doReturn(response).when(lucaEndpointsV3).getDailyKeyPair();
        Single<Issuer> issuerResponse = Single.just(issuer);
        doReturn(issuerResponse).when(lucaEndpointsV3).getIssuer(Mockito.anyString());
        Single<LucaEndpointsV3> fakeEndpoint = Single.just(lucaEndpointsV3);
        doReturn(fakeEndpoint).when(networkManager).getLucaEndpointsV3();
    }

    @Test
    public void getTraceIdWrapper_generateNew_isNotEmpty() {
        doReturn(Single.just(decodeSecret(ENCODED_TRACE_SECRET)))
                .when(cryptoManager).getCurrentTracingSecret();
        cryptoManager.getTraceIdWrapper(USER_ID)
                .map(traceIdWrapper -> traceIdWrapper.getTraceId().length > 0)
                .test()
                .assertValue(true);
    }

    @Test
    public void getTraceIdWrappers_afterGenerateTraceId_isNotEmpty() {
        doReturn(Single.just(decodeSecret(ENCODED_TRACE_SECRET)))
                .when(cryptoManager).getCurrentTracingSecret();
        cryptoManager.generateTraceId(USER_ID, 1601481600L)
                .ignoreElement()
                .andThen(cryptoManager.getTraceIdWrappers())
                .toList()
                .test()
                .assertValueCount(1);
    }

    @Test
    public void getTraceIdWrappers_afterDeleteTraceData_isEmpty() {
        cryptoManager.generateTraceId(USER_ID, 1601481600L)
                .ignoreElement()
                .andThen(cryptoManager.deleteTraceData())
                .andThen(cryptoManager.getTraceIdWrappers())
                .toList()
                .test()
                .assertEmpty();
    }

    @Test
    public void generateTraceId() {
        doReturn(Single.just(decodeSecret(ENCODED_TRACE_SECRET)))
                .when(cryptoManager).getCurrentTracingSecret();
        cryptoManager.generateTraceId(USER_ID, 1601481600L)
                .flatMap(CryptoManager::encodeToString)
                .test()
                .assertValue(ENCODED_TRACE_ID);
    }

    @Ignore("Hardcoded key expired")
    @Test
    public void updateDailyKeyPairPublicKey_withBackendMock_persistsDailyKeyPair() {
        mockNetworkResponses(DAILY_KEY_PAIR, ISSUER);
        cryptoManager.updateDailyKeyPairPublicKey().blockingAwait();
        Mockito.verify(cryptoManager).persistDailyKeyPairPublicKeyWrapper(Mockito.any(DailyKeyPairPublicKeyWrapper.class));
    }

    @Ignore("Hardcoded key expired")
    @Test
    public void getDailyKeyPairPublicKeyWrapper_publicKey_usesEcdsa() {
        mockNetworkResponses(DAILY_KEY_PAIR, ISSUER);
        cryptoManager.updateDailyKeyPairPublicKey()
                .andThen(cryptoManager.getDailyKeyPairPublicKeyWrapper())
                .map(dailyKeyPairPublicKeyWrapper -> dailyKeyPairPublicKeyWrapper.getPublicKey().getAlgorithm())
                .test()
                .assertValue("ECDSA");
    }

    @Test
    public void getDailyKeyPairPublicKeyWrapper_persistedPublicKeyIsFromDailyKeyPair_isValid() throws InterruptedException {
        DailyKeyPairIssuer dailyKeyPairIssuer =
                CryptoTestHelper.INSTANCE.createDailyKeyPairAndIssuer(
                        1,
                        System.currentTimeMillis() / 1000,
                        "SOME_UNIQUE_ISSUER_ID",
                        "SOME_ISSUER_NAME"
                );

        mockNetworkResponses(dailyKeyPairIssuer.getDailyKeyPair(), dailyKeyPairIssuer.getIssuer());

        cryptoManager.updateDailyKeyPairPublicKey()
                .andThen(cryptoManager.getDailyKeyPairPublicKeyWrapper())
                .map(DailyKeyPairPublicKeyWrapper::getPublicKey)
                .flatMap(AsymmetricCipherProvider::encode)
                .flatMap(encodedPublicKey -> RxBase64.encode(encodedPublicKey, Base64.NO_WRAP))
                .test()
                .await()
                .assertValue(dailyKeyPairIssuer.getDailyKeyPair().getPublicKey());
    }

    @Test
    public void updateDailyKeyPairPublicKey_validKeyPair_verifySuccess() throws InterruptedException {
        DailyKeyPairIssuer dailyKeyPairIssuer =
                CryptoTestHelper.INSTANCE.createDailyKeyPairAndIssuer(
                        1,
                        System.currentTimeMillis() / 1000,
                        "SOME_UNIQUE_ISSUER_ID",
                        "SOME_ISSUER_NAME"
                );

        mockNetworkResponses(dailyKeyPairIssuer.getDailyKeyPair(), dailyKeyPairIssuer.getIssuer());

        cryptoManager.updateDailyKeyPairPublicKey()
                .test()
                .await()
                .assertComplete();
    }

    @Test
    public void updateDailyKeyPairPublicKey_noGenuineTime_verifyFails() throws InterruptedException {
        mockNetworkResponses(DAILY_KEY_PAIR, ISSUER);
        doReturn(Single.just(false)).when(genuinityManager).isGenuineTime();

        cryptoManager.updateDailyKeyPairPublicKey()
                .test()
                .await()
                .assertError(NoGenuineTimeException.class);
    }

    @Test
    public void updateDailyKeyPairPublicKey_createdAtOlderThanSevenDays_verifyFails() throws InterruptedException {
        DailyKeyPairIssuer dailyKeyPairIssuer =
                CryptoTestHelper.INSTANCE.createDailyKeyPairAndIssuer(
                        1,
                        (System.currentTimeMillis() / 1000) - ((60 * 60 * 24 * 7) + 60),
                        "SOME_UNIQUE_ISSUER_ID",
                        "SOME_ISSUER_NAME"
                );

        mockNetworkResponses(dailyKeyPairIssuer.getDailyKeyPair(), dailyKeyPairIssuer.getIssuer());

        cryptoManager.updateDailyKeyPairPublicKey()
                .test()
                .await()
                .assertError(DailyKeyExpiredException.class);
    }

    @Test
    public void updateDailyKeyPairPublicKey_signatureInvalid_verifyFails() throws InterruptedException {
        DailyKeyPairIssuer validDailyKeyPairIssuer = CryptoTestHelper.INSTANCE.createDailyKeyPairAndIssuer(
                1,
                System.currentTimeMillis() / 1000,
                "SOME_UNIQUE_ISSUER_ID",
                "SOME_ISSUER_NAME"
        );

        // manipulate the signature now
        DailyKeyPair manipulatedDailyKeyPair = new DailyKeyPair(
                validDailyKeyPairIssuer.getDailyKeyPair().getKeyId(),
                validDailyKeyPairIssuer.getDailyKeyPair().getCreatedAt(),
                validDailyKeyPairIssuer.getDailyKeyPair().getIssuerId(),
                validDailyKeyPairIssuer.getDailyKeyPair().getPublicKey(),
                Base64.encodeToString("SOME_INVALID_SIGNATURE".getBytes(), Base64.NO_WRAP)
        );

        mockNetworkResponses(manipulatedDailyKeyPair, validDailyKeyPairIssuer.getIssuer());

        cryptoManager.updateDailyKeyPairPublicKey()
                .test()
                .await()
                .assertError(RuntimeException.class);
    }

    @Test
    public void updateDailyKeyPairPublicKey_signedDataChanged_verifyFails() throws InterruptedException {
        DailyKeyPairIssuer validDailyKeyPairIssuer = CryptoTestHelper.INSTANCE.createDailyKeyPairAndIssuer(
                1,
                System.currentTimeMillis() / 1000,
                "SOME_UNIQUE_ISSUER_ID",
                "SOME_ISSUER_NAME"
        );

        // manipulate the data now
        DailyKeyPair manipulatedDailyKeyPair = new DailyKeyPair(
                1000, // manipulate keyId
                validDailyKeyPairIssuer.getDailyKeyPair().getCreatedAt(),
                validDailyKeyPairIssuer.getDailyKeyPair().getIssuerId(),
                validDailyKeyPairIssuer.getDailyKeyPair().getPublicKey(),
                validDailyKeyPairIssuer.getDailyKeyPair().getSignature()
        );

        mockNetworkResponses(manipulatedDailyKeyPair, validDailyKeyPairIssuer.getIssuer());

        cryptoManager.updateDailyKeyPairPublicKey()
                .test()
                .await()
                .assertError(RuntimeException.class);
    }

    @Test
    public void updateDailyKeyPairPublicKey_signaturePublicKeyInvalid_verifyFails() throws InterruptedException {
        DailyKeyPairIssuer validDailyKeyPairIssuer = CryptoTestHelper.INSTANCE.createDailyKeyPairAndIssuer(
                1,
                System.currentTimeMillis() / 1000,
                "SOME_UNIQUE_ISSUER_ID",
                "SOME_ISSUER_NAME"
        );

        // create new public key
        KeyPair newKeyPair = CryptoTestHelper.INSTANCE.createKeyPair();
        String newEncodedPublicKey =
                Base64.encodeToString(
                        AsymmetricCipherProvider.encode((ECPublicKey) newKeyPair.getPublic())
                                .blockingGet(),
                        Base64.NO_WRAP
                );

        // manipulate the public key now
        DailyKeyPair manipulatedDailyKeyPair = new DailyKeyPair(
                validDailyKeyPairIssuer.getDailyKeyPair().getKeyId(),
                validDailyKeyPairIssuer.getDailyKeyPair().getCreatedAt(),
                validDailyKeyPairIssuer.getDailyKeyPair().getIssuerId(),
                newEncodedPublicKey,
                validDailyKeyPairIssuer.getDailyKeyPair().getSignature()
        );

        mockNetworkResponses(manipulatedDailyKeyPair, validDailyKeyPairIssuer.getIssuer());

        cryptoManager.updateDailyKeyPairPublicKey()
                .test()
                .await()
                .assertError(RuntimeException.class);
    }

    @Test
    public void assertKeyNotExpired_validKey_completes() {
        DailyKeyPairPublicKeyWrapper key = new DailyKeyPairPublicKeyWrapper(
                0,
                decodePublicKey(ENCODED_DAILY_KEY_PAIR_PUBLIC_KEY),
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6)
        );
        cryptoManager.assertKeyNotExpired(key)
                .test()
                .assertNoErrors()
                .assertComplete();
    }

    @Test
    public void assertKeyNotExpired_expiredKey_emitsError() {
        DailyKeyPairPublicKeyWrapper key = new DailyKeyPairPublicKeyWrapper(
                0,
                decodePublicKey(ENCODED_DAILY_KEY_PAIR_PUBLIC_KEY),
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(8)
        );
        cryptoManager.assertKeyNotExpired(key)
                .test()
                .assertError(DailyKeyExpiredException.class);
    }

    @Test
    public void generateSharedDiffieHellmanSecret() {
        doReturn(Single.just(new DailyKeyPairPublicKeyWrapper(
                0,
                decodePublicKey(ENCODED_DAILY_KEY_PAIR_PUBLIC_KEY),
                System.currentTimeMillis()
        ))).when(cryptoManager).getDailyKeyPairPublicKeyWrapper();
        KeyPair userMasterKeyPair = new KeyPair(
                decodePublicKey(ENCODED_GUEST_KEY_PAIR_PUBLIC_KEY),
                decodePrivateKey(ENCODED_GUEST_KEY_PAIR_PRIVATE_KEY)
        );
        doReturn(Single.just(userMasterKeyPair))
                .when(cryptoManager).getGuestKeyPair();

        cryptoManager.generateSharedDiffieHellmanSecret()
                .flatMap(CryptoManager::encodeToString)
                .test()
                .assertValue(ENCODED_SHARED_DH_SECRET);
    }

    @Test
    public void generateScannerEphemeralKeyPair_publicKey_usesEc() {
        cryptoManager.generateScannerEphemeralKeyPair()
                .map(keyPair -> keyPair.getPublic().getAlgorithm())
                .test()
                .assertValue("EC");
    }

    @Test
    public void generateMeetingEphemeralKeyPair_publicKey_usesEc() {
        cryptoManager.generateMeetingEphemeralKeyPair()
                .map(keyPair -> keyPair.getPublic().getAlgorithm())
                .test()
                .assertValue("EC");
    }

    @Test
    public void encodeToString_decodeFromString_isSameAsInput() {
        String input = "abc123!§$%&/()=,.-*";
        CryptoManager.encodeToString(input.getBytes(StandardCharsets.UTF_8))
                .flatMap(CryptoManager::decodeFromString)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .test()
                .assertResult(input);
    }

    @Test
    public void testGenerateRecentStartOfDayTimestamps() {
        List<Long> test = cryptoManager.generateRecentStartOfDayTimestamps(SHARE_DATA_DURATION).toList().blockingGet();
        for (int i = 0; i < test.size() - 1; i++) {
            long diff = test.get(i) - test.get(i + 1);
            Assert.assertEquals(diff, TimeUnit.DAYS.toMillis(1));
        }
    }

    @Test
    public void encodeToString() {
        CryptoManager.encodeToString("AOU\nÄÖÜ".getBytes(StandardCharsets.UTF_8))
                .test()
                .assertResult("QU9VCsOEw5bDnA==");
    }

    @Test
    public void decodeFromString() {
        CryptoManager.decodeFromString("QU9VCsOEw5bDnA==")
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .test()
                .assertResult("AOU\nÄÖÜ");
    }

    public static byte[] decodeSecret(@NonNull String encodedSecret) {
        return CryptoManager.decodeFromString(encodedSecret).blockingGet();
    }

}