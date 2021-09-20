package de.culture4life.luca.network;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import de.culture4life.luca.BuildConfig;
import de.culture4life.luca.LucaInstrumentationTest;
import de.culture4life.luca.network.endpoints.LucaEndpointsV3;
import de.culture4life.luca.network.pojo.DailyKeyPair;
import de.culture4life.luca.network.pojo.UserDeletionRequestData;
import retrofit2.HttpException;

public class NetworkManagerTest extends LucaInstrumentationTest {

    private NetworkManager networkManager;
    private LucaEndpointsV3 lucaEndpoints;

    @Before
    public void setup() {
        networkManager = getInitializedManager(application.getNetworkManager());
        lucaEndpoints = networkManager.getLucaEndpointsV3().blockingGet();
    }

    @Test
    public void getDailyKeyPairPublicKey_call_isSuccessful() throws InterruptedException {
        lucaEndpoints.getDailyKeyPair()
                .map(DailyKeyPair::getPublicKey)
                .map(String::isEmpty)
                .test().await()
                .assertValue(false);
    }

    @Test
    public void deleteUser_invalidRequestBody_fails() throws InterruptedException {
        Assume.assumeTrue(BuildConfig.DEBUG);
        initializeManager(application.getCryptoManager());
        UserDeletionRequestData data = new UserDeletionRequestData("...");
        lucaEndpoints.deleteUser("9a5c8715-2810-4e17-a3c9-0c8190507dd5", data)
                .test().await()
                .assertError(HttpException.class);
    }

}