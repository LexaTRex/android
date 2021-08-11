package de.culture4life.luca.ui;

import android.app.Application;
import android.content.ActivityNotFoundException;

import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;

import com.tbruyelle.rxpermissions3.Permission;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.culture4life.luca.LucaApplication;
import de.culture4life.luca.R;
import de.culture4life.luca.checkin.CheckInManager;
import de.culture4life.luca.document.DocumentManager;
import de.culture4life.luca.meeting.MeetingManager;
import de.culture4life.luca.notification.LucaNotificationManager;
import de.culture4life.luca.preference.PreferencesManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

import static de.culture4life.luca.notification.LucaNotificationManager.NOTIFICATION_ID_EVENT;

public abstract class BaseViewModel extends AndroidViewModel {

    public static final String KEY_CAMERA_CONSENT_GIVEN = "camera_consent_given";

    protected final LucaApplication application;
    protected final CompositeDisposable modelDisposable = new CompositeDisposable();
    protected final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    protected final MutableLiveData<Set<ViewError>> errors = new MutableLiveData<>();
    protected final MutableLiveData<ViewEvent<? extends Set<String>>> requiredPermissions = new MutableLiveData<>();
    protected final MutableLiveData<Boolean> showCameraPreview = new MutableLiveData<>();
    protected NavController navigationController;
    private ViewError deleteAccountError;
    private PreferencesManager preferencesManager;

    public BaseViewModel(@NonNull Application application) {
        super(application);
        this.application = (LucaApplication) application;
        this.preferencesManager = this.application.getPreferencesManager();
        this.isLoading.setValue(false);
        this.errors.setValue(new HashSet<>());
        Timber.d("Created %s", this);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        modelDisposable.dispose();
    }

    @CallSuper
    public Completable initialize() {
        return updateRequiredPermissions()
                .andThen(preferencesManager.initialize(application))
                .andThen(update(showCameraPreview, false))
                .andThen(navigateForDeepLinkIfAvailable())
                .doOnSubscribe(disposable -> Timber.d("Initializing %s", this));
    }

    @CallSuper
    public Completable keepDataUpdated() {
        return Completable.never();
    }

    protected final <ValueType> Completable update(@NonNull MutableLiveData<ValueType> mutableLiveData, ValueType value) {
        return Completable.fromAction(() -> updateAsSideEffect(mutableLiveData, value));
    }

    protected final <ValueType> void updateAsSideEffect(@NonNull MutableLiveData<ValueType> mutableLiveData, ValueType value) {
        mutableLiveData.postValue(value);
    }

    protected final Completable updateRequiredPermissions() {
        return createRequiredPermissions()
                .distinct()
                .toList()
                .map(HashSet::new)
                .map(ViewEvent::new)
                .flatMapCompletable(permissions -> update(requiredPermissions, permissions));
    }

    @CallSuper
    protected Observable<String> createRequiredPermissions() {
        return Observable.empty();
    }

    protected void addPermissionToRequiredPermissions(String... newlyRequiredPermissions) {
        addPermissionToRequiredPermissions(new HashSet<>(Arrays.asList(newlyRequiredPermissions)));
    }

    protected void addPermissionToRequiredPermissions(Set<String> newlyRequiredPermissions) {
        Timber.v("Added permissions to be requested: %s", newlyRequiredPermissions);
        ViewEvent<? extends Set<String>> permissions = this.requiredPermissions.getValue();
        if (permissions != null && !permissions.hasBeenHandled()) {
            newlyRequiredPermissions.addAll(permissions.getValueAndMarkAsHandled());
        }
        updateAsSideEffect(requiredPermissions, new ViewEvent<>(newlyRequiredPermissions));
    }

    /**
     * Will add the specified error to {@link #errors} when subscribed and remove it when disposed.
     */
    protected final Completable addErrorUntilDisposed(@NonNull ViewError viewError) {
        return Completable.create(emitter -> {
            addError(viewError);
            emitter.setCancellable(() -> removeError(viewError));
        });
    }

    protected ViewError.Builder createErrorBuilder(@NonNull Throwable throwable) {
        return new ViewError.Builder(application)
                .withCause(throwable);
    }

    protected final void addError(@Nullable ViewError viewError) {
        modelDisposable.add(Completable.fromAction(
                () -> {
                    if (viewError == null) {
                        return;
                    }
                    if (!application.isUiCurrentlyVisible() && viewError.canBeShownAsNotification()) {
                        showErrorAsNotification(viewError);
                    } else {
                        synchronized (errors) {
                            Set<ViewError> errorSet = new HashSet<>(errors.getValue());
                            errorSet.add(viewError);
                            errors.setValue(Collections.unmodifiableSet(errorSet));
                        }
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> Timber.d("Added error: %s", viewError),
                        throwable -> Timber.w("Unable to add error: %s: %s", viewError, throwable.toString())
                ));
    }

    protected void showErrorAsNotification(@NonNull ViewError error) {
        LucaNotificationManager notificationManager = application.getNotificationManager();
        NotificationCompat.Builder notificationBuilder;
        if (error.isExpected()) {
            notificationBuilder = notificationManager
                    .createErrorNotificationBuilder(MainActivity.class, error.getTitle(), error.getDescription());
        } else {
            notificationBuilder = notificationManager
                    .createErrorNotificationBuilder(MainActivity.class, error.getTitle(), application.getString(R.string.error_specific_description, error.getDescription()));
        }

        notificationManager.showNotification(NOTIFICATION_ID_EVENT, notificationBuilder.build())
                .subscribe();
    }

    public final void removeError(@Nullable ViewError viewError) {
        modelDisposable.add(Completable.fromAction(
                () -> {
                    if (viewError == null) {
                        return;
                    }
                    synchronized (errors) {
                        Set<ViewError> errorSet = new HashSet<>(errors.getValue());
                        boolean removed = errorSet.remove(viewError);
                        if (removed) {
                            errors.setValue(Collections.unmodifiableSet(errorSet));
                        } else {
                            throw new IllegalStateException("Error was not added before");
                        }
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> Timber.d("Removed error: %s", viewError),
                        throwable -> Timber.w("Unable to remove error: %s: %s", viewError, throwable.toString())
                ));
    }

    public void onErrorShown(@Nullable ViewError viewError) {
        Timber.d("onErrorShown() called with: viewError = [%s]", viewError);
        if (viewError != null && viewError.getRemoveWhenShown()) {
            removeError(viewError);
        }
    }

    public void onErrorDismissed(@Nullable ViewError viewError) {
        Timber.d("onErrorDismissed() called with: viewError = [%s]", viewError);
        removeError(viewError);
    }

    @CallSuper
    public void onPermissionResult(@NonNull Permission permission) {
        Timber.i("Permission result: %s", permission);
    }

    private Completable navigateForDeepLinkIfAvailable() {
        return application.getDeepLink()
                .flatMap(url -> {
                    if (CheckInManager.isSelfCheckInUrl(url) || MeetingManager.isPrivateMeeting(url)) {
                        return Maybe.just(R.id.qrCodeFragment);
                    } else if (DocumentManager.isTestResult(url) || DocumentManager.isAppointment(url)) {
                        return Maybe.just(R.id.myLucaFragment);
                    } else {
                        return Maybe.empty();
                    }
                })
                .flatMapCompletable(destinationId -> Completable.fromAction(() -> {
                    if (!isCurrentDestinationId(destinationId)) {
                        navigationController.navigate(destinationId);
                    }
                }));
    }

    public void requestSupportMail() {
        try {
            this.application.openSupportMailIntent();
        } catch (ActivityNotFoundException exception) {
            addError(createErrorBuilder(exception)
                    .withTitle(R.string.menu_support_error_title)
                    .withDescription(R.string.menu_support_error_description)
                    .removeWhenShown()
                    .build());
        }
    }

    protected boolean isCurrentDestinationId(@IdRes int destinationId) {
        NavDestination currentDestination = navigationController.getCurrentDestination();
        return currentDestination != null && currentDestination.getId() == destinationId;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public final LiveData<Set<ViewError>> getErrors() {
        return errors;
    }

    public final LiveData<ViewEvent<? extends Set<String>>> getRequiredPermissionsViewEvent() {
        return requiredPermissions;
    }

    public void setNavigationController(NavController navigationController) {
        this.navigationController = navigationController;
    }

    public Single<Boolean> isCameraConsentGiven() {
        return preferencesManager.restoreOrDefault(KEY_CAMERA_CONSENT_GIVEN, false);
    }

    public void setCameraConsentAccepted() {
        modelDisposable.add(preferencesManager.persist(KEY_CAMERA_CONSENT_GIVEN, true)
                .subscribeOn(Schedulers.io())
                .subscribe()
        );
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    /**
     * Delete the account data on backend and clear data locally. Restart the app from scratch when
     * successful, show error dialog when an error occurred.
     */
    public void deleteAccount() {
        modelDisposable.add(application.getDocumentManager().unredeemAndDeleteAllDocuments()
                .andThen(application.getRegistrationManager().deleteRegistrationOnBackend())
                .doOnSubscribe(disposable -> {
                    updateAsSideEffect(isLoading, true);
                    removeError(deleteAccountError);
                })
                .andThen(application.getRegistrationManager().deleteRegistrationData())
                .andThen(application.getCryptoManager().deleteAllKeyStoreEntries())
                .andThen(application.getPreferencesManager().deleteAll())
                .doFinally(() -> updateAsSideEffect(isLoading, false))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    Timber.i("Account deleted");
                    application.restart();
                }, throwable -> {
                    Timber.w("Unable to delete account: %s", throwable);
                    deleteAccountError = createErrorBuilder(throwable)
                            .withTitle(R.string.error_request_failed_title)
                            .build();
                    addError(deleteAccountError);
                }));
    }

    public void showCameraPreview(boolean isActive) {
        showCameraPreview.postValue(isActive);
    }

    public MutableLiveData<Boolean> getShowCameraPreview() {
        return showCameraPreview;
    }

}