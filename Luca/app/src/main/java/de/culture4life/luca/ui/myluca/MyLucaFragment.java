package de.culture4life.luca.ui.myluca;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;

import android.util.Size;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import de.culture4life.luca.R;
import de.culture4life.luca.document.Document;
import de.culture4life.luca.ui.BaseFragment;
import de.culture4life.luca.ui.dialog.BaseDialogFragment;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class MyLucaFragment extends BaseFragment<MyLucaViewModel> implements MyLucaListAdapter.MyLucaListClickListener {

    private MaterialCardView qrCodeCardView;
    private PreviewView cameraPreviewView;
    private View scanDocumentHintTextView;
    private View loadingView;
    private ImageView bookAppointmentImageView;
    private TextView emptyTitleTextView;
    private TextView emptyDescriptionTextView;
    private ImageView emptyImageView;
    private RecyclerView myLucaRecyclerView;
    private MyLucaListAdapter myLucaListAdapter;
    private MaterialButton importTestButton;

    private ProcessCameraProvider cameraProvider;

    private Disposable cameraPreviewDisposable;

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_my_luca;
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
                viewModel.invokeListUpdate()
        ).subscribe());
    }

    @Override
    protected Completable initializeViews() {
        return super.initializeViews()
                .andThen(Completable.fromAction(() -> {
                    initializeMyLucaItemsViews();
                    initializeEmptyStateViews();
                    initializeImportViews();
                }));
    }

    private void initializeMyLucaItemsViews() {
        TextView headingTextView = getView().findViewById(R.id.headingTextView);
        observe(viewModel.getUserName(), headingTextView::setText);

        myLucaRecyclerView = getView().findViewById(R.id.myLucaRecyclerView);
        myLucaListAdapter = new MyLucaListAdapter(this);
        myLucaRecyclerView.setAdapter(myLucaListAdapter);
        myLucaRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        observe(viewModel.getMyLucaItems(), items -> myLucaListAdapter.setItems(items));
    }

    private void initializeEmptyStateViews() {
        emptyTitleTextView = getView().findViewById(R.id.emptyTitleTextView);
        emptyDescriptionTextView = getView().findViewById(R.id.emptyDescriptionTextView);
        emptyImageView = getView().findViewById(R.id.emptyImageView);

        observe(viewModel.getMyLucaItems(), items -> {
            int emptyStateVisibility = items.isEmpty() ? View.VISIBLE : View.GONE;
            int contentVisibility = !items.isEmpty() ? View.VISIBLE : View.GONE;
            emptyTitleTextView.setVisibility(emptyStateVisibility);
            emptyDescriptionTextView.setVisibility(emptyStateVisibility);
            emptyImageView.setVisibility(emptyStateVisibility);
            myLucaRecyclerView.setVisibility(contentVisibility);
        });
    }

    private void initializeImportViews() {
        qrCodeCardView = getView().findViewById(R.id.cardView);
        qrCodeCardView.setVisibility(View.GONE);

        bookAppointmentImageView = getView().findViewById(R.id.bookAppointmentImageView);
        bookAppointmentImageView.setOnClickListener(v -> viewModel.onAppointmentRequested());

        importTestButton = getView().findViewById(R.id.primaryActionButton);
        importTestButton.setOnClickListener(v -> toggleCameraPreview());

        cameraPreviewView = getView().findViewById(R.id.cameraPreviewView);
        scanDocumentHintTextView = getView().findViewById(R.id.scanDocumentHintTextView);
        loadingView = getView().findViewById(R.id.loadingLayout);
        observe(viewModel.getIsLoading(), loading -> loadingView.setVisibility(loading ? View.VISIBLE : View.GONE));

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
                    cameraPreviewView.setVisibility(View.VISIBLE);
                    scanDocumentHintTextView.setVisibility(View.VISIBLE);
                    qrCodeCardView.setVisibility(View.VISIBLE);
                    importTestButton.setText(R.string.action_cancel);
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
        scanDocumentHintTextView.setVisibility(View.GONE);
        qrCodeCardView.setVisibility(View.GONE);
        myLucaRecyclerView.setVisibility(View.VISIBLE);
        importTestButton.setText(R.string.document_import_action);
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

        preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) getContext(), cameraSelector, imageAnalysis, preview);
    }

    private void showDocumentImportConsentDialog(@NonNull Document document) {
        new BaseDialogFragment(new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.document_import_action)
                .setMessage(R.string.document_import_consent)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                    viewDisposable.add(viewModel.addDocument(document)
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    () -> Timber.i("Document added: %s", document),
                                    throwable -> Timber.w("Unable to add document: %s", throwable.toString())
                            ));
                })
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
