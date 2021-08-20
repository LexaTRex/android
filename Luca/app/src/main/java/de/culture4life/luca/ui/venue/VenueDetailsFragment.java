package de.culture4life.luca.ui.venue;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.ncorti.slidetoact.SlideToActView;
import com.tbruyelle.rxpermissions3.Permission;

import de.culture4life.luca.R;
import de.culture4life.luca.ui.BaseFragment;
import de.culture4life.luca.ui.ViewError;
import de.culture4life.luca.ui.dialog.BaseDialogFragment;
import de.culture4life.luca.util.AccessibilityServiceUtil;
import five.star.me.FiveStarMe;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class VenueDetailsFragment extends BaseFragment<VenueDetailsViewModel> {

    private static final int REQUEST_ENABLE_LOCATION_SERVICES = 2;

    private TextView subtitle;
    private TextView title;
    private TextView descriptionTextView;
    private TextView additionalDataTitleTextView;
    private TextView additionalDataValueTextView;
    private TextView childCounterTextView;
    private ImageView childAddingImageView;
    private TextView checkInDurationHeadingTextView;
    private TextView checkInDurationTextView;
    private ImageView automaticCheckOutInfoImageView;
    private SwitchMaterial automaticCheckoutSwitch;
    private SlideToActView slideToActView;
    private Completable handleGrantedLocationAccess;
    private Completable handleDeniedLocationAccess;

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_venue_details;
    }

    @Override
    protected Class<VenueDetailsViewModel> getViewModelClass() {
        return VenueDetailsViewModel.class;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!viewModel.getIsCheckedIn().getValue()) {
            // navigation can be skipped if app is not open and user gets checked out by server or
            // via the notification
            safeNavigateFromNavController(R.id.action_venueDetailFragment_to_qrCodeFragment);
            AccessibilityServiceUtil.speak(getContext(), getString(R.string.venue_checked_out));
        }
    }

    @Override
    protected Completable initializeViews() {
        return super.initializeViews()
                .andThen(Completable.fromAction(() -> {
                    subtitle = getView().findViewById(R.id.subtitle);
                    observe(viewModel.getSubtitle(), value -> {
                        subtitle.setText(value);
                        subtitle.setVisibility(value == null ? View.GONE : View.VISIBLE);
                    });

                    title = getView().findViewById(R.id.title);
                    observe(viewModel.getTitle(), value -> title.setText(value));

                    descriptionTextView = getView().findViewById(R.id.subHeadingTextView);
                    observe(viewModel.getDescription(), value -> descriptionTextView.setText(value));

                    additionalDataTitleTextView = getView().findViewById(R.id.additionalDataTitleTextView);
                    observe(viewModel.getAdditionalDataTitle(), value -> additionalDataTitleTextView.setText(value));
                    additionalDataValueTextView = getView().findViewById(R.id.additionalDataValueTextView);
                    observe(viewModel.getAdditionalDataValue(), value -> additionalDataValueTextView.setText(value));
                    observe(viewModel.getShowAdditionalData(), value -> setAdditionalDataVisibility(value ? View.VISIBLE : View.GONE));

                    childCounterTextView = getView().findViewById(R.id.childCounterTextView);
                    childCounterTextView.setOnClickListener(view -> viewModel.openChildrenView());
                    observe(viewModel.getChildCounter(), counter -> {
                        if (counter == 0) {
                            childCounterTextView.setVisibility(View.GONE);
                        } else {
                            childCounterTextView.setVisibility(View.VISIBLE);
                            childCounterTextView.setText(String.valueOf(counter));
                        }
                    });

                    childAddingImageView = getView().findViewById(R.id.childAddingIconImageView);
                    childAddingImageView.setOnClickListener(view -> viewModel.openChildrenView());

                    checkInDurationHeadingTextView = getView().findViewById(R.id.checkInDurationHeadingTextView);

                    checkInDurationTextView = getView().findViewById(R.id.checkInDurationTextView);
                    observe(viewModel.getCheckInDuration(), value -> checkInDurationTextView.setText(value));

                    initializeAutomaticCheckoutViews();
                    initializeSlideToActView();
                }));
    }

    private void initializeAutomaticCheckoutViews() {
        automaticCheckOutInfoImageView = getView().findViewById(R.id.automaticCheckoutInfoImageView);
        automaticCheckOutInfoImageView.setOnClickListener(view -> showAutomaticCheckOutInfoDialog());

        automaticCheckoutSwitch = getView().findViewById(R.id.automaticCheckoutToggle);

        observe(viewModel.getHasLocationRestriction(), hasLocationRestriction -> updateAutoCheckoutViewsVisibility());
        observe(viewModel.getIsGeofencingSupported(), isGeofencingSupported -> updateAutoCheckoutViewsVisibility());

        automaticCheckoutSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (automaticCheckoutSwitch.isEnabled() && isChecked) {
                viewModel.isLocationConsentGiven()
                        .flatMapCompletable(isConsentGiven -> {
                            if (isConsentGiven) {
                                viewModel.enableAutomaticCheckout();
                            } else {
                                showGrantLocationAccessDialog();
                            }
                            return Completable.complete();
                        })
                        .subscribeOn(Schedulers.io())
                        .subscribe();
            } else {
                viewModel.disableAutomaticCheckout();
            }
        });

        observe(viewModel.getShouldEnableAutomaticCheckOut(), isActive -> automaticCheckoutSwitch.setChecked(isActive));

        observe(viewModel.getShouldEnableLocationServices(), shouldEnable -> {
            if (shouldEnable && !viewModel.isLocationServiceEnabled()) {
                handleGrantedLocationAccess = Completable.fromAction(() -> {
                    automaticCheckoutSwitch.setEnabled(false);
                    automaticCheckoutSwitch.setChecked(true);
                    automaticCheckoutSwitch.setEnabled(true);
                    viewModel.enableAutomaticCheckout();
                });
                handleDeniedLocationAccess = Completable.fromAction(() -> automaticCheckoutSwitch.setChecked(false));
                showLocationServicesDisabledDialog();
            }
        });
    }

    private void initializeSlideToActView() {
        slideToActView = getView().findViewById(R.id.slideToActView);
        slideToActView.setOnSlideCompleteListener(view -> viewModel.onSlideCompleted());
        slideToActView.setOnSlideUserFailedListener((view, isOutside) -> {
            if (AccessibilityServiceUtil.isGoogleTalkbackActive(getContext())) {
                viewModel.onSlideCompleted();
            } else {
                Toast.makeText(getContext(), R.string.venue_slider_clicked, Toast.LENGTH_SHORT).show();
            }
        });

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1) {
            // Work-around because resetSlider fails on SDK 22 in onDraw():
            //  java.lang.IllegalArgumentException: width and height must be > 0
            //    at com.ncorti.slidetoact.SlideToActView.onDraw(SlideToActView.kt:525)
            slideToActView.setAnimateCompletion(false);
        }

        observe(viewModel.getIsCheckedIn(), isCheckedIn -> {
            slideToActView.setReversed(isCheckedIn);
            slideToActView.setText(getString(isCheckedIn ? R.string.venue_check_out_action : R.string.venue_check_in_action));
            slideToActView.setContentDescription(getString(isCheckedIn ? R.string.venue_check_out_content_description : R.string.venue_check_in_content_description));
            checkInDurationHeadingTextView.setVisibility(isCheckedIn ? View.VISIBLE : View.GONE);
            checkInDurationTextView.setVisibility(isCheckedIn ? View.VISIBLE : View.GONE);
            if (!isCheckedIn) {
                safeNavigateFromNavController(R.id.action_venueDetailFragment_to_qrCodeFragment);
                AccessibilityServiceUtil.speak(getContext(), getString(R.string.venue_checked_out));
                FiveStarMe.showRateDialogIfMeetsConditions(getActivity());
            }
        });

        observe(viewModel.getIsLoading(), loading -> {
            if (!loading) {
                slideToActView.resetSlider();
            }
        });
    }

    private void updateAutoCheckoutViewsVisibility() {
        boolean enable = viewModel.getHasLocationRestriction().getValue() && viewModel.getIsGeofencingSupported().getValue();
        getView().findViewById(R.id.automaticCheckOutTextView).setVisibility(enable ? View.VISIBLE : View.GONE);
        getView().findViewById(R.id.automaticCheckoutInfoImageView).setVisibility(enable ? View.VISIBLE : View.GONE);
        automaticCheckoutSwitch.setVisibility(enable ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_ENABLE_LOCATION_SERVICES) {
            return;
        }
        viewDisposable.add(Completable.defer(() -> {
            if (viewModel.isLocationServiceEnabled()) {
                Timber.i("Successfully enabled location services");
                return handleGrantedLocationAccess;
            } else {
                Timber.i("Failed to enable location services");
                return handleDeniedLocationAccess;
            }
        })
                .doOnError(throwable -> Timber.e("Unable to handle location service change: %s", throwable.toString()))
                .onErrorComplete()
                .doFinally(this::clearRequestResultActions)
                .subscribe());
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPermissionResult(Permission permission) {
        super.onPermissionResult(permission);
        boolean isLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION.equals(permission.name);
        boolean isBackgroundLocationPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION.equals(permission.name);
        if (permission.granted || !(isLocationPermission || isBackgroundLocationPermission)) {
            return;
        }
        if (permission.shouldShowRequestPermissionRationale) {
            showRequestLocationPermissionRationale(permission, false);
        } else {
            showLocationPermissionPermanentlyDeniedError(permission);
        }
    }

    private void showRequestLocationPermissionRationale(@NonNull Permission permission, boolean permanentlyDenied) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext())
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> viewModel.onEnablingAutomaticCheckOutFailed())
                .setOnCancelListener(dialogInterface -> viewModel.onEnablingAutomaticCheckOutFailed())
                .setOnDismissListener(dialogInterface -> viewModel.onEnablingAutomaticCheckOutFailed());

        if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission.name) || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            builder.setTitle(R.string.auto_checkout_location_access_title);
            builder.setMessage(R.string.auto_checkout_location_access_description);
        } else {
            builder.setTitle(R.string.auto_checkout_background_location_access_title);
            builder.setMessage(getString(R.string.auto_checkout_background_location_access_description, application.getPackageManager().getBackgroundPermissionOptionLabel()));
        }

        if (permanentlyDenied) {
            builder.setPositiveButton(R.string.action_settings, (dialog, which) -> application.openAppSettings());
        } else {
            builder.setPositiveButton(R.string.action_grant, (dialog, which) -> {
                if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission.name)) {
                    viewModel.requestLocationPermissionForAutomaticCheckOut();
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    viewModel.requestBackgroundLocationPermissionForAutomaticCheckOut();
                }
            });
        }

        new BaseDialogFragment(builder).show();
    }

    private void showAutomaticCheckOutInfoDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.auto_checkout_info_title)
                .setMessage(R.string.auto_checkout_info_description)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> dialog.cancel());
        new BaseDialogFragment(builder).show();
    }

    private void showGrantLocationAccessDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.auto_checkout_location_access_title)
                .setMessage(R.string.auto_checkout_location_access_description)
                .setPositiveButton(R.string.action_enable, (dialog, which) -> {
                    viewModel.setLocationConsentGiven();
                    viewModel.enableAutomaticCheckout();
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                    automaticCheckoutSwitch.setChecked(false);
                    dialog.cancel();
                });
        new BaseDialogFragment(builder).show();
    }

    private void showLocationServicesDisabledDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.auto_checkout_enable_location_title)
                .setMessage(R.string.auto_checkout_enable_location_description)
                .setPositiveButton(R.string.action_settings, (dialog, which) -> requestLocationServiceActivation())
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> handleDeniedLocationAccess.onErrorComplete()
                        .doFinally(this::clearRequestResultActions)
                        .subscribe());
        new BaseDialogFragment(builder).show();
    }

    private void requestLocationServiceActivation() {
        Timber.d("Requesting to enable location services");
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent, REQUEST_ENABLE_LOCATION_SERVICES);
    }

    private void clearRequestResultActions() {
        handleGrantedLocationAccess = null;
        handleDeniedLocationAccess = null;
    }

    private void showLocationPermissionPermanentlyDeniedError(@NonNull Permission permission) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        ViewError viewError = new ViewError.Builder(context)
                .withTitle(getString(R.string.missing_permission_arg, getString(R.string.permission_name_location)))
                .withDescription(getString(R.string.missing_permission_arg, getString(R.string.permission_name_location)))
                .withResolveLabel(getString(R.string.action_resolve))
                .withResolveAction(Completable.fromAction(() -> showRequestLocationPermissionRationale(permission, true)))
                .build();

        showErrorAsSnackbar(viewError);
    }

    private void setAdditionalDataVisibility(int visibility) {
        additionalDataTitleTextView.setVisibility(visibility);
        additionalDataValueTextView.setVisibility(visibility);
    }

}