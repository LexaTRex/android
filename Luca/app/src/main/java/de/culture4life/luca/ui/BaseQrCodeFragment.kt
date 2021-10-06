package de.culture4life.luca.ui

import android.Manifest
import android.util.Size
import androidx.annotation.CallSuper
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.culture4life.luca.R
import de.culture4life.luca.ui.dialog.BaseDialogFragment
import de.culture4life.luca.util.addTo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class BaseQrCodeFragment<ViewModelType : BaseQrCodeViewModel?> : BaseFragment<ViewModelType>() {

    protected var cameraProvider: ProcessCameraProvider? = null
    protected var camera: Camera? = null
    protected var cameraPreviewView: PreviewView? = null
    protected var cameraPreviewDisposable: Disposable? = null

    @CallSuper
    override fun initializeViews(): Completable {
        return super.initializeViews()
            .andThen(initializeCameraPreview())
    }

    @CallSuper
    protected open fun initializeCameraPreview(): Completable {
        return Completable.fromAction {
            observe(viewModel!!.shouldShowCameraPreview()) {
                if (it.showCamera) {
                    val requestMissingStuff = !it.onlyIfPossible
                    showCameraPreview(requestMissingStuff, requestMissingStuff)
                } else {
                    hideCameraPreview()
                }
            }
        }
    }

    /**
     * Will attempt to start the camera preview after checking that the prominent disclosure consent
     * has been given and the camera permission has been granted. Will request missing consent or
     * permission if enabled by the respective parameter or do nothing at all if not.
     */
    protected fun showCameraPreview(requestConsent: Boolean = true, requestPermission: Boolean = true) {
        if (cameraPreviewDisposable != null) {
            Timber.d("Not starting camera preview, already started")
            return
        }
        val requestConsentIfRequired = viewModel!!.getCameraConsentGiven()
            .flatMapCompletable { isConsentGiven: Boolean ->
                if (!isConsentGiven) {
                    if (requestConsent) {
                        showCameraConsentDialog(false)
                        viewModel!!.onCameraConsentRequired()
                    }
                    return@flatMapCompletable Completable.error(IllegalStateException("Camera consent not given"))
                }
                return@flatMapCompletable Completable.complete()
            }

        val requestPermissionIfRequired = Single.fromCallable { isCameraPermissionGranted() }
            .flatMapCompletable { isPermissionGranted: Boolean ->
                if (!isPermissionGranted) {
                    if (requestPermission) {
                        viewModel!!.onCameraPermissionRequired()
                        return@flatMapCompletable requestCameraPermission()
                    }
                    return@flatMapCompletable Completable.error(IllegalStateException("Camera permission not granted"))
                }
                return@flatMapCompletable Completable.complete();
            }

        requestConsentIfRequired
            .andThen(requestPermissionIfRequired)
            .delay(50, TimeUnit.MILLISECONDS) // required because of a weird race condition with the camera preview view
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete { setCameraPreviewVisible(true) }
            .andThen(startCameraPreview())
            .doOnError { Timber.w(it, "Unable to start camera preview: %s", it.toString()) }
            .onErrorComplete()
            .doFinally {
                setCameraPreviewVisible(false)
                cameraPreviewDisposable = null
            }
            .doOnSubscribe {
                Timber.d("Attempting to start camera preview")
                cameraPreviewDisposable = it
            }
            .subscribe()
            .addTo(viewDisposable)
    }

    protected fun hideCameraPreview() {
        Timber.d("Hiding camera preview")
        cameraPreviewDisposable?.dispose()
        cameraPreviewDisposable = null
        unbindCameraPreview()
        setCameraPreviewVisible(false)
    }

    @CallSuper
    protected open fun setCameraPreviewVisible(isVisible: Boolean) {
        cameraPreviewView?.isVisible = isVisible
    }

    /**
     * Will attempt to get a camera provider and bind it to the lifecycle.
     * Will not complete. Will unbind the camera on disposal.
     *
     * Should only be used after the camera consent and permission have been given.
     */
    protected fun startCameraPreview(): Completable {
        return Maybe.fromCallable { cameraProvider }
            .switchIfEmpty(Single.create { emitter ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(
                    requireContext()
                )
                cameraProviderFuture.addListener({
                    try {
                        cameraProvider = cameraProviderFuture.get()
                        emitter.onSuccess(cameraProvider!!)
                    } catch (e: Exception) {
                        emitter.onError(e)
                    }
                }, ContextCompat.getMainExecutor(context))
            })
            .flatMapCompletable { cameraProvider ->
                Completable.create { emitter ->
                    cameraProvider?.let { bindCameraPreview(it) }
                    emitter.setCancellable { unbindCameraPreview() }
                }
            }
    }

    @CallSuper
    protected open fun bindCameraPreview(cameraProvider: ProcessCameraProvider) {
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        val preview = Preview.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(2048, 2048))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), viewModel!!)
        preview.setSurfaceProvider(cameraPreviewView!!.surfaceProvider)
        camera = cameraProvider.bindToLifecycle(
            requireContext() as LifecycleOwner,
            cameraSelector,
            imageAnalysis,
            preview
        )
    }

    @CallSuper
    protected open fun unbindCameraPreview() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        setTorchEnabled(false)
    }

    protected fun toggleTorch() {
        val torchIsEnabled = camera?.cameraInfo?.torchState?.value == TorchState.ON
        setTorchEnabled(!torchIsEnabled)
    }

    @CallSuper
    open fun setTorchEnabled(isEnabled: Boolean) {
        camera?.cameraControl?.enableTorch(isEnabled)
    }

    protected fun isCameraPermissionGranted(): Boolean {
        return rxPermissions.isGranted(Manifest.permission.CAMERA)
    }

    private fun requestCameraPermission(): Completable {
        return rxPermissions.request(Manifest.permission.CAMERA)
            .flatMapCompletable { granted: Boolean ->
                if (granted) {
                    viewModel!!.onCameraPermissionGiven()
                    return@flatMapCompletable Completable.complete()
                } else {
                    viewModel!!.onCameraPermissionDenied()
                    showCameraPermissionPermanentlyDeniedError()
                    return@flatMapCompletable Completable.error(IllegalStateException("Camera permission missing"))
                }
            }
    }

    private fun showCameraConsentDialog(directToSettings: Boolean) {
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.check_in_enable_camera_access_title)
            .setMessage(R.string.check_in_enable_camera_access_description)
            .setNegativeButton(R.string.action_cancel) { _, _ ->
                viewModel!!.onCameraConsentDenied()
            }
        if (directToSettings) {
            builder.setPositiveButton(R.string.action_settings) { _, _ ->
                application.openAppSettings()
            }
        } else {
            builder.setPositiveButton(R.string.action_enable) { _, _ ->
                viewModel!!.onCameraConsentGiven()
            }
        }
        BaseDialogFragment(builder).show()
    }

    protected fun showCameraPermissionPermanentlyDeniedError() {
        showErrorAsSnackbar(
            ViewError.Builder(requireContext())
                .withTitle(getString(R.string.missing_permission_arg, getString(R.string.permission_name_camera)))
                .withDescription(getString(R.string.missing_permission_arg, getString(R.string.permission_name_camera)))
                .withResolveLabel(getString(R.string.action_resolve))
                .withResolveAction(Completable.fromAction { showCameraConsentDialog(true) })
                .build()
        )
    }

}