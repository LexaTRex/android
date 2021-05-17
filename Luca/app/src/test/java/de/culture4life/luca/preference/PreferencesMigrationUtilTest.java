package de.culture4life.luca.preference;

import com.nexenio.rxpreferences.provider.InMemoryPreferencesProvider;
import com.nexenio.rxpreferences.provider.PreferencesProvider;

import de.culture4life.luca.crypto.WrappedSecret;
import de.culture4life.luca.history.HistoryItemContainer;

import org.junit.Before;
import org.junit.Test;

import static de.culture4life.luca.crypto.CryptoManager.TRACING_SECRET_KEY_PREFIX;
import static de.culture4life.luca.history.HistoryManager.KEY_HISTORY_ITEMS;
import static de.culture4life.luca.ui.onboarding.OnboardingActivity.WELCOME_SCREEN_SEEN_KEY;

public class PreferencesMigrationUtilTest {

    private PreferencesProvider oldProvider;
    private PreferencesProvider newProvider;

    @Before
    public void setUp() {
        oldProvider = new InMemoryPreferencesProvider(PreferencesManager.SERIALIZER);
        newProvider = new InMemoryPreferencesProvider(PreferencesManager.SERIALIZER);
    }

    @Test
    public void migrate_knownKey_persistedInNewProvider() {
        oldProvider.persist(WELCOME_SCREEN_SEEN_KEY, true)
                .andThen(PreferencesMigrationUtil.migrate(oldProvider, newProvider))
                .andThen(newProvider.restore(WELCOME_SCREEN_SEEN_KEY, Boolean.class))
                .test()
                .assertValue(true);
    }

    @Test
    public void migrate_unknownKey_notPersistedInNewProvider() {
        oldProvider.persist("unknown key", true)
                .andThen(PreferencesMigrationUtil.migrate(oldProvider, newProvider))
                .andThen(newProvider.containsKey("unknown key"))
                .test()
                .assertValue(false);
    }

    @Test
    public void migrate_knownKey_deletedInOldProvider() {
        oldProvider.persist(WELCOME_SCREEN_SEEN_KEY, true)
                .andThen(PreferencesMigrationUtil.migrate(oldProvider, newProvider))
                .andThen(oldProvider.containsKey(WELCOME_SCREEN_SEEN_KEY))
                .test()
                .assertValue(false);
    }

    @Test
    public void getPreferenceType_unknownKey_completesEmpty() {
        PreferencesMigrationUtil.getPreferenceType("unknown key")
                .test()
                .assertNoValues();
    }

    @Test
    public void getPreferenceType_knownKey_emitsType() {
        PreferencesMigrationUtil.getPreferenceType(KEY_HISTORY_ITEMS)
                .test()
                .assertValue(HistoryItemContainer.class);
    }

    @Test
    public void getPreferenceType_tracingSecretKey_emitsType() {
        PreferencesMigrationUtil.getPreferenceType(TRACING_SECRET_KEY_PREFIX + "_something")
                .test()
                .assertValue(WrappedSecret.class);
    }

}