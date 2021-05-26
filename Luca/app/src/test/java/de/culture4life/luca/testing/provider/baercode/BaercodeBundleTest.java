package de.culture4life.luca.testing.provider.baercode;

import de.culture4life.luca.testing.TestResultParsingException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;

import androidx.test.runner.AndroidJUnit4;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class BaercodeBundleTest {

    @Test
    public void createPublicKey_fromBundleFile_isNotNull() throws Exception {
        BaercodeBundle baercodeBundle = BaercodeBundle.getTestBundle();
        BaercodeKey baercodeKey = baercodeBundle.getKey("Oh5xf/y3TSgFUGRSEYv1nw==");
        Assert.assertEquals(2, baercodeKey.getCredType());
        Assert.assertTrue(baercodeBundle.isExpired());

        ECPublicKey publicKey = BaercodeTestResultProvider.createPublicKey(baercodeKey);
        Assert.assertNotNull(publicKey);
    }

    @Test
    public void validateBundle_withBaCert_succeeds() throws Exception {
        BaercodeBundle baercodeBundle = BaercodeBundle.getTestBundle();
        PublicKey publicKey = new BaercodeCertificate(BaercodeCertificateTest.getFileContent("ba.crt")).getPublicKey();
        baercodeBundle.verify(publicKey);
    }

    @Test(expected = TestResultParsingException.class)
    public void validateBundle_withLetsEncryptCert_fails() throws Exception {
        BaercodeBundle baercodeBundle = BaercodeBundle.getTestBundle();
        PublicKey publicKey = new BaercodeCertificate(BaercodeCertificateTest.getFileContent("le_root.crt")).getPublicKey();
        baercodeBundle.verify(publicKey);
    }

}