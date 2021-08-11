package de.culture4life.luca.network;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.culture4life.luca.BuildConfig;
import de.culture4life.luca.LucaApplication;
import de.culture4life.luca.network.endpoints.LucaEndpointsV3;
import de.culture4life.luca.network.pojo.DailyKeyPair;
import de.culture4life.luca.network.pojo.UserDeletionRequestData;
import retrofit2.HttpException;

public class NetworkManagerTest {

    private LucaApplication application;
    private NetworkManager networkManager;
    private LucaEndpointsV3 lucaEndpoints;

    @Before
    public void setup() {
        application = (LucaApplication) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        application.getCryptoManager().initialize(application).blockingAwait();
        networkManager = application.getNetworkManager();
        networkManager.doInitialize(application).blockingAwait();
        lucaEndpoints = networkManager.getLucaEndpointsV3().blockingGet();
    }

    @Test
    public void getDailyKeyPairPublicKey_call_isSuccessful() {
        DailyKeyPair response = lucaEndpoints.getDailyKeyPair().blockingGet();
        Assert.assertFalse(response.getPublicKey().isEmpty());
    }

    @Test(expected = HttpException.class)
    public void deleteUser_invalidRequestBody_fails() {
        if (BuildConfig.DEBUG) {
            UserDeletionRequestData data = new UserDeletionRequestData("...");
            lucaEndpoints.deleteUser("9a5c8715-2810-4e17-a3c9-0c8190507dd5", data)
                    .blockingAwait();
        }
    }

}