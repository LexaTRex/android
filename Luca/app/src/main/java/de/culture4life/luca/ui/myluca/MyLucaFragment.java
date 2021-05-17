package de.culture4life.luca.ui.myluca;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;

import android.util.Size;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import de.culture4life.luca.R;
import de.culture4life.luca.testing.TestResult;
import de.culture4life.luca.ui.BaseFragment;
import de.culture4life.luca.ui.UiUtil;
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
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class MyLucaFragment extends BaseFragment<MyLucaViewModel> implements MyLucaListAdapter.MyLucaListClickListener {

    private MaterialCardView qrCodeCardView;
    private PreviewView cameraPreviewView;
    private View hintTextScanTestResult;
    private View loadingView;
    private ImageView importTestImageView;
    private TextView emptyTitleTextView;
    private TextView emptyDescriptionTextView;
    private ImageView emptyImageView;
    private ListView myLucaListView;
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
        viewDisposable.add(viewModel.invokeUpdate().subscribe());
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

        myLucaListView = getView().findViewById(R.id.myLucaListView);
        View paddingView = new View(getContext());
        paddingView.setMinimumHeight((int) UiUtil.convertDpToPixel(16, getContext()));
        myLucaListView.addHeaderView(paddingView);

        myLucaListAdapter = new MyLucaListAdapter(getContext(), myLucaListView.getId(), this);
        myLucaListView.setAdapter(myLucaListAdapter);

        observe(viewModel.getMyLucaItems(), items -> myLucaListAdapter.setHistoryItems(items));
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
            myLucaListView.setVisibility(contentVisibility);
        });
    }

    private void initializeImportViews() {
        qrCodeCardView = getView().findViewById(R.id.cardView);
        qrCodeCardView.setVisibility(View.GONE);

        importTestImageView = getView().findViewById(R.id.importTestImageView);
        importTestImageView.setOnClickListener(v -> toggleCameraPreview());

        importTestButton = getView().findViewById(R.id.primaryActionButton);
        importTestButton.setOnClickListener(v -> toggleCameraPreview());

        cameraPreviewView = getView().findViewById(R.id.cameraPreviewView);
        hintTextScanTestResult = getView().findViewById(R.id.hintTextScanTestResult);
        loadingView = getView().findViewById(R.id.loadingLayout);
        observe(viewModel.getIsLoading(), loading -> loadingView.setVisibility(loading ? View.VISIBLE : View.GONE));

        observe(viewModel.getImportedTestResult(), testResultViewEvent -> {
            if (!testResultViewEvent.hasBeenHandled()) {
                TestResult testResult = testResultViewEvent.getValueAndMarkAsHandled();
                Toast.makeText(getContext(), R.string.test_import_success_message, Toast.LENGTH_SHORT).show();
                hideCameraPreview();
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
            showTestImportConsentDialog();
        } else {
            hideCameraPreview();
        }
    }

    private void showCameraPreview() {
        cameraPreviewDisposable = getCameraPermission()
                .doOnComplete(() -> {
                    cameraPreviewView.setVisibility(View.VISIBLE);
                    hintTextScanTestResult.setVisibility(View.VISIBLE);
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
        hintTextScanTestResult.setVisibility(View.GONE);
        qrCodeCardView.setVisibility(View.GONE);
        myLucaListView.setVisibility(View.VISIBLE);
        importTestButton.setText(R.string.test_import_action);
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

    private void showTestImportConsentDialog() {
        new BaseDialogFragment(new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.test_import_action)
                .setMessage(R.string.test_import_consent)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> showCameraDialog(false))
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.cancel()))
                .show();
    }

    private void showDeleteTestResultDialog(int position) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.test_delete_alert_dialog_title)
                .setMessage(R.string.test_delete_alert_dialog_message)
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                })
                .setPositiveButton(R.string.action_confirm, (dialog, which) -> {
                    viewDisposable.add(viewModel.deleteTestResult(position)
                            .subscribeOn(Schedulers.io())
                            .subscribe());
                });
        new BaseDialogFragment(builder).show();
    }

    private void unbindCameraPreview() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
    }

    @Override
    public void onDelete(int position) {
        showDeleteTestResultDialog(position);
    }

}
