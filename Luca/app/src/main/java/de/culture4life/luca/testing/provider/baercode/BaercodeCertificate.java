package de.culture4life.luca.testing.provider.baercode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import de.culture4life.luca.LucaApplication;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.TrustManagerFactory;

import androidx.annotation.NonNull;

class BaercodeCertificate {

    private static final String AUTHORITY_BAERCODE_DE = "authority.baercode.de";
    private final List<X509Certificate> certificateChain;
    private final PublicKey publicKey;

    public BaercodeCertificate(@NonNull byte[] bytes) throws CertificateException {
        certificateChain = createCertificateChain(bytes);
        publicKey = certificateChain.get(0).getPublicKey();
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Verify that this certificate was issued by LetsEncrypt
     */
    public void verifySignedByLetsEncrypt(@NonNull Context context) throws GeneralSecurityException, IOException {
        X509Certificate letsEncryptCertificate = getRootCertificate(context);
        checkServerTrusted(letsEncryptCertificate, certificateChain);
    }

    @SuppressLint("NewApi")
    private X509Certificate getRootCertificate(@NonNull Context context) throws IOException, CertificateException {
        InputStream stream;
        if (LucaApplication.isRunningUnitTests()) {
            stream = Files.newInputStream(Paths.get("src/main/assets/le_root.crt"));
        } else {
            stream = context.getAssets().open("le_root.crt");
        }
        return createCertificate(stream);
    }

    public static void checkServerTrusted(@NonNull X509Certificate root, @NonNull List<X509Certificate> certificatesCollection) throws GeneralSecurityException, IOException {
        root.checkValidity();
        validateCertPath(root, certificatesCollection);
    }

    private static void validateCertPath(@NonNull X509Certificate root, @NonNull List<X509Certificate> certificateChain) throws GeneralSecurityException, IOException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        CertPathValidator validator = CertPathValidator.getInstance("PKIX");
        CertPath path = certificateFactory.generateCertPath(certificateChain);

        // Create a KeyStore containing our trusted CAs
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("baercode_root_ca", root);

        // Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        // validate the cert path
        PKIXParameters parameters = new PKIXParameters(keyStore);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PKIXRevocationChecker checker = (PKIXRevocationChecker) validator.getRevocationChecker();
            Set<PKIXRevocationChecker.Option> options = new HashSet<>();
            options.add(PKIXRevocationChecker.Option.SOFT_FAIL);
            options.add(PKIXRevocationChecker.Option.ONLY_END_ENTITY);
            checker.setOptions(options);
            parameters.addCertPathChecker(checker);
            X509CertSelector selector = new X509CertSelector();
            selector.addSubjectAlternativeName(2 /* for DNS, see RFC 5280 Page 38 */, AUTHORITY_BAERCODE_DE);
            selector.setKeyUsage(new boolean[] {true, false, false, false, false, false, false, false, false});
            parameters.setTargetCertConstraints(selector);
        } else {
            parameters.setRevocationEnabled(false);
        }
        validator.validate(path, parameters);
    }

    public static X509Certificate createCertificate(@NonNull InputStream inputStream) throws CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
        return (X509Certificate) certificateFactory.generateCertificate(inputStream);
    }

    public static List<X509Certificate> createCertificateChain(@NonNull byte[] fileContent) throws CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<X509Certificate> chain = (Collection<X509Certificate>) certificateFactory.generateCertificates(new ByteArrayInputStream(fileContent));
        return new ArrayList<>(chain);
    }

}
