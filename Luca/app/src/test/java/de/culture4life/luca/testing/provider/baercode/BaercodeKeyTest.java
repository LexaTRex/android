package de.culture4life.luca.testing.provider.baercode;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import androidx.test.runner.AndroidJUnit4;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class BaercodeKeyTest {

    @Test
    public void getKeyId_fromExampleKey_isCorrect() {
        byte[] x = new byte[]{
                0, 1, 75, 92, -89, -2, 34, -102, 6, -90, 70, 1, 127, -59, 28, -31, 108, -14, 38, 106, 11, 34, -41, 45, -2, 4, -53, 67, -72, -106, -5, -62, -17, 117, -40, 103, -94, -108, 27, -35, 21, 112, 12, 6, -76, -13, 124, -74, 1, 102, 58, 30, 113, 127, -4, -73, 77, 40, 5, 80, 100, 82, 17, -117, -11, -97
        };
        BaercodeKey baercodeKey = new BaercodeKey(1, null, x, null);
        Assert.assertEquals("Oh5xf/y3TSgFUGRSEYv1nw==", baercodeKey.getKeyId());
    }

}