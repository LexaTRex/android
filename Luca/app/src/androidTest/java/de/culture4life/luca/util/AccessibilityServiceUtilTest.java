package de.culture4life.luca.util;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import de.culture4life.luca.LucaInstrumentationTest;

public class AccessibilityServiceUtilTest extends LucaInstrumentationTest {

    @Test
    @Ignore("Enable only temporarily when your device has talkBack enabled")
    public void testIsGoogleTalkbackActive() {
        Assert.assertTrue(AccessibilityServiceUtil.isGoogleTalkbackActive(application));
    }

}