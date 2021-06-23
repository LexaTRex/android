package de.culture4life.luca.document.provider.baercode;

import android.content.Context;

import de.culture4life.luca.BuildConfig;
import de.culture4life.luca.document.DocumentExpiredException;
import de.culture4life.luca.document.DocumentImportException;
import de.culture4life.luca.document.DocumentParsingException;
import de.culture4life.luca.document.provider.DocumentProvider;
import de.culture4life.luca.util.SerializationUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
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

public class BaercodeDocumentProvider extends DocumentProvider<BaercodeDocument> {

    private static final String BUNDLE_ENDPOINT = "https://s3-de-central.profitbricks.com/baercode/bundle.cose";
    private static final String CERTIFICATE_ENDPOINT = "https://s3-de-central.profitbricks.com/baercode/ba.crt";
    protected static BaercodeBundle baercodeBundle;
    protected static BaercodeCertificate baercodeCertificate;
    private static OkHttpClient client;
    private final Context context;

    public BaercodeDocumentProvider(@NonNull Context context) {
        this.context = context;
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
                    int version = CoseMessage.MAPPER.readValue(data, Integer.class);
                    if (version != BaercodeDocument.PROTOCOL_VERSION) {
                        return false;
                    }
                    byte[] cborEncodedData = Arrays.copyOfRange(data, 2, data.length);
                    new CoseMessage(cborEncodedData);
                    return true;
                }).onErrorReturnItem(false);
    }

    @Override
    public Single<BaercodeDocument> parse(@NonNull String encodedData) {
        return SerializationUtil.deserializeFromBase64(encodedData)
                .map(bytes -> {
                    BaercodeDocument document = new BaercodeDocument(bytes);
                    decryptPersonalData(document);
                    return document;
                }).onErrorResumeNext(throwable -> {
                    if (throwable instanceof DocumentParsingException || throwable instanceof DocumentImportException) {
                        return Single.error(throwable);
                    }
                    return Single.error(new DocumentParsingException(throwable));
                });
    }

    protected static BaercodeBundle downloadBundle() throws IOException {
        return new BaercodeBundle(download(BUNDLE_ENDPOINT));
    }

    protected static BaercodeCertificate downloadCertificate() throws IOException, CertificateException {
        return new BaercodeCertificate(download(CERTIFICATE_ENDPOINT));
    }

    @NonNull
    protected static byte[] download(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            return response.body().bytes();
        } else {
            throw new IOException(String.format("Could not download: %s", response.message()));
        }
    }

    protected void decryptPersonalData(@NonNull BaercodeDocument document) throws IOException, DocumentParsingException, DocumentExpiredException, GeneralSecurityException {
        downloadRequiredFiles();
        baercodeCertificate.verifySignedByLetsEncrypt(context);
        baercodeBundle.verify(baercodeCertificate.getPublicKey());
        BaercodeKey baercodeKey = baercodeBundle.getKey(document.getBase64KeyId());
        if (baercodeKey == null) {
            throw new DocumentExpiredException("No valid key found in bundle");
        }
        document.verifyAndDecryptPersonalData(baercodeKey);
    }

    public synchronized void downloadRequiredFiles() throws IOException, CertificateException {
        if (baercodeBundle == null || (baercodeBundle.isExpired() && !BuildConfig.DEBUG)) {
            baercodeBundle = downloadBundle();
            baercodeCertificate = downloadCertificate();
        }
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
