package de.culture4life.luca.util;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.culture4life.luca.LucaApplication;

public class AccessibilityServiceUtilTest {

    private LucaApplication application;

    @Before
    public void setup() {
        application = (LucaApplication) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
    }

    @Test
    @Ignore("Enable only temporarily when your device has talkBack enabled")
    public void testIsGoogleTalkbackActive() {
        Assert.assertTrue(AccessibilityServiceUtil.isGoogleTalkbackActive(application));
    }

}