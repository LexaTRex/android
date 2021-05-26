package de.culture4life.luca.testing.provider.baercode;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import androidx.test.runner.AndroidJUnit4;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class BaercodeCertificateTest {

    private X509Certificate baercodeCert;
    private X509Certificate letsEncryptCert;

    public static byte[] getFileContent(String fileName) throws IOException {
        return Files.readAllBytes(Paths.get("src/main/assets/" + fileName));
    }

    @Before
    public void setUp() throws CertificateException, IOException {
        baercodeCert = BaercodeCertificate.createCertificate(getFileContent("ba.crt"));
        letsEncryptCert = BaercodeCertificate.createCertificate(getFileContent("le_root.crt"));
    }

    @Test
    @Ignore
    public void verify_certificateWithLetsEncrypt_succeeds() throws GeneralSecurityException {
        BaercodeCertificate.checkServerTrusted(letsEncryptCert, baercodeCert);
    }

}