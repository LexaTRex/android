package de.culture4life.luca.checkin;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class CheckInManagerTest {

    @Test
    public void isSelfCheckInUrl_validUrls_returnsTrue() {
        assertTrue(CheckInManager.isSelfCheckInUrl("https://app.luca-app.de/webapp/512875cb-17e6-4dad-ac62-3e792d94e03f"));
        assertTrue(CheckInManager.isSelfCheckInUrl("https://app.luca-app.de/webapp/512875cb-17e6-4dad-ac62-3e792d94e03f#e30"));
        assertTrue(CheckInManager.isSelfCheckInUrl("https://app.luca-app.de/webapp/512875cb-17e6-4dad-ac62-3e792d94e03f#e30/CWA1/CiRmY2E..."));
    }

    @Test
    public void isSelfCheckInUrl_invalidUrls_returnsFalse() {
        assertFalse(CheckInManager.isSelfCheckInUrl("https://app.luca-app.de/webapp/"));
        assertFalse(CheckInManager.isSelfCheckInUrl("https://app.luca-app.de/webapp/setup"));
        assertFalse(CheckInManager.isSelfCheckInUrl("https://app.luca-app.de/webapp/testresult/#eyJ0eXAi..."));
    }

}