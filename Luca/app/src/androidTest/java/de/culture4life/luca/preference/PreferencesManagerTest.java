package de.culture4life.luca.preference;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import de.culture4life.luca.LucaApplication;
import de.culture4life.luca.crypto.CryptoManager;

public class PreferencesManagerTest {

    private static final String KEY = "testKey";
    private static final String testString = "testString";
    private LucaApplication application;
    private PreferencesManager preferencesManager;
    private CryptoManager cryptoManager;

    @Before
    public void setup() {
        application = (LucaApplication) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        application.getCryptoManager().initialize(application).blockingAwait();
        preferencesManager = application.getPreferencesManager();
        preferencesManager.doInitialize(application).blockingAwait();
        cryptoManager = application.getCryptoManager();
        cryptoManager.initialize(application).blockingAwait();
    }

    @Test
    public void restore_persistedValue_isSame() {
        preferencesManager.persist(KEY, testString)
                .andThen(preferencesManager.restore(KEY, String.class))
                .test().assertValue(testString);
    }

    @Test
    public void restore_afterDeletingAccountAndInitialize_succeeds() {
        cryptoManager.deleteAllKeyStoreEntries()
                .andThen(preferencesManager.doInitialize(application))
                .andThen(preferencesManager.persist(KEY, testString))
                .andThen(preferencesManager.restore(KEY, String.class))
                .test().assertValue(testString);
    }

}