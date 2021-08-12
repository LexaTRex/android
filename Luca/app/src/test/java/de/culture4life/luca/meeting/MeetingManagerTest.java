package de.culture4life.luca.meeting;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import de.culture4life.luca.LucaUnitTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class MeetingManagerTest extends LucaUnitTest {

    @Test
    public void isMeeting_validUrls_returnsTrue() {
        assertTrue(MeetingManager.isPrivateMeeting("https://app.luca-app.de/webapp/meeting/e4e3c...#e30"));
    }

    @Test
    public void isMeeting_invalidUrls_returnsFalse() {
        assertFalse(MeetingManager.isPrivateMeeting("https://app.luca-app.de/webapp"));
        assertFalse(MeetingManager.isPrivateMeeting("https://www.google.de"));
    }

}