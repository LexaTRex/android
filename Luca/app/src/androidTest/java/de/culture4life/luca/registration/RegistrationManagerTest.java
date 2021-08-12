package de.culture4life.luca.registration;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import de.culture4life.luca.BuildConfig;
import de.culture4life.luca.LucaApplication;

import static org.junit.Assert.assertFalse;

public class RegistrationManagerTest {

    private LucaApplication application;
    private RegistrationManager registrationManager;

    @Before
    public void setup() {
        Assume.assumeTrue(BuildConfig.DEBUG);
        application = (LucaApplication) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        registrationManager = application.getRegistrationManager();
        registrationManager.doInitialize(application).blockingAwait();
    }

    @Test
    public void registerUser_hasCompletedRegistration_isTrue() {
        registrationManager.registerUser().blockingAwait();
        Boolean isRegistered = registrationManager.hasCompletedRegistration().blockingGet();
        Assert.assertTrue(isRegistered);
    }

    @Test
    public void hasCompletedRegistration_afterDeleteAll_isFalse() {
        registrationManager.registerUser().blockingAwait();
        registrationManager.deleteRegistrationData().blockingAwait();
        Boolean isRegistered = registrationManager.hasCompletedRegistration().blockingGet();
        assertFalse(isRegistered);
    }

}