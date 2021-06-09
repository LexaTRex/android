package de.culture4life.luca.testing.provider.baercode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import androidx.test.runner.AndroidJUnit4;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class BaercodeCertificateTest {

    private X509Certificate letsEncryptCert;

    public static byte[] getFileContent(String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(fileName));
    }

    @Before
    public void setUp() throws CertificateException, IOException {
        letsEncryptCert = BaercodeCertificate.createCertificate(Files.newInputStream(Paths.get("src/main/assets/le_root.crt")));
    }

    @Test
    public void verify_baercodeCertificateWithLetsEncrypt_succeeds() throws GeneralSecurityException, IOException {
        byte[] fileContent = getFileContent("src/test/assets/baercode.crt");
        List<X509Certificate> baercodeChain = BaercodeCertificate.createCertificateChain(fileContent);
        BaercodeCertificate.checkServerTrusted(letsEncryptCert, baercodeChain);
    }

    @Test(expected = CertPathValidatorException.class)
    public void verify_lucaCertificateWithLetsEncrypt_fails() throws GeneralSecurityException, IOException {
        byte[] fileContent = getFileContent("src/test/assets/luca.cer");
        List<X509Certificate> lucaChain = BaercodeCertificate.createCertificateChain(fileContent);
        BaercodeCertificate.checkServerTrusted(letsEncryptCert, lucaChain);
    }

    @Test(expected = CertPathValidatorException.class)
    public void verify_revokedLetsEncryptCertificate_fails() throws GeneralSecurityException, IOException {
        byte[] fileContent = getFileContent("src/test/assets/revoked-letsencrypt.cer");
        List<X509Certificate> revokedLetsEncryptChain = BaercodeCertificate.createCertificateChain(fileContent);
        BaercodeCertificate.checkServerTrusted(letsEncryptCert, revokedLetsEncryptChain);
    }

}