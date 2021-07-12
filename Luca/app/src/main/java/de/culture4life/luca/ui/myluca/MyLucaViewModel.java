package de.culture4life.luca.ui.myluca;

import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import android.annotation.SuppressLint;
import android.app.Application;
import android.webkit.URLUtil;

import de.culture4life.luca.R;
import de.culture4life.luca.checkin.CheckInManager;
import de.culture4life.luca.document.Document;
import de.culture4life.luca.document.DocumentAlreadyImportedException;
import de.culture4life.luca.document.DocumentExpiredException;
import de.culture4life.luca.document.DocumentManager;
import de.culture4life.luca.document.DocumentParsingException;
import de.culture4life.luca.document.DocumentVerificationException;
import de.culture4life.luca.document.TestResultPositiveException;
import de.culture4life.luca.meeting.MeetingManager;
import de.culture4life.luca.notification.LucaNotificationManager;
import de.culture4life.luca.registration.RegistrationManager;
import de.culture4life.luca.ui.BaseViewModel;
import de.culture4life.luca.ui.ViewError;
import de.culture4life.luca.ui.ViewEvent;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import dgca.verifier.app.decoder.BuildConfig;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class MyLucaViewModel extends BaseViewModel implements ImageAnalysis.Analyzer {

    private final DocumentManager documentManager;
    private final LucaNotificationManager notificationManager;
    private final RegistrationManager registrationManager;

    private final MutableLiveData<String> userName = new MutableLiveData<>();
    private final MutableLiveData<List<MyLucaListItem>> myLucaItems = new MutableLiveData<>();
    private final MutableLiveData<ViewEvent<Document>> parsedDocument = new MutableLiveData<>();
    private final MutableLiveData<ViewEvent<Document>> addedDocument = new MutableLiveData<>();

    private final BarcodeScanner scanner;

    private Disposable imageProcessingDisposable;
    private ViewError importError;

    public MyLucaViewModel(@NonNull Application application) {
        super(application);
        this.documentManager = this.application.getTestingManager();
        this.notificationManager = this.application.getNotificationManager();
        this.registrationManager = this.application.getRegistrationManager();
        this.scanner = BarcodeScanning.getClient();
        this.isLoading.setValue(false);
    }

    @Override
    public Completable initialize() {
        return super.initialize()
                .andThen(Completable.mergeArray(
                        documentManager.initialize(application),
                        notificationManager.initialize(application),
                        registrationManager.initialize(application)
                ))
                .andThen(updateUserName())
                .andThen(invokeListUpdate())
                .andThen(handleApplicationDeepLinkIfAvailable());
    }

    public Completable invokeListUpdate() {
        return Completable.fromAction(() -> modelDisposable.add(updateList()
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Timber.d("Updated my luca list"),
                        throwable -> Timber.w("Unable to update my luca list: %s", throwable.toString())
                )));
    }

    public Completable updateUserName() {
        return registrationManager.getOrCreateRegistrationData()
                .flatMapCompletable(registrationData -> update(userName, registrationData.getFullName()));
    }

    private Completable updateList() {
        return loadListItems()
                .toList()
                .flatMapCompletable(items -> update(myLucaItems, items));
    }

    private Observable<MyLucaListItem> loadListItems() {
        return Completable.defer(() -> BuildConfig.DEBUG ? Completable.complete() : documentManager.deleteExpiredDocuments())
                .andThen(documentManager.getOrRestoreDocuments())
                .flatMapMaybe(this::createListItem)
                .doOnNext(myLucaListItem -> Timber.d("Created list item: %s", myLucaListItem))
                .sorted((first, second) -> Long.compare(second.getTimestamp(), first.getTimestamp()));
    }

    private Maybe<MyLucaListItem> createListItem(@NonNull Document document) {
        return Maybe.fromCallable(() -> {
            if (document.getType() == Document.TYPE_GREEN_PASS) {
                return new GreenPassItem(application, document);
            } else if (document.getType() == Document.TYPE_APPOINTMENT) {
                return new AppointmentItem(application, document);
            } else if (document.getType() == Document.TYPE_VACCINATION) {
                return new VaccinationItem(application, document);
            } else if (document.getType() == Document.TYPE_RECOVERY) {
                return new RecoveryItem(application, document);
            } else {
                return new TestResultItem(application, document);
            }
        });
    }

    public Completable deleteListItem(@NonNull MyLucaListItem myLucaListItem) {
        return Completable.defer(() -> {
            if (myLucaListItem instanceof TestResultItem) {
                return documentManager.deleteDocument(((TestResultItem) myLucaListItem).getDocument().getId())
                        .andThen(invokeListUpdate());
            } else if (myLucaListItem instanceof GreenPassItem) {
                return documentManager.deleteDocument(((GreenPassItem) myLucaListItem).getDocument().getId())
                        .andThen(invokeListUpdate());
            } else {
                return Completable.error(new IllegalArgumentException("Unable to delete item, unknown type"));
            }
        });
    }

    public void onAppointmentRequested() {
        application.openUrl("https://www.luca-app.de/coronatest");
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
                .flatMapSingle(barcodeData -> {
                    if (DocumentManager.isTestResult(barcodeData)) {
                        return getEncodedDocumentFromDeepLink(barcodeData);
                    } else {
                        return Single.just(barcodeData);
                    }
                })
                .flatMapCompletable(this::parseAndValidateDocument);
    }

    private Completable parseAndValidateDocument(@NonNull String encodedDocument) {
        return documentManager.parseAndValidateEncodedDocument(encodedDocument)
                .doOnSubscribe(disposable -> Timber.d("Attempting to parse encoded document: %s", encodedDocument))
                .doOnSuccess(testResult -> Timber.d("Parsed document: %s", testResult))
                .flatMapCompletable(testResult -> update(parsedDocument, new ViewEvent<>(testResult)))
                .doOnSubscribe(disposable -> {
                    removeError(importError);
                    updateAsSideEffect(showCameraPreview, false);
                    updateAsSideEffect(isLoading, true);
                })
                .doOnError(throwable -> {
                    Timber.w("Unable to parse document: %s", throwable.toString());
                    ViewError.Builder errorBuilder = createErrorBuilder(throwable)
                            .withTitle(R.string.document_import_error_title);

                    if (throwable instanceof DocumentParsingException) {
                        if (MeetingManager.isPrivateMeeting(encodedDocument) || CheckInManager.isSelfCheckInUrl(encodedDocument)) {
                            // the user tried to check-in from the wrong tab
                            errorBuilder.withTitle(R.string.document_import_error_check_in_scanner_title);
                            errorBuilder.withDescription(R.string.document_import_error_check_in_scanner_description);
                        } else if (URLUtil.isValidUrl(encodedDocument) && !DocumentManager.isTestResult(encodedDocument)) {
                            // data is actually an URL that the user may want to open
                            errorBuilder.withDescription(R.string.document_import_error_unsupported_but_url_description);
                            errorBuilder.withResolveLabel(R.string.action_continue);
                            errorBuilder.withResolveAction(Completable.fromAction(() -> application.openUrl(encodedDocument)));
                        } else {
                            errorBuilder.withDescription(R.string.document_import_error_unsupported_description);
                        }
                    } else if (throwable instanceof DocumentExpiredException) {
                        errorBuilder.withDescription(R.string.document_import_error_expired_description);
                    } else if (throwable instanceof DocumentVerificationException) {
                        switch (((DocumentVerificationException) throwable).getReason()) {
                            case NAME_MISMATCH:
                                errorBuilder.withDescription(R.string.document_import_error_name_mismatch_description);
                                break;
                            case INVALID_SIGNATURE:
                                errorBuilder.withDescription(R.string.document_import_error_invalid_signature_description);
                                break;
                        }
                    }

                    importError = errorBuilder.build();
                    addError(importError);
                })
                .doFinally(() -> updateAsSideEffect(isLoading, false))
                .subscribeOn(Schedulers.io());
    }

    public Completable addDocument(@NonNull Document document) {
        return documentManager.redeemDocument(document)
                .andThen(documentManager.addDocument(document))
                .andThen(update(addedDocument, new ViewEvent<>(document)))
                .andThen(invokeListUpdate())
                .doOnSubscribe(disposable -> {
                    removeError(importError);
                    updateAsSideEffect(isLoading, true);
                })
                .doOnError(throwable -> {
                    ViewError.Builder errorBuilder = createErrorBuilder(throwable)
                            .withTitle(R.string.document_import_error_title);

                    boolean outcomeUnknown = false;
                    if (throwable instanceof DocumentVerificationException) {
                        outcomeUnknown = ((DocumentVerificationException) throwable).getReason() == DocumentVerificationException.Reason.OUTCOME_UNKNOWN;
                    }
                    if (throwable instanceof TestResultPositiveException || outcomeUnknown) {
                        errorBuilder
                                .withTitle(R.string.document_import_error_not_negative_title)
                                .withDescription(R.string.document_import_error_not_negative_description);
                    } else if (throwable instanceof DocumentAlreadyImportedException) {
                        errorBuilder.withDescription(R.string.document_import_error_already_imported_description);
                    } else if (throwable instanceof DocumentExpiredException) {
                        errorBuilder.withDescription(R.string.document_import_error_expired_description);
                    }

                    importError = errorBuilder.build();
                    addError(importError);
                })
                .doFinally(() -> updateAsSideEffect(isLoading, false));
    }

    /*
        Deep link handling
     */

    private Completable handleApplicationDeepLinkIfAvailable() {
        return application.getDeepLink()
                .flatMapCompletable(this::handleDeepLink)
                .onErrorComplete();
    }

    private Completable handleDeepLink(@NonNull String url) {
        return Completable.defer(() -> {
            if (DocumentManager.isTestResult(url)) {
                return getEncodedDocumentFromDeepLink(url)
                        .flatMapCompletable(this::parseAndValidateDocument)
                        .doFinally(() -> application.onDeepLinkHandled(url));
            } else if (DocumentManager.isAppointment(url)) {
                return parseAndValidateDocument(url)
                        .doFinally(() -> application.onDeepLinkHandled(url));
            }
            return Completable.complete();
        }).doOnComplete(() -> Timber.d("Handled application deep link: %s", url));
    }

    private Single<String> getEncodedDocumentFromDeepLink(@NonNull String url) {
        return Single.fromCallable(() -> {
            if (!DocumentManager.isTestResult(url)) {
                throw new IllegalArgumentException("Unable to get encoded document from URL");
            }
            String[] parts = url.split("#", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Unable to get encoded document from URL");
            }
            return parts[1];
        });
    }

    public LiveData<List<MyLucaListItem>> getMyLucaItems() {
        return myLucaItems;
    }

    public LiveData<ViewEvent<Document>> getParsedDocument() {
        return parsedDocument;
    }

    public LiveData<ViewEvent<Document>> getAddedDocument() {
        return addedDocument;
    }

    public LiveData<String> getUserName() {
        return userName;
    }

}
