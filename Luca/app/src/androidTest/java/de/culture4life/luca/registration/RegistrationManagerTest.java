package de.culture4life.luca.registration;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import de.culture4life.luca.BuildConfig;
import de.culture4life.luca.LucaInstrumentationTest;

public class RegistrationManagerTest extends LucaInstrumentationTest {

    private RegistrationManager registrationManager;

    @Before
    public void setup() {
        Assume.assumeTrue(BuildConfig.DEBUG);
        registrationManager = getInitializedManager(application.getRegistrationManager());
    }

    @Test
    public void hasCompletedRegistration_afterRegistration_emitsTrue() throws InterruptedException {
        registrationManager.registerUser()
                .andThen(registrationManager.hasCompletedRegistration())
                .test().await()
                .assertValue(true);
    }

    @Test
    public void hasCompletedRegistration_afterDeletion_emitsFalse() throws InterruptedException {
        registrationManager.registerUser()
                .andThen(registrationManager.deleteRegistrationData())
                .andThen(registrationManager.hasCompletedRegistration())
                .test().await()
                .assertValue(false);
    }

}