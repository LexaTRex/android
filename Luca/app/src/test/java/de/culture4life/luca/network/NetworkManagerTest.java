package de.culture4life.luca.network;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import de.culture4life.luca.LucaUnitTest;
import okhttp3.Request;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class NetworkManagerTest extends LucaUnitTest {

    @Test
    public void useCdn_cacheableEndpoints_returnsTrue() {
        String[] urls = new String[]{
                "https://app.luca-app.de/api/v4/notifications/traces",
                "https://app.luca-app.de/api/v4/notifications/traces/9a5c8715-2810-4e17-a3c9-0c8190507dd5",
                "https://app.luca-app.de/api/v4/healthDepartments",
                "https://app.luca-app.de/api/v4/healthDepartments/9a5c8715-2810-4e17-a3c9-0c8190507dd5"

        };
        for (String url : urls) {
            Request request = new Request.Builder().url(url).build();
            assertTrue(NetworkManager.useCdn(request));
        }
    }

    @Test
    public void useCdn_nonCacheableEndpoints_returnsFalse() {
        String[] urls = new String[]{
                "https://app.luca-app.de/api/v3/traces/9a5c8715-2810-4e17-a3c9-0c8190507dd5",
                "https://app.luca-app.de/api/v4/locations/traces/9a5c8715-2810-4e17-a3c9-0c8190507dd5"
        };
        for (String url : urls) {
            Request request = new Request.Builder().url(url).build();
            assertFalse(NetworkManager.useCdn(request));
        }
    }

    @Test
    public void replaceHostWithCdn_validProductionRequest_replacesHost() {
        String expected = "https://data.luca-app.de/api/v4/notifications";
        Request request = new Request.Builder().url("https://app.luca-app.de/api/v4/notifications").build();
        assertEquals(expected, NetworkManager.replaceHostWithCdn(request).url().toString());
    }

    @Test
    public void replaceHostWithCdn_validStagingRequest_replacesHost() {
        String expected = "https://data-dev.luca-app.de/api/v4/notifications";
        Request request = new Request.Builder().url("https://app-dev.luca-app.de/api/v4/notifications").build();
        assertEquals(expected, NetworkManager.replaceHostWithCdn(request).url().toString());
    }

}