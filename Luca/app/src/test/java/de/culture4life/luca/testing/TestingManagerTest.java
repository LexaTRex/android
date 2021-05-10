package de.culture4life.luca.testing;

import de.culture4life.luca.LucaUnitTest;
import de.culture4life.luca.crypto.CryptoManager;
import de.culture4life.luca.history.HistoryManager;
import de.culture4life.luca.location.LocationManager;
import de.culture4life.luca.network.NetworkManager;
import de.culture4life.luca.preference.PreferencesManager;
import de.culture4life.luca.registration.RegistrationData;
import de.culture4life.luca.registration.RegistrationManager;
import de.culture4life.luca.testing.provider.opentestcheck.OpenTestCheckTestResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.concurrent.TimeUnit;

import androidx.test.runner.AndroidJUnit4;
import io.reactivex.rxjava3.core.Single;

import static de.culture4life.luca.testing.provider.opentestcheck.OpenTestCheckTestResultProviderTest.UNVERIFIED_TEST_RESULT;
import static de.culture4life.luca.testing.provider.opentestcheck.OpenTestCheckTestResultProviderTest.VALID_TEST_RESULT;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class TestingManagerTest extends LucaUnitTest {

    PreferencesManager preferencesManager;
    LocationManager locationManager;
    NetworkManager networkManager;
    HistoryManager historyManager;
    CryptoManager cryptoManager;
    RegistrationManager registrationManager;
    TestingManager testingManager;

    private TestResult testResult;

    @Before
    public void setUp() {
        preferencesManager = spy(new PreferencesManager());
        locationManager = spy(new LocationManager());
        networkManager = spy(new NetworkManager());
        historyManager = spy(new HistoryManager(preferencesManager));
        cryptoManager = spy(new CryptoManager(preferencesManager, networkManager));
        registrationManager = spy(new RegistrationManager(preferencesManager, networkManager, cryptoManager));

        testingManager = spy(new TestingManager(preferencesManager, historyManager, registrationManager));
        testingManager.initialize(application).blockingAwait();

        testResult = new TestResult();
        testResult.setId("12345");
        testResult.setType(TestResult.TYPE_FAST);
        testResult.setTestingTimestamp(System.currentTimeMillis());
    }

    @Test
    public void add_test_result() {
        testingManager.addTestResult(testResult)
                .andThen(testingManager.getOrRestoreTestResults())
                .test()
                .assertValue(testResult -> testResult.getId().equals(this.testResult.getId()));
    }

    @Test
    public void reimportTestResults_noTestResults_completes() {
        testingManager.reImportTestResults()
                .test()
                .assertComplete();
    }

    @Test
    public void reimportTestResults_invalidTestResults_doesNotReImportInvalidTestResults() {
        TestResult invalidTestResult = new OpenTestCheckTestResult(UNVERIFIED_TEST_RESULT).getLucaTestResult();

        testingManager.addTestResult(invalidTestResult)
                .andThen(testingManager.reImportTestResults())
                .andThen(testingManager.getOrRestoreTestResults())
                .test()
                .assertNoValues();
    }

    @Test
    public void reimportTestResults_validTestResults_reImportsTestResults() {
        TestResult validTestResult = new OpenTestCheckTestResult(VALID_TEST_RESULT).getLucaTestResult();
        TestResult invalidTestResult = new OpenTestCheckTestResult(UNVERIFIED_TEST_RESULT).getLucaTestResult();

        RegistrationData registrationData = new RegistrationData();
        registrationData.setFirstName("Jannusch");
        registrationData.setLastName("Barnech");
        when(registrationManager.getOrCreateRegistrationData()).thenReturn(Single.just(registrationData));

        testingManager.addTestResult(validTestResult)
                .andThen(testingManager.addTestResult(invalidTestResult))
                .andThen(testingManager.reImportTestResults())
                .andThen(testingManager.getOrRestoreTestResults())
                .map(TestResult::getId)
                .test()
                .assertValue(validTestResult.getId());
    }

    @Test
    public void clearTestResults_withExistingResult_hasNoValues() {
        testingManager.addTestResult(testResult)
                .andThen(testingManager.clearTestResults())
                .andThen(testingManager.getOrRestoreTestResults())
                .test()
                .assertNoValues();
    }

    @Test
    public void deleteTestResult_validId_removesFromTestResults() {
        testingManager.addTestResult(testResult)
                .andThen(testingManager.deleteTestResult(testResult.getId()))
                .andThen(testingManager.getOrRestoreTestResults())
                .test()
                .assertNoValues();
    }

    @Test
    public void deleteTestResult_invalidId_doesNotChangeCount() {
        testingManager.addTestResult(testResult)
                .andThen(testingManager.deleteTestResult("does not exist"))
                .andThen(testingManager.getOrRestoreTestResults())
                .test()
                .assertValueCount(1);
    }

    @Test
    public void deleteTestedBefore_oldTest_remove() {
        testResult.setTestingTimestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3));
        testingManager.addTestResult(testResult)
                .andThen(testingManager.deleteOldTests())
                .andThen(testingManager.getOrRestoreTestResults())
                .test()
                .assertNoValues();
    }

    @Test
    public void deleteTestedBefore_currentTest_keep() {
        testResult.setTestingTimestamp(System.currentTimeMillis());
        testingManager.addTestResult(testResult)
                .andThen(testingManager.deleteOldTests())
                .andThen(testingManager.getOrRestoreTestResults())
                .test()
                .assertValueCount(1);
    }

}