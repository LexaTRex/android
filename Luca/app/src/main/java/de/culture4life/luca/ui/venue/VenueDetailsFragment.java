package de.culture4life.luca.ui.venue;

import static de.culture4life.luca.ui.BaseQrCodeViewModel.BARCODE_DATA_KEY;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tbruyelle.rxpermissions3.Permission;

import de.culture4life.luca.R;
import de.culture4life.luca.databinding.FragmentVenueDetailsBinding;
import de.culture4life.luca.ui.BaseFragment;
import de.culture4life.luca.ui.ViewError;
import de.culture4life.luca.ui.dialog.BaseDialogFragment;
import de.culture4life.luca.util.AccessibilityServiceUtil;
import five.star.me.FiveStarMe;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class VenueDetailsFragment extends BaseFragment<VenueDetailsViewModel> {

    private static final int REQUEST_ENABLE_LOCATION_SERVICES = 2;

    private FragmentVenueDetailsBinding binding;

    private Completable handleGrantedLocationAccess;
    private Completable handleDeniedLocationAccess;

    @Nullable
    @Override
    protected ViewBinding getViewBinding() {
        binding = FragmentVenueDetailsBinding.inflate(getLayoutInflater());
        return binding;
    }

    @Override
    protected Class<VenueDetailsViewModel> getViewModelClass() {
        return VenueDetailsViewModel.class;
    }

    @Override
    protected Completable initializeViews() {
        return super.initializeViews()
                .andThen(Completable.fromAction(() -> {
                    observe(viewModel.getSubtitle(), value -> {
                        binding.subtitle.setText(value);
                        binding.subtitle.setVisibility(value == null ? View.GONE : View.VISIBLE);
                    });

                    observe(viewModel.getTitle(), value -> binding.title.setText(value));

                    observe(viewModel.getCheckInTime(), value -> binding.checkInTimeTextView.setText(getFormattedString(R.string.venue_checked_in_time, value)));

                    observe(viewModel.getAdditionalDataTitle(), value -> binding.additionalDataTitleTextView.setText(value));
                    observe(viewModel.getAdditionalDataValue(), value -> binding.additionalDataValueTextView.setText(value));
                    observe(viewModel.getShowAdditionalData(), value -> setAdditionalDataVisibility(value ? View.VISIBLE : View.GONE));

                    binding.childCounterTextView.setOnClickListener(view -> viewModel.openChildrenView());
                    observe(viewModel.getChildCounter(), counter -> {
                        if (counter == 0) {
                            binding.childCounterTextView.setVisibility(View.GONE);
                        } else {
                            binding.childCounterTextView.setVisibility(View.VISIBLE);
                            binding.childCounterTextView.setText(String.valueOf(counter));
                        }
                    });

                    binding.childAddingIconImageView.setOnClickListener(view -> viewModel.openChildrenView());
                    observe(viewModel.getCheckInDuration(), value -> binding.checkInDurationTextView.setText(value));

                    initializeAutomaticCheckoutViews();
                    initializeSlideToActView();

                    observe(viewModel.getBundle(), this::processBundle);
                }));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!viewModel.getIsCheckedIn().getValue()) {
            // navigation can be skipped if app is not open and user gets checked out by server or
            // via the notification
            safeNavigateFromNavController(R.id.action_venueDetailFragment_to_checkInFragment, viewModel.getBundle().getValue());
            AccessibilityServiceUtil.speak(getContext(), getString(R.string.venue_checked_out));
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            viewModel.setBundle(arguments);
        }

        viewDisposable.add(viewModel.updateLocationServicesStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .doOnError(throwable -> Timber.w("Error updating location services status. %s", throwable.getMessage()))
                .subscribe());
    }

    @Override
    public void onStop() {
        viewModel.setBundle(null);
        super.onStop();
    }

    private void initializeAutomaticCheckoutViews() {
        binding.automaticCheckoutInfoImageView.setOnClickListener(view -> showAutomaticCheckOutInfoDialog());
        observe(viewModel.getHasLocationRestriction(), hasLocationRestriction -> updateAutoCheckoutViewsVisibility());
        observe(viewModel.getIsGeofencingSupported(), isGeofencingSupported -> updateAutoCheckoutViewsVisibility());

        binding.automaticCheckoutToggle.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (binding.automaticCheckoutToggle.isEnabled() && isChecked) {
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

        binding.automaticCheckoutToggle.setOnClickListener(view -> viewModel.setAutomaticCheckoutActiveAsDefault(binding.automaticCheckoutToggle.isChecked()));

        observe(viewModel.getShouldEnableAutomaticCheckOut(), isActive -> binding.automaticCheckoutToggle.setChecked(isActive));

        observe(viewModel.getShouldEnableLocationServices(), shouldEnable -> {
            if (shouldEnable && !viewModel.isLocationServiceEnabled()) {
                handleGrantedLocationAccess = Completable.fromAction(() -> {
                    binding.automaticCheckoutToggle.setEnabled(false);
                    binding.automaticCheckoutToggle.setChecked(true);
                    binding.automaticCheckoutToggle.setEnabled(true);
                    viewModel.enableAutomaticCheckout();
                });
                handleDeniedLocationAccess = Completable.fromAction(() -> binding.automaticCheckoutToggle.setChecked(false));
                showLocationServicesDisabledDialog();
            }
        });
    }

    private void initializeSlideToActView() {
        binding.slideToActView.setOnSlideCompleteListener(view -> viewModel.onSlideCompleted());
        binding.slideToActView.setOnSlideUserFailedListener((view, isOutside) -> {
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
            binding.slideToActView.setAnimateCompletion(false);
        }

        observe(viewModel.getIsCheckedIn(), isCheckedIn -> {
            binding.slideToActView.setText(getString(isCheckedIn ? R.string.venue_check_out_action : R.string.venue_check_in_action));
            binding.slideToActView.setContentDescription(getString(isCheckedIn ? R.string.venue_check_out_content_description : R.string.venue_check_in_content_description));
            binding.checkInDurationTextView.setVisibility(isCheckedIn ? View.VISIBLE : View.GONE);
            if (!isCheckedIn) {
                safeNavigateFromNavController(R.id.action_venueDetailFragment_to_checkInFragment, viewModel.getBundle().getValue());
                AccessibilityServiceUtil.speak(getContext(), getString(R.string.venue_checked_out));
                FiveStarMe.showRateDialogIfMeetsConditions(getActivity());
            }
        });

        observe(viewModel.getIsLoading(), loading -> {
            if (!loading) {
                binding.slideToActView.resetSlider();
            }
        });
    }

    private void updateAutoCheckoutViewsVisibility() {
        boolean hasLocationRestriction = viewModel.getHasLocationRestriction().getValue();
        boolean enable = hasLocationRestriction && viewModel.getIsGeofencingSupported().getValue();
        binding.automaticCheckOutTextView.setVisibility(enable ? View.VISIBLE : View.GONE);
        binding.automaticCheckoutInfoImageView.setVisibility(enable ? View.VISIBLE : View.GONE);
        binding.automaticCheckoutToggle.setVisibility(hasLocationRestriction ? View.VISIBLE : View.GONE);
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

    private void processBundle(@Nullable Bundle bundle) {
        if (bundle == null) {
            return;
        }

        String barcode = bundle.getString(BARCODE_DATA_KEY);
        if (barcode != null) {
            // is supposed to check-in into different location
            showLocationChangeDialog();
        }
    }

    private void showRequestLocationPermissionRationale(@NonNull Permission permission, boolean permanentlyDenied) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
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
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.auto_checkout_info_title)
                .setMessage(R.string.auto_checkout_info_description)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> dialog.cancel());
        new BaseDialogFragment(builder).show();
    }

    private void showGrantLocationAccessDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.auto_checkout_location_access_title)
                .setMessage(R.string.auto_checkout_location_access_description)
                .setPositiveButton(R.string.action_enable, (dialog, which) -> {
                    viewModel.setLocationConsentGiven();
                    viewModel.enableAutomaticCheckout();
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                    binding.automaticCheckoutToggle.setChecked(false);
                    dialog.cancel();
                });
        new BaseDialogFragment(builder).show();
    }

    private void showLocationServicesDisabledDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.auto_checkout_enable_location_title)
                .setMessage(R.string.auto_checkout_enable_location_description)
                .setPositiveButton(R.string.action_settings, (dialog, which) -> requestLocationServiceActivation())
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                    if (handleDeniedLocationAccess != null) {
                        handleDeniedLocationAccess.onErrorComplete()
                                .doFinally(this::clearRequestResultActions)
                                .subscribe();
                    }
                });
        new BaseDialogFragment(builder).show();
    }

    private void showLocationChangeDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.venue_change_location_title)
                .setMessage(R.string.venue_change_location_description)
                .setPositiveButton(R.string.action_change, (dialog, which) -> viewModel.changeLocation())
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.cancel())
                .setOnCancelListener(dialogInterface -> viewModel.setBundle(null));
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
        binding.additionalDataTitleTextView.setVisibility(visibility);
        binding.additionalDataValueTextView.setVisibility(visibility);
    }

}
