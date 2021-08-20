package de.culture4life.luca.history;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.culture4life.luca.LucaUnitTest;
import de.culture4life.luca.checkin.CheckInData;
import de.culture4life.luca.children.ChildrenManager;
import de.culture4life.luca.crypto.CryptoManager;
import de.culture4life.luca.network.NetworkManager;
import de.culture4life.luca.preference.PreferencesManager;
import de.culture4life.luca.registration.RegistrationManager;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 28)
public class HistoryManagerTest extends LucaUnitTest {

    ChildrenManager childrenManager;
    HistoryManager historyManager;
    CheckInData checkInData = new CheckInData();

    @Before
    public void setUp() {
        PreferencesManager preferencesManager = new PreferencesManager();
        NetworkManager networkManager = new NetworkManager();
        CryptoManager cryptoManager = new CryptoManager(preferencesManager, networkManager);
        RegistrationManager registrationManager = new RegistrationManager(preferencesManager, networkManager, cryptoManager);
        childrenManager = new ChildrenManager(preferencesManager, registrationManager);
        historyManager = new HistoryManager(preferencesManager, childrenManager);
        historyManager.initialize(application).blockingAwait();

        checkInData.setTraceId("asdf");
        checkInData.setLocationId(UUID.randomUUID());
    }

    @Test
    public void getItems_initially_hasNoValues() {
        historyManager.getItems().test().assertNoValues();
    }

    @Test
    public void getItems_afterAddingCheckInItem_hasValue() {
        historyManager.addCheckInItem(checkInData)
                .andThen(historyManager.getItems())
                .test().assertValueCount(1);
    }

    @Test
    public void getItems_afterClearItems_hasNoValues() {
        historyManager.addCheckInItem(checkInData)
                .andThen(historyManager.clearItems())
                .andThen(historyManager.getItems())
                .map(historyItem -> historyItem.type)
                .test()
                .assertValue(HistoryItem.TYPE_DATA_DELETED)
                .assertValueCount(1);
    }

    @Test
    public void deleteOldItems_hasOldItems_removesOldItems() {
        checkInData.setTimestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(29));
        historyManager.addCheckInItem(checkInData)
                .andThen(historyManager.deleteOldItems())
                .andThen(historyManager.getItems())
                .test()
                .assertNoValues();
    }

    @Test
    public void deleteOldItems_noOldItems_keepsCurrentItems() {
        checkInData.setTimestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(23));
        historyManager.addCheckInItem(checkInData)
                .andThen(historyManager.deleteOldItems())
                .andThen(historyManager.getItems())
                .test().assertValueCount(1);
    }

}