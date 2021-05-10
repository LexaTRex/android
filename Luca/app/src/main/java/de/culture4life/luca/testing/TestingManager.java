package de.culture4life.luca.testing;

import android.content.Context;

import de.culture4life.luca.BuildConfig;
import de.culture4life.luca.Manager;
import de.culture4life.luca.history.HistoryManager;
import de.culture4life.luca.preference.PreferencesManager;
import de.culture4life.luca.registration.RegistrationManager;
import de.culture4life.luca.testing.provider.ProvidedTestResult;
import de.culture4life.luca.testing.provider.TestResultProvider;
import de.culture4life.luca.testing.provider.opentestcheck.OpenTestCheckTestResultProvider;
import de.culture4life.luca.testing.provider.ubirch.UbirchTestResultProvider;

import java.util.NoSuchElementException;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Predicate;
import timber.log.Timber;

public class TestingManager extends Manager {

    private static final String KEY_TEST_RESULTS = "test_results";

    private final PreferencesManager preferencesManager;
    private final HistoryManager historyManager;
    private final RegistrationManager registrationManager;

    private final UbirchTestResultProvider ubirchTestResultProvider;
    private final OpenTestCheckTestResultProvider openTestCheckTestResultProvider;

    private TestResults testResults;

    public TestingManager(@NonNull PreferencesManager preferencesManager, @NonNull HistoryManager historyManager, @NonNull RegistrationManager registrationManager) {
        this.preferencesManager = preferencesManager;
        this.historyManager = historyManager;
        this.registrationManager = registrationManager;
        this.ubirchTestResultProvider = new UbirchTestResultProvider();
        this.openTestCheckTestResultProvider = new OpenTestCheckTestResultProvider();
    }

    @Override
    protected Completable doInitialize(@NonNull Context context) {
        return Completable.mergeArray(
                preferencesManager.initialize(context),
                historyManager.initialize(context)
        ).andThen(deleteOldTests());
    }

    public Completable addTestResult(@NonNull TestResult testResult) {
        return getTestResultIfAvailable(testResult.getId())
                .isEmpty()
                .flatMapCompletable(isNewTest -> {
                    if (!isNewTest) {
                        return Completable.error(new TestResultAlreadyImportedException());
                    }
                    if (testResult.getExpirationTimestamp() < System.currentTimeMillis() && !BuildConfig.DEBUG) {
                        return Completable.error(new TestResultExpiredException());
                    }
                    return getOrRestoreTestResults()
                            .mergeWith(Observable.just(testResult))
                            .toList()
                            .map(TestResults::new)
                            .flatMapCompletable(this::persistTestResults)
                            .andThen(addToHistory(testResult))
                            .doOnSubscribe(disposable -> Timber.d("Persisting test result: %s", testResult));
                });
    }

    public Single<TestResult> parseEncodedTestResult(@NonNull String encodedTestResult) {
        return Single.mergeDelayError(registrationManager.getOrCreateRegistrationData()
                .flatMapPublisher(registrationData -> getTestResultProvidersFor(encodedTestResult)
                        .doOnNext(testResultProvider -> Timber.v("Attempting to parse using %s", testResultProvider.getClass().getSimpleName()))
                        .map(testResultProvider -> testResultProvider.parseAndValidate(encodedTestResult, registrationData)
                                .doOnError(throwable -> Timber.w("Parsing failed: %s", throwable.toString()))
                                .map(ProvidedTestResult::getLucaTestResult))
                        .toFlowable(BackpressureStrategy.BUFFER)))
                .firstOrError()
                .onErrorResumeNext(throwable -> {
                    if (throwable instanceof NoSuchElementException) {
                        return Single.error(new TestResultParsingException("No parser available for encoded data"));
                    } else {
                        return Single.error(throwable);
                    }
                });
    }

    private Observable<? extends TestResultProvider<? extends ProvidedTestResult>> getTestResultProvidersFor(@NonNull String encodedTestResult) {
        return getTestResultProviders()
                .filter(testResultProvider -> testResultProvider.canParse(encodedTestResult).blockingGet());
    }

    private Observable<? extends TestResultProvider<? extends ProvidedTestResult>> getTestResultProviders() {
        return Observable.just(openTestCheckTestResultProvider); // TODO: 07.05.21 add ubirchTestResultProvider
    }

    private Completable addToHistory(@NonNull TestResult testResult) {
        return historyManager.addTestResultImportedItem(testResult);
    }

    public Maybe<TestResult> getTestResultIfAvailable(@NonNull String id) {
        return getOrRestoreTestResults()
                .filter(testResult -> id.equals(testResult.getId()))
                .firstElement();
    }

    public Observable<TestResult> getOrRestoreTestResults() {
        return Maybe.fromCallable(() -> testResults)
                .flatMapObservable(Observable::fromIterable)
                .switchIfEmpty(restoreTestResults());
    }

    private Observable<TestResult> restoreTestResults() {
        return preferencesManager.restoreOrDefault(KEY_TEST_RESULTS, new TestResults())
                .doOnSubscribe(disposable -> Timber.d("Restoring test results"))
                .doOnSuccess(restoredData -> this.testResults = restoredData)
                .flatMapObservable(Observable::fromIterable);
    }

    private Completable persistTestResults(@NonNull TestResults testResults) {
        return preferencesManager.persist(KEY_TEST_RESULTS, testResults)
                .doOnSubscribe(disposable -> {
                    Timber.d("Persisting " + testResults.size() + " test results");
                    this.testResults = testResults;
                });
    }

    public Completable reImportTestResults() {
        return getOrRestoreTestResults()
                .map(TestResult::getEncodedData).toList()
                .doOnSuccess(encodedTestResults -> Timber.i("Re-importing %d test results", encodedTestResults.size()))
                .flatMapCompletable(encodedTestResults -> clearTestResults()
                        .andThen(Observable.fromIterable(encodedTestResults)
                                .flatMapMaybe(encodedTestResult -> parseEncodedTestResult(encodedTestResult)
                                        .doOnError(throwable -> Timber.w("Unable to re-import test result: %s", throwable.toString()))
                                        .onErrorComplete())
                                .flatMapCompletable(this::addTestResult)));
    }

    public Completable clearTestResults() {
        return preferencesManager.delete(KEY_TEST_RESULTS)
                .doOnComplete(() -> testResults = null);
    }

    protected Completable deleteOldTests() {
        return Single.fromCallable(System::currentTimeMillis)
                .flatMapCompletable(this::deleteTestsExpiredBefore);
    }

    private Completable deleteTestsExpiredBefore(long timestamp) {
        return deleteTestResults(testResult -> timestamp < testResult.getExpirationTimestamp())
                .doOnSubscribe(disposable -> Timber.d("Deleting tests expired before %d", timestamp));
    }

    public Completable deleteTestResult(@NonNull String id) {
        return deleteTestResults(testResult -> !id.equals(testResult.getId()))
                .doOnSubscribe(disposable -> Timber.d("Deleting test result: %s", id));
    }

    private Completable deleteTestResults(@NonNull Predicate<TestResult> filterFunction) {
        return getOrRestoreTestResults()
                .filter(filterFunction)
                .toList()
                .map(TestResults::new)
                .flatMapCompletable(this::persistTestResults);
    }

}
