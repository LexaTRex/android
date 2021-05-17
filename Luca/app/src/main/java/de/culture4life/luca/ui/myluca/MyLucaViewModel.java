package de.culture4life.luca.ui.myluca;

import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import android.annotation.SuppressLint;
import android.app.Application;

import de.culture4life.luca.R;
import de.culture4life.luca.notification.LucaNotificationManager;
import de.culture4life.luca.registration.RegistrationManager;
import de.culture4life.luca.testing.TestResult;
import de.culture4life.luca.testing.TestResultAlreadyImportedException;
import de.culture4life.luca.testing.TestResultExpiredException;
import de.culture4life.luca.testing.TestResultParsingException;
import de.culture4life.luca.testing.TestResultPositiveException;
import de.culture4life.luca.testing.TestResultVerificationException;
import de.culture4life.luca.testing.TestingManager;
import de.culture4life.luca.ui.BaseViewModel;
import de.culture4life.luca.ui.ViewError;
import de.culture4life.luca.ui.ViewEvent;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class MyLucaViewModel extends BaseViewModel implements ImageAnalysis.Analyzer {

    public static final String TEST_VERIFY_URL_PREFIX = "https://testverify.io/v1#";

    private final TestingManager testingManager;
    private final LucaNotificationManager notificationManager;
    private final RegistrationManager registrationManager;

    private final MutableLiveData<String> userName = new MutableLiveData<>();
    private final MutableLiveData<List<MyLucaListItem>> myLucaItems = new MutableLiveData<>();
    private final MutableLiveData<ViewEvent<TestResult>> importedTestResult = new MutableLiveData<>();

    private final BarcodeScanner scanner;

    private Disposable imageProcessingDisposable;
    private ViewError importError;

    public MyLucaViewModel(@NonNull Application application) {
        super(application);
        this.testingManager = this.application.getTestingManager();
        this.notificationManager = this.application.getNotificationManager();
        this.registrationManager = this.application.getRegistrationManager();
        this.scanner = BarcodeScanning.getClient();
        this.isLoading.setValue(false);
    }

    @Override
    public Completable initialize() {
        return super.initialize()
                .andThen(Completable.mergeArray(
                        testingManager.initialize(application),
                        notificationManager.initialize(application),
                        registrationManager.initialize(application)
                ))
                .andThen(invokeUpdate())
                .andThen(handleApplicationDeepLinkIfAvailable());
    }

    public Completable invokeUpdate() {
        return Completable.fromAction(() -> modelDisposable.add(updateList()
                .andThen(registrationManager.getOrCreateRegistrationData())
                .flatMapCompletable(registrationData -> update(userName, registrationData.getFullName()))
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Timber.i("Updated my luca list"),
                        throwable -> Timber.w("Unable to update my luca list: %s", throwable.toString())
                )));
    }

    private Completable updateList() {
        return loadListItems()
                .toList()
                .flatMapCompletable(items -> update(myLucaItems, items));
    }

    private Observable<MyLucaListItem> loadListItems() {
        return testingManager.getOrRestoreTestResults()
                .flatMapMaybe(this::createListItem)
                .sorted((first, second) -> Long.compare(second.getTimestamp(), first.getTimestamp()));
    }

    private Maybe<MyLucaListItem> createListItem(@NonNull TestResult testResult) {
        return Maybe.fromCallable(() -> {
            MyLucaListItem item = MyLucaListItem.from(application, testResult);
            return item;
        });
    }

    /*
        QR code scanning
     */

    @SuppressLint("UnsafeExperimentalUsageError")
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (imageProcessingDisposable != null && !imageProcessingDisposable.isDisposed()) {
            Timber.v("Not processing new camera image, still processing previous one");
            imageProxy.close();
            return;
        }

        imageProcessingDisposable = processCameraImage(imageProxy)
                .subscribeOn(Schedulers.computation())
                .doOnError(throwable -> Timber.w("Unable to process camera image: %s", throwable.toString()))
                .onErrorComplete()
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(imageProxy::close)
                .subscribe();

        modelDisposable.add(imageProcessingDisposable);
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private Completable processCameraImage(@NonNull ImageProxy imageProxy) {
        return Maybe.fromCallable(imageProxy::getImage)
                .filter(image -> {
                    if (importError != null && errors.getValue().contains(importError)) {
                        // currently showing an import related error
                        return false;
                    } else {
                        return true;
                    }
                })
                .map(image -> InputImage.fromMediaImage(image, imageProxy.getImageInfo().getRotationDegrees()))
                .flatMapObservable(this::detectBarcodes)
                .flatMapCompletable(this::processBarcode);
    }

    private Observable<Barcode> detectBarcodes(@NonNull InputImage image) {
        return Observable.create(emitter -> scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        emitter.onNext(barcode);
                    }
                    emitter.onComplete();
                })
                .addOnFailureListener(emitter::tryOnError));
    }

    private Completable processBarcode(@NonNull Barcode barcode) {
        return Maybe.fromCallable(barcode::getRawValue)
                .doOnSuccess(value -> {
                    Timber.d("Processing barcode: %s", value);
                    notificationManager.vibrate().subscribe();
                })
                .map(barcodeValue -> barcodeValue.replace(TEST_VERIFY_URL_PREFIX, ""))
                .flatMapCompletable(this::importTestResult);
    }

    private Completable importTestResult(@NonNull String encodedTestResult) {
        return testingManager.parseEncodedTestResult(encodedTestResult)
                .doOnSuccess(testResult -> Timber.d("Parsed test result: %s", testResult))
                .flatMapCompletable(testResult -> testingManager.redeemTestResult(testResult)
                        .andThen(testingManager.addTestResult(testResult))
                        .andThen(update(importedTestResult, new ViewEvent<>(testResult)))
                        .andThen(invokeUpdate()))
                .doOnSubscribe(disposable -> {
                    removeError(importError);
                    updateAsSideEffect(showCameraPreview, false);
                    updateAsSideEffect(isLoading, true);
                })
                .doOnError(throwable -> {
                    Timber.w("Unable to import test result: %s", throwable.toString());
                    ViewError.Builder errorBuilder = createErrorBuilder(throwable)
                            .withTitle(R.string.test_import_error_title);

                    if (throwable instanceof TestResultVerificationException) {
                        switch (((TestResultVerificationException) throwable).getReason()) {
                            case TEST_EXPIRED:
                                errorBuilder.withDescription(R.string.test_import_error_expired_description);
                                break;
                            case NAME_MISMATCH: // intended fall-through
                            case INVALID_SIGNATURE:
                                errorBuilder.withDescription(R.string.test_import_error_verification_description);
                                break;
                        }
                    } else if (throwable instanceof TestResultPositiveException) {
                        errorBuilder.withDescription(R.string.test_import_error_positive_description);
                    } else if (throwable instanceof TestResultParsingException) {
                        errorBuilder.withDescription(R.string.test_import_error_unsupported_description);
                    } else if (throwable instanceof TestResultAlreadyImportedException) {
                        errorBuilder.withDescription(R.string.test_import_error_already_imported_description);
                    } else if (throwable instanceof TestResultExpiredException) {
                        errorBuilder.withDescription(R.string.test_import_error_expired_description);
                    }

                    importError = errorBuilder.build();
                    addError(importError);
                })
                .onErrorComplete()
                .doFinally(() -> updateAsSideEffect(isLoading, false))
                .subscribeOn(Schedulers.io());
    }

    /**
     * Delete the test result
     *
     * @param position position in the list that will be deleted
     */
    public Completable deleteTestResult(int position) {
        String testResultId = myLucaItems.getValue().get(position).getTestResultId();
        return testingManager.deleteTestResult(testResultId).andThen(updateList());
    }

    public LiveData<List<MyLucaListItem>> getMyLucaItems() {
        return myLucaItems;
    }

    public LiveData<ViewEvent<TestResult>> getImportedTestResult() {
        return importedTestResult;
    }

    public MutableLiveData<String> getUserName() {
        return userName;
    }

    /*
        Deep link handling
     */

    private Completable handleApplicationDeepLinkIfAvailable() {
        return application.getDeepLink()
                .flatMapCompletable(this::handleDeepLink)
                .doOnComplete(() -> Timber.d("Handled application deep link"));
    }

    private Completable handleDeepLink(@NonNull String url) {
        return Completable.defer(() -> {
            if (TestingManager.isTestResult(url)) {
                String[] parts = url.split("#", 2);
                if (parts.length == 2) {
                    return importTestResult(parts[1])
                            .doFinally(() -> application.onDeepLinkHandled(url));
                }
            }
            return Completable.complete();
        });
    }

}
