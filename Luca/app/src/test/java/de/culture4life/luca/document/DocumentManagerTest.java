package de.culture4life.luca.document;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.culture4life.luca.BuildConfig;
import de.culture4life.luca.LucaUnitTest;
import de.culture4life.luca.crypto.CryptoManager;
import de.culture4life.luca.document.provider.appointment.Appointment;
import de.culture4life.luca.document.provider.opentestcheck.OpenTestCheckDocument;
import de.culture4life.luca.history.HistoryManager;
import de.culture4life.luca.location.LocationManager;
import de.culture4life.luca.network.NetworkManager;
import de.culture4life.luca.network.pojo.DocumentProviderData;
import de.culture4life.luca.network.pojo.DocumentProviderDataList;
import de.culture4life.luca.preference.PreferencesManager;
import de.culture4life.luca.registration.RegistrationManager;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

import static de.culture4life.luca.document.provider.appointment.AppointmentProviderTest.VALID_APPOINTMENT;
import static de.culture4life.luca.document.provider.opentestcheck.OpenTestCheckDocumentProviderTest.EXPIRED_TEST_RESULT_TICKET_IO;
import static de.culture4life.luca.document.provider.opentestcheck.OpenTestCheckDocumentProviderTest.UNVERIFIED_TEST_RESULT;
import static de.culture4life.luca.document.provider.opentestcheck.OpenTestCheckDocumentProviderTest.VALID_TEST_RESULT_TICKET_IO;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class DocumentManagerTest extends LucaUnitTest {

    PreferencesManager preferencesManager;
    LocationManager locationManager;
    NetworkManager networkManager;
    HistoryManager historyManager;
    CryptoManager cryptoManager;
    RegistrationManager registrationManager;
    DocumentManager documentManager;

    private Document document;

    @Before
    public void setUp() {
        preferencesManager = spy(new PreferencesManager());
        locationManager = spy(new LocationManager());
        networkManager = spy(new NetworkManager());
        historyManager = spy(new HistoryManager(preferencesManager));
        cryptoManager = spy(new CryptoManager(preferencesManager, networkManager));
        registrationManager = spy(new RegistrationManager(preferencesManager, networkManager, cryptoManager));

        documentManager = spy(new DocumentManager(preferencesManager, networkManager, historyManager, cryptoManager, registrationManager));
        documentManager.initialize(application).blockingAwait();

        document = new Document();
        document.setId("12345");
        document.setType(Document.TYPE_FAST);
        document.setTestingTimestamp(System.currentTimeMillis());
        document.setOutcome(Document.OUTCOME_NEGATIVE);
    }

    @Test
    public void addDocument_validDocument_addsDocument() {
        documentManager.addDocument(document)
                .andThen(documentManager.getOrRestoreDocuments())
                .test()
                .assertValue(testResult -> testResult.getId().equals(this.document.getId()));
    }

    @Test
    public void addDocument_positiveTest_fails() {
        document.setOutcome(Document.OUTCOME_POSITIVE);
        documentManager.addDocument(document)
                .test()
                .assertError(TestResultPositiveException.class);
    }

    @Test
    public void generateEncodedDocumentHash_validDocument_expectedHash() {
        Document validDocument = new Document();
        validDocument.setHashableEncodedData(VALID_TEST_RESULT_TICKET_IO);

        documentManager.generateEncodedDocumentHash(validDocument)
                .test()
                .assertValue("Z/AAbdDXi/dZgJ27Wz6bAmLmbSOZ1MNHXEt34hrPQ4A=");
    }

    @Test
    public void reimportDocuments_noDocuments_completes() {
        documentManager.reImportDocuments()
                .test()
                .assertComplete();
    }

    @Test
    public void reimportDocuments_invalidDocuments_doesNotReImportInvalidDocuments() {
        Document invalidDocument = new OpenTestCheckDocument(UNVERIFIED_TEST_RESULT).getDocument();

        documentManager.addDocument(invalidDocument)
                .andThen(documentManager.reImportDocuments())
                .andThen(documentManager.getOrRestoreDocuments())
                .test()
                .assertNoValues();
    }

    @Test
    public void reimportDocuments_validDocuments_reImportsDocuments() {
        assumeTrue("Only run on debug where we ignore the expiry time", BuildConfig.DEBUG);
        doReturn(Completable.complete()).when(documentManager).unredeemDocument(any());

        Document validDocument = new Appointment(VALID_APPOINTMENT).getDocument();
        Document invalidDocument = new OpenTestCheckDocument(UNVERIFIED_TEST_RESULT).getDocument();

        documentManager.addDocument(validDocument)
                .andThen(documentManager.addDocument(invalidDocument))
                .andThen(documentManager.reImportDocuments())
                .andThen(documentManager.getOrRestoreDocuments())
                .map(Document::getId)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValue(validDocument.getId());
    }

    @Test
    public void addDocument_expiredDocument_throwsInRelease() {
        assumeTrue("Only run on release where we don't ignore the expiry time", !BuildConfig.DEBUG);

        Document expiredDocument = new OpenTestCheckDocument(EXPIRED_TEST_RESULT_TICKET_IO).getDocument();

        documentManager.addDocument(expiredDocument)
                .test()
                .assertError(DocumentExpiredException.class);
    }

    @Test
    public void clearDocuments_withExistingResult_hasNoValues() {
        documentManager.addDocument(document)
                .andThen(documentManager.clearDocuments())
                .andThen(documentManager.getOrRestoreDocuments())
                .test()
                .assertNoValues();
    }

    @Test
    public void deleteDocument_validId_removesFromDocuments() {
        documentManager.addDocument(document)
                .andThen(documentManager.deleteDocument(document.getId()))
                .andThen(documentManager.getOrRestoreDocuments())
                .test()
                .assertNoValues();
    }

    @Test
    public void deleteDocument_invalidId_doesNotChangeCount() {
        documentManager.addDocument(document)
                .andThen(documentManager.deleteDocument("does not exist"))
                .andThen(documentManager.getOrRestoreDocuments())
                .test()
                .assertValueCount(1);
    }

    @Test
    public void deleteTestedBefore_oldTest_remove() {
        document.setTestingTimestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3));
        documentManager.addDocument(document)
                .andThen(documentManager.deleteExpiredDocuments())
                .andThen(documentManager.getOrRestoreDocuments())
                .test()
                .assertNoValues();
    }

    @Test
    public void deleteTestedBefore_currentTest_keep() {
        document.setTestingTimestamp(System.currentTimeMillis());
        documentManager.addDocument(document)
                .andThen(documentManager.deleteExpiredDocuments())
                .andThen(documentManager.getOrRestoreDocuments())
                .test()
                .assertValueCount(1);
    }

    @Test
    public void getDocumentProviderData_cachedFingerprint_emitsMatchingCachedData() {
        String cachedFingerprint = "1";
        DocumentProviderDataList cachedDataList = new DocumentProviderDataList(Collections.singletonList(
                new DocumentProviderData("", "", cachedFingerprint)
        ));
        doReturn(Maybe.just(cachedDataList)).when(documentManager).restoreDocumentProviderDataListIfAvailable();
        doReturn(Single.error(new IllegalStateException("Should not be subscribed"))).when(documentManager).fetchDocumentProviderDataList();

        documentManager.getDocumentProviderData(cachedFingerprint)
                .test()
                .assertValue(documentProviderData -> documentProviderData.getFingerprint().equals(cachedFingerprint));
    }

    @Test
    public void getDocumentProviderData_newFingerprint_emitsMatchingFetchedDataAndCachesData() {
        String fetchedFingerprint = "2";
        DocumentProviderDataList fetchedDataList = new DocumentProviderDataList(Collections.singletonList(
                new DocumentProviderData("", "", fetchedFingerprint)
        ));
        doReturn(Single.just(fetchedDataList)).when(documentManager).fetchDocumentProviderDataList();

        documentManager.getDocumentProviderData(fetchedFingerprint)
                .test()
                .assertValue(documentProviderData -> documentProviderData.getFingerprint().equals(fetchedFingerprint));

        documentManager.restoreDocumentProviderDataListIfAvailable()
                .flatMapObservable(Observable::fromIterable)
                .test()
                .assertValue(documentProviderData -> fetchedFingerprint.equals(documentProviderData.getFingerprint()));
    }

    @Test
    public void getDocumentProviderData_unknownFingerprint_emitsAllFetchedData() {
        String fetchedFingerprint = "2";
        String unknownFingerprint = "";
        DocumentProviderDataList fetchedDataList = new DocumentProviderDataList(Collections.singletonList(
                new DocumentProviderData("", "", fetchedFingerprint)
        ));
        doReturn(Single.just(fetchedDataList)).when(documentManager).fetchDocumentProviderDataList();

        documentManager.getDocumentProviderData(unknownFingerprint)
                .toList()
                .map(List::size)
                .test()
                .assertValue(fetchedDataList.size());
    }

    @Test
    public void isTestResult_validUrls_returnsTrue() {
        assertTrue(DocumentManager.isTestResult("https://app.luca-app.de/webapp/testresult/#eyJ0eXAi..."));
    }

    @Test
    public void isTestResult_invalidUrls_returnsFalse() {
        assertFalse(DocumentManager.isTestResult("https://app.luca-app.de/webapp/meeting/e4e3c...#e30"));
        assertFalse(DocumentManager.isTestResult("https://app.luca-app.de/webapp/"));
        assertFalse(DocumentManager.isTestResult("https://www.google.com"));
        assertFalse(DocumentManager.isTestResult("https://www.google.com/webapp/testresult/#eyJ0eXAi..."));
        assertFalse(DocumentManager.isTestResult(""));
    }

}