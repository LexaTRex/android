package de.culture4life.luca.preference;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.nexenio.rxpreferences.provider.EncryptedSharedPreferencesProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

import de.culture4life.luca.LucaInstrumentationTest;

@RunWith(AndroidJUnit4.class)
public class EncryptedSharedPreferencesProviderTest extends LucaInstrumentationTest {

    private static final String TEST_KEY = "test key";
    private static final HashMap<String, String> TEST_DATA = new HashMap<>();

    private EncryptedSharedPreferencesProvider provider;

    @Before
    public void setUp() {
        provider = new EncryptedSharedPreferencesProvider(application);
        TEST_DATA.put("a", "b");
    }

    @Test
    public void containsKey_notExisting_returnsFalse() {
        provider.containsKey("not existing")
                .test()
                .assertValue(false);
    }

    @Test
    public void containsKey_existingKey_returnsTrue() {
        provider.persist(TEST_KEY, TEST_DATA)
                .andThen(provider.containsKey(TEST_KEY))
                .test()
                .assertValue(true);
    }

    @Test
    public void restore_afterPersist_restoresSameData() {
        provider.persist(TEST_KEY, TEST_DATA)
                .andThen(provider.restore(TEST_KEY, TEST_DATA.getClass()))
                .map(testData -> testData.get("a"))
                .test()
                .assertValue(TEST_DATA.get("a"));
    }

    @Test
    public void delete_existingKey_isGone() {
        provider.persist(TEST_KEY, TEST_DATA)
                .andThen(provider.delete(TEST_KEY))
                .andThen(provider.containsKey(TEST_KEY))
                .test()
                .assertValue(false);
    }

    @Test
    public void deleteAll_withExistingKeys_hasNoKeys() {
        provider.persist(TEST_KEY, TEST_DATA)
                .andThen(provider.persist("another key", "whatever"))
                .andThen(provider.deleteAll())
                .andThen(provider.getKeys())
                .isEmpty()
                .test()
                .assertValue(true);
    }

}