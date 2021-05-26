package de.culture4life.luca.testing.provider.baercode;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import androidx.annotation.NonNull;

class BaercodeCertificate {

    private X509Certificate certificate;
    private final PublicKey publicKey;

    public BaercodeCertificate(@NonNull byte[] bytes) throws CertificateException {
        certificate = createCertificate(bytes);
        publicKey = certificate.getPublicKey();
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Verify that this certificate was issued by LetsEncrypt
     */
    public void verifySignedByLetsEncrypt(@NonNull Context context) throws IOException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        X509Certificate letsEncryptCertificate = getRootCertificate(context);
        checkServerTrusted(letsEncryptCertificate, certificate);
    }

    private X509Certificate getRootCertificate(@NonNull Context context) throws IOException, CertificateException {
        InputStream stream = context.getAssets().open("le_root.crt");
        CertificateFactory fac = CertificateFactory.getInstance("X509");
        return (X509Certificate) fac.generateCertificate(stream);
    }

    public static void checkServerTrusted(@NonNull X509Certificate caCertificate, @NonNull X509Certificate cert) throws CertificateException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        cert.verify(caCertificate.getPublicKey());
        cert.checkValidity();
    }

    public static X509Certificate createCertificate(@NonNull byte[] bytes) throws CertificateException {
        return createCertificate(new ByteArrayInputStream(bytes));
    }

    public static X509Certificate createCertificate(@NotNull InputStream inputStream) throws CertificateException {
        CertificateFactory fac = CertificateFactory.getInstance("X509");
        return (X509Certificate) fac.generateCertificate(inputStream);
    }

}
