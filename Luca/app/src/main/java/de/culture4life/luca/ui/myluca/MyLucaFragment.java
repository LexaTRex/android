package de.culture4life.luca.ui.myluca;

import static de.culture4life.luca.ui.BaseQrCodeViewModel.BARCODE_DATA_KEY;

import android.content.Intent;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import de.culture4life.luca.R;
import de.culture4life.luca.dataaccess.AccessedTraceData;
import de.culture4life.luca.databinding.FragmentMyLucaBinding;
import de.culture4life.luca.databinding.TopSheetContainerBinding;
import de.culture4life.luca.document.Document;
import de.culture4life.luca.registration.Person;
import de.culture4life.luca.ui.BaseFragment;
import de.culture4life.luca.ui.accesseddata.AccessedDataListItem;
import de.culture4life.luca.ui.dialog.BaseDialogFragment;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class MyLucaFragment extends BaseFragment<MyLucaViewModel> implements MyLucaListAdapter.MyLucaListClickListener {

    private MyLucaListAdapter myLucaListAdapter;
    private ProcessCameraProvider cameraProvider;
    private Disposable cameraPreviewDisposable;
    private FragmentMyLucaBinding binding;

    @Override
    protected ViewBinding getViewBinding() {
        binding = FragmentMyLucaBinding.inflate(getLayoutInflater());
        return binding;
    }

    @Override
    protected Class<MyLucaViewModel> getViewModelClass() {
        return MyLucaViewModel.class;
    }

    @Override
    public void onResume() {
        super.onResume();
        viewDisposable.add(Completable.mergeArray(
                viewModel.updateUserName(),
                viewModel.invokeListUpdate(),
                viewModel.invokeServerTimeOffsetUpdate()
        ).subscribe());

        Bundle arguments = getArguments();
        if (arguments != null) {
            String barcode = arguments.getString(BARCODE_DATA_KEY);
            if (barcode != null) {
                viewDisposable.add(viewModel.process(barcode)
                        .onErrorComplete()
                        .subscribe());
            }
        }
    }

    @Override
    protected Completable initializeViewModel() {
        return super.initializeViewModel()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> viewModel.setupViewModelReference(requireActivity()));
    }

    @Override
    protected Completable initializeViews() {
        return super.initializeViews()
                .andThen(Completable.fromAction(() -> {
                    initializeMyLucaItemsViews();
                    initializeImportViews();
                    initializeBanners();
                }));
    }

    private void initializeMyLucaItemsViews() {
        myLucaListAdapter = new MyLucaListAdapter(this, this);
        binding.myLucaRecyclerView.setAdapter(myLucaListAdapter);
        binding.myLucaRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.childAddingIconImageView.setOnClickListener(v -> viewModel.openChildrenView());
        binding.childCounterTextView.setOnClickListener(view -> viewModel.openChildrenView());
        observe(viewModel.getChildren(), children -> {
            if (children.size() == 0) {
                binding.childCounterTextView.setVisibility(View.GONE);
            } else {
                binding.childCounterTextView.setVisibility(View.VISIBLE);
                binding.childCounterTextView.setText(String.valueOf(children.size()));
            }
        });

        observe(viewModel.getMyLucaItems(), items -> {
            List<MyLucaListItemsWrapper> listItems = myLucaListAdapter.setItems(items, getPersons());
            int emptyStateVisibility = listItems.isEmpty() ? View.VISIBLE : View.GONE;
            int contentVisibility = !listItems.isEmpty() ? View.VISIBLE : View.GONE;
            binding.emptyStateScrollView.setVisibility(emptyStateVisibility);
            binding.myLucaRecyclerView.setVisibility(contentVisibility);
        });

        observe(viewModel.getItemToDelete(), viewEvent -> {
            if (!viewEvent.hasBeenHandled()) {
                showDeleteDocumentDialog(viewEvent.getValueAndMarkAsHandled());
            }
        });
        observe(viewModel.getItemToExpand(), viewEvent -> {
            if (!viewEvent.hasBeenHandled()) {
                MyLucaListItemsWrapper wrapper = myLucaListAdapter.getWrapperWith(viewEvent.getValueAndMarkAsHandled());
                for (MyLucaListItem wrapperItem : wrapper.getItems()) {
                    wrapperItem.toggleExpanded();
                }
                myLucaListAdapter.notifyDataSetChanged();
            }
        });
    }

    private ArrayList<Person> getPersons() {
        ArrayList<Person> persons = new ArrayList<>();
        persons.add(viewModel.getUser().getValue());
        persons.addAll(viewModel.getChildren().getValue());
        return persons;
    }

    private void initializeImportViews() {
        binding.cardView.setVisibility(View.GONE);
        binding.bookAppointmentImageView.setOnClickListener(v -> viewModel.onAppointmentRequested());
        binding.primaryActionButton.setOnClickListener(v -> toggleCameraPreview());

        observe(viewModel.getIsLoading(), loading -> binding.loadingLayout.setVisibility(loading ? View.VISIBLE : View.GONE));

        observe(viewModel.getParsedDocument(), documentViewEvent -> {
            if (!documentViewEvent.hasBeenHandled()) {
                Document document = documentViewEvent.getValueAndMarkAsHandled();
                showDocumentImportConsentDialog(document);
                hideCameraPreview();
            }
        });

        observe(viewModel.getAddedDocument(), documentViewEvent -> {
            if (!documentViewEvent.hasBeenHandled()) {
                documentViewEvent.setHandled(true);
                Toast.makeText(getContext(), R.string.document_import_success_message, Toast.LENGTH_SHORT).show();
            }
        });

        observe(viewModel.getShowCameraPreview(), isActive -> {
            if (isActive) {
                showCameraPreview();
            } else {
                hideCameraPreview();
            }
        });

        observe(viewModel.getPossibleCheckInData(), barcodeDataEvent -> {
            if (barcodeDataEvent != null && !barcodeDataEvent.hasBeenHandled()) {
                Bundle bundle = new Bundle();
                String barcodeData = barcodeDataEvent.getValueAndMarkAsHandled();
                bundle.putString(BARCODE_DATA_KEY, barcodeData);
                new BaseDialogFragment(new MaterialAlertDialogBuilder(getContext())
                        .setTitle(R.string.document_import_check_in_redirect_title)
                        .setMessage(R.string.document_import_check_in_redirect_description)
                        .setPositiveButton(R.string.action_continue, (dialogInterface, i) -> navigationController.navigate(R.id.checkInFragment, bundle))
                        .setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> {
                            // do nothing
                        })).show();
            }
        });

        observe(viewModel.getShowBirthDateHint(), documentViewEvent -> {
            if (!documentViewEvent.hasBeenHandled()) {
                Document document = documentViewEvent.getValueAndMarkAsHandled();
                new BaseDialogFragment(new MaterialAlertDialogBuilder(getContext())
                        .setTitle(R.string.document_import_error_different_birth_dates_title)
                        .setMessage(R.string.document_import_error_different_birth_dates_description)
                        .setPositiveButton(R.string.action_ok, (dialogInterface, i) -> dialogInterface.dismiss())
                        .setNeutralButton(R.string.document_import_anyway_action, (dialogInterface, i) -> {
                            viewDisposable.add(viewModel.addDocument(document)
                                    .onErrorComplete()
                                    .subscribeOn(Schedulers.io())
                                    .subscribe());
                        }))
                        .show();
            }
        });
    }

    private void initializeBanners() {
        observe(viewModel.getIsGenuineTime(), isGenuineTime -> refreshBanners(isGenuineTime, viewModel.getAccessNotificationsPerLevel().getValue()));
        observe(viewModel.getAccessNotificationsPerLevel(), accessNotifications -> refreshBanners(viewModel.getIsGenuineTime().getValue(), accessNotifications));
    }

    private void refreshBanners(@NonNull Boolean isGenuineTime, @NonNull HashMap<Integer, AccessedDataListItem> accessNotifications) {
        LinearLayout container = getView().findViewById(R.id.bannerLayout);
        container.removeAllViews();

        if (!isGenuineTime) {
            TopSheetContainerBinding bannerBinding = TopSheetContainerBinding.inflate(getLayoutInflater(), container, true);
            bannerBinding.sheetDescriptionTextView.setText(R.string.time_error_description);
            bannerBinding.sheetActionButton.setText(R.string.time_error_action);
            bannerBinding.sheetActionButton.setOnClickListener(v -> {
                Intent intent = new Intent(android.provider.Settings.ACTION_DATE_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            });
        }

        for (int warningLevel = 1; warningLevel <= AccessedTraceData.NUMBER_OF_WARNING_LEVELS; warningLevel++) {
            if (accessNotifications.containsKey(warningLevel)) {
                TopSheetContainerBinding bannerBinding = TopSheetContainerBinding.inflate(getLayoutInflater(), container, true);
                bannerBinding.sheetActionButton.setText(R.string.accessed_data_banner_action_show);
                bannerBinding.sheetIconImageView.setImageResource(R.drawable.ic_eye);
                bannerBinding.sheetDescriptionTextView.setText(accessNotifications.get(warningLevel).getBannerText());
                int finalWarningLevel = warningLevel;
                bannerBinding.sheetActionButton.setOnClickListener(v -> viewModel.onShowAccessedDataRequested(finalWarningLevel));
            }
        }
    }

    private void toggleCameraPreview() {
        if (cameraPreviewDisposable == null) {
            viewModel.isCameraConsentGiven()
                    .flatMapCompletable(isCameraConsentGiven -> {
                        if (isCameraConsentGiven) {
                            showCameraPreview();
                        } else {
                            showCameraDialog(false);
                        }
                        return Completable.complete();
                    })
                    .subscribe();
        } else {
            hideCameraPreview();
        }
    }

    private void showCameraPreview() {
        cameraPreviewDisposable = getCameraPermission()
                .doOnComplete(() -> {
                    binding.cameraPreviewView.setVisibility(View.VISIBLE);
                    binding.scanDocumentHintTextView.setVisibility(View.VISIBLE);
                    binding.cardView.setVisibility(View.VISIBLE);
                    binding.blackBackground.setVisibility(View.VISIBLE);
                    binding.myLucaRecyclerView.setVisibility(View.GONE);
                    binding.bannerScrollView.setVisibility(View.GONE);
                    binding.emptyStateScrollView.setVisibility(View.GONE);
                    binding.primaryActionButton.setText(R.string.action_cancel);
                })
                .andThen(startCameraPreview())
                .doOnError(throwable -> Timber.w("Unable to show camera preview: %s", throwable.toString()))
                .doFinally(this::hideCameraPreview)
                .onErrorComplete()
                .subscribe();

        viewDisposable.add(cameraPreviewDisposable);
    }

    private void hideCameraPreview() {
        if (cameraPreviewDisposable != null) {
            cameraPreviewDisposable.dispose();
            cameraPreviewDisposable = null;
        }
        binding.scanDocumentHintTextView.setVisibility(View.GONE);
        binding.cardView.setVisibility(View.GONE);
        binding.myLucaRecyclerView.setVisibility(View.VISIBLE);
        binding.bannerScrollView.setVisibility(View.VISIBLE);
        binding.blackBackground.setVisibility(View.GONE);
        int emptyStateVisibility = myLucaListAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE;
        binding.emptyStateScrollView.setVisibility(emptyStateVisibility);
        binding.primaryActionButton.setText(R.string.document_import_action);
    }

    public Completable startCameraPreview() {
        return Maybe.fromCallable(() -> cameraProvider)
                .switchIfEmpty(Single.create(emitter -> {
                    ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
                    cameraProviderFuture.addListener(() -> {
                        try {
                            cameraProvider = cameraProviderFuture.get();
                            emitter.onSuccess(cameraProvider);
                        } catch (ExecutionException | InterruptedException e) {
                            emitter.onError(e);
                        }
                    }, ContextCompat.getMainExecutor(getContext()));
                }))
                .flatMapCompletable(cameraProvider -> Completable.create(emitter -> {
                    bindCameraPreview(cameraProvider);
                    emitter.setCancellable(this::unbindCameraPreview);
                }));
    }

    private void bindCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        Preview preview = new Preview.Builder().build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(2048, 2048))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), viewModel);

        preview.setSurfaceProvider(binding.cameraPreviewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle((LifecycleOwner) getContext(), cameraSelector, imageAnalysis, preview);
    }

    private void showDocumentImportConsentDialog(@NonNull Document document) {
        new BaseDialogFragment(new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.document_import_action)
                .setMessage(R.string.document_import_consent)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> viewDisposable
                        .add(viewModel.addDocumentIfBirthDatesMatch(document)
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                        () -> Timber.i("Document added: %s", document),
                                        throwable -> Timber.w("Unable to add document: %s", throwable.toString())
                                )))
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.cancel()))
                .show();
    }

    private void showDeleteDocumentDialog(@NonNull MyLucaListItem myLucaListItem) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext())
                .setTitle(myLucaListItem.getDeleteButtonText())
                .setMessage(R.string.document_delete_confirmation_message)
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                })
                .setPositiveButton(R.string.action_confirm, (dialog, which) ->
                        viewDisposable.add(viewModel.deleteListItem(myLucaListItem)
                                .onErrorComplete()
                                .subscribeOn(Schedulers.io())
                                .subscribe()));
        new BaseDialogFragment(builder).show();
    }

    private void unbindCameraPreview() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
    }

    @Override
    public void onDelete(@NonNull MyLucaListItem myLucaListItem) {
        showDeleteDocumentDialog(myLucaListItem);
    }

}
