package de.culture4life.luca.ui.myluca;

import android.app.Application;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;
import java.util.concurrent.TimeUnit;

import de.culture4life.luca.R;
import de.culture4life.luca.checkin.CheckInManager;
import de.culture4life.luca.document.Document;
import de.culture4life.luca.document.DocumentAlreadyImportedException;
import de.culture4life.luca.document.DocumentExpiredException;
import de.culture4life.luca.document.DocumentManager;
import de.culture4life.luca.document.DocumentParsingException;
import de.culture4life.luca.document.DocumentVerificationException;
import de.culture4life.luca.document.TestResultPositiveException;
import de.culture4life.luca.genuinity.GenuinityManager;
import de.culture4life.luca.meeting.MeetingManager;
import de.culture4life.luca.registration.RegistrationManager;
import de.culture4life.luca.ui.BaseQrCodeViewModel;
import de.culture4life.luca.ui.ViewError;
import de.culture4life.luca.ui.ViewEvent;
import de.culture4life.luca.ui.qrcode.QrCodeViewModel;
import dgca.verifier.app.decoder.BuildConfig;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class MyLucaViewModel extends BaseQrCodeViewModel {

    private final DocumentManager documentManager;
    private final RegistrationManager registrationManager;
    private final GenuinityManager genuinityManager;

    private final MutableLiveData<String> userName = new MutableLiveData<>();
    private final MutableLiveData<List<MyLucaListItem>> myLucaItems = new MutableLiveData<>();
    private final MutableLiveData<ViewEvent<Document>> parsedDocument = new MutableLiveData<>();
    private final MutableLiveData<ViewEvent<Document>> addedDocument = new MutableLiveData<>();
    private final MutableLiveData<ViewEvent<String>> possibleCheckInData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isGenuineTime = new MutableLiveData<>();

    private QrCodeViewModel qrCodeViewModel;

    private ViewError importError;
    private ViewError deleteError;

    public MyLucaViewModel(@NonNull Application application) {
        super(application);
        this.documentManager = this.application.getDocumentManager();
        this.registrationManager = this.application.getRegistrationManager();
        this.genuinityManager = this.application.getGenuinityManager();
    }

    public void setupViewModelReference(FragmentActivity activity) {
        if (qrCodeViewModel == null) {
            qrCodeViewModel = new ViewModelProvider(activity).get(QrCodeViewModel.class);
            qrCodeViewModel.setupViewModelReference(activity);
        }
    }

    @Override
    public Completable initialize() {
        return super.initialize()
                .andThen(Completable.mergeArray(
                        documentManager.initialize(application),
                        registrationManager.initialize(application),
                        genuinityManager.initialize(application)
                ))
                .andThen(updateUserName())
                .andThen(invokeListUpdate())
                .andThen(invokeIsGenuineTimeUpdate())
                .andThen(handleApplicationDeepLinkIfAvailable());
    }

    @Override
    public Completable keepDataUpdated() {
        return Completable.mergeArray(
                super.keepDataUpdated(),
                keepIsGenuineTimeUpdated()
        );
    }

    private Completable keepIsGenuineTimeUpdated() {
        return genuinityManager.getIsGenuineTimeChanges()
                .flatMapCompletable(genuineTime -> update(isGenuineTime, genuineTime));
    }

    public Completable invokeListUpdate() {
        return Completable.fromAction(() -> modelDisposable.add(updateList()
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Timber.d("Updated my luca list"),
                        throwable -> Timber.w("Unable to update my luca list: %s", throwable.toString())
                )));
    }

    private Completable invokeIsGenuineTimeUpdate() {
        return Completable.fromAction(() -> modelDisposable.add(genuinityManager.isGenuineTime()
                .flatMapCompletable(genuineTime -> update(isGenuineTime, genuineTime))
                .subscribe()));
    }

    public Completable invokeServerTimeOffsetUpdate() {
        return Completable.fromAction(() -> modelDisposable.add(genuinityManager.invokeServerTimeOffsetUpdate()
                .delaySubscription(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe()));
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
            if (document.getType() == Document.TYPE_APPOINTMENT) {
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
            Document document;
            if (myLucaListItem instanceof TestResultItem) {
                document = ((TestResultItem) myLucaListItem).getDocument();
            } else {
                return Completable.error(new IllegalArgumentException("Unable to delete item, unknown type"));
            }
            return documentManager.unredeemDocument(document)
                    .andThen(documentManager.deleteDocument(document.getId()))
                    .andThen(invokeListUpdate())
                    .doOnSubscribe(disposable -> {
                        removeError(deleteError);
                        updateAsSideEffect(isLoading, true);
                    })
                    .doOnError(throwable -> {
                        deleteError = new ViewError.Builder(application)
                                .withCause(throwable)
                                .removeWhenShown()
                                .build();
                        addError(deleteError);
                    })
                    .doFinally(() -> updateAsSideEffect(isLoading, false));
        });
    }

    public void onAppointmentRequested() {
        application.openUrl("https://www.luca-app.de/coronatest");
    }

    /*
        QR code scanning
     */

    @Override
    protected boolean canProcessImage() {
        // currently showing an import related error
        return importError == null || !errors.getValue().contains(importError);
    }

    public boolean canProcessBarcode(@NonNull String barcodeData) {
        return Single.defer(() -> {
            if (DocumentManager.isTestResult(barcodeData)) {
                return getEncodedDocumentFromDeepLink(barcodeData);
            } else {
                return Single.just(barcodeData);
            }
        })
                .flatMap(documentManager::parseAndValidateEncodedDocument)
                .map(document -> true)
                .onErrorReturnItem(false)
                .blockingGet();
    }

    @Override
    @NonNull
    protected Completable processBarcode(@NonNull String barcodeData) {
        return Completable.defer(() -> {
            if (qrCodeViewModel.canProcessBarcode(barcodeData)) {
                ViewEvent<String> barcodeDataEvent = new ViewEvent<>(barcodeData);
                return update(possibleCheckInData, barcodeDataEvent);
            } else {
                return process(barcodeData);
            }
        }).doOnSubscribe(disposable -> {
            removeError(importError);
            updateAsSideEffect(showCameraPreview, false);
            updateAsSideEffect(isLoading, true);
        }).doFinally(() -> updateAsSideEffect(isLoading, false));
    }

    public Completable process(@NonNull String barcodeData) {
        return Single.defer(
                () -> {
                    if (DocumentManager.isTestResult(barcodeData)) {
                        return getEncodedDocumentFromDeepLink(barcodeData);
                    } else {
                        return Single.just(barcodeData);
                    }
                })
                .doOnSuccess(value -> {
                    Timber.d("Processing barcodeData: %s", value);
                    getNotificationManager().vibrate().subscribe();
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

    public LiveData<ViewEvent<String>> getPossibleCheckInData() {
        return possibleCheckInData;
    }

    public LiveData<Boolean> getIsGenuineTime() {
        return isGenuineTime;
    }

}
