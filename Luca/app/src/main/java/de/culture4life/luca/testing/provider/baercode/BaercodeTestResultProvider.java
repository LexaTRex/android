package de.culture4life.luca.testing.provider.baercode;

import de.culture4life.luca.BuildConfig;
import de.culture4life.luca.testing.TestResultExpiredException;
import de.culture4life.luca.testing.TestResultImportException;
import de.culture4life.luca.testing.TestResultParsingException;
import de.culture4life.luca.testing.provider.TestResultProvider;
import de.culture4life.luca.util.SerializationUtil;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.core.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class BaercodeTestResultProvider extends TestResultProvider<BaercodeTestResult> {

    private static final String BUNDLE_ENDPOINT = "https://s3-de-central.profitbricks.com/baercode/bundle.cose";
    private static final String CERTIFICATE_ENDPOINT = "https://s3-de-central.profitbricks.com/baercode/ba.crt";
    protected static BaercodeBundle baercodeBundle;
    protected static BaercodeCertificate baercodeCertificate;
    private static OkHttpClient client;

    public BaercodeTestResultProvider() {
        client = new OkHttpClient.Builder()
                .callTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Check if a given byte array can be parsed as a baercode. This does not decrypt the message or
     * run validity checks.
     *
     * @param encodedData encoded data to check
     */
    @Override
    public Single<Boolean> canParse(@NonNull String encodedData) {
        return SerializationUtil.deserializeFromBase64(encodedData)
                .map(data -> {
                    try {
                        int version = CoseMessage.MAPPER.readValue(data, Integer.class);
                        if (version == BaercodeTestResult.PROTOCOL_VERSION) {
                            byte[] cborEncodedData = Arrays.copyOfRange(data, 2, data.length);
                            new CoseMessage(cborEncodedData);
                            return true;
                        }
                    } catch (Exception e) {
                        Timber.d("Data is not a readable BaercodeTestResult");
                    }
                    return false;
                }).onErrorReturnItem(false);
    }

    @Override
    public Single<BaercodeTestResult> parse(@NonNull String encodedData) {
        return SerializationUtil.deserializeFromBase64(encodedData)
                .map(bytes -> {
                    BaercodeTestResult testResult = new BaercodeTestResult(bytes);
                    decryptPersonalData(testResult);
                    return testResult;
                }).onErrorResumeNext(throwable -> {
                    if (throwable instanceof TestResultParsingException || throwable instanceof TestResultImportException) {
                        return Single.error(throwable);
                    }
                    return Single.error(new TestResultParsingException(throwable));
                });
    }

    protected static BaercodeBundle downloadBundle() throws IOException {
        return new BaercodeBundle(download(BUNDLE_ENDPOINT));
    }

    protected static BaercodeCertificate downloadCertificate() throws IOException, CertificateException {
        return new BaercodeCertificate(download(CERTIFICATE_ENDPOINT));
    }

    protected static @NotNull byte[] download(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            return response.body().bytes();
        } else {
            throw new IOException(String.format("Could not download bundle: %s", response.message()));
        }
    }

    protected void decryptPersonalData(@NonNull BaercodeTestResult testResult) throws IOException, TestResultParsingException, CertificateException, TestResultExpiredException {
        if (baercodeBundle == null || (baercodeBundle.isExpired() && !BuildConfig.DEBUG)) {
            baercodeBundle = downloadBundle();
            baercodeCertificate = downloadCertificate();
        }
        baercodeBundle.verify(baercodeCertificate.getPublicKey());
        BaercodeKey baercodeKey = baercodeBundle.getKey(testResult.getBase64KeyId());
        if (baercodeKey == null) {
            throw new TestResultExpiredException("No valid key found in bundle");
        }
        testResult.verifyAndDecryptPersonalData(baercodeKey);
    }

    protected static ECPublicKey createPublicKey(@NonNull BaercodeKey baercodeKey) throws NoSuchAlgorithmException, InvalidParameterSpecException, InvalidKeySpecException {
        ECPoint pubPoint = new ECPoint(new BigInteger(baercodeKey.getxCoordinate()), new BigInteger(baercodeKey.getyCoordinate()));
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp521r1"));
        ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
        ECPublicKeySpec pubSpec = new ECPublicKeySpec(pubPoint, ecParameters);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return (ECPublicKey) kf.generatePublic(pubSpec);
    }

}
