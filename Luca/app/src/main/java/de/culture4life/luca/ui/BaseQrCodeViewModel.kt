package de.culture4life.luca.ui

import android.annotation.SuppressLint
import android.app.Application
import androidx.annotation.CallSuper
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import de.culture4life.luca.notification.LucaNotificationManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber

abstract class BaseQrCodeViewModel(application: Application) :
    BaseViewModel(application), ImageAnalysis.Analyzer {

    protected var notificationManager: LucaNotificationManager = this.application.notificationManager

    private val scanner by lazy { BarcodeScanning.getClient() }
    private var imageProcessingDisposable: Disposable? = null
    protected val showCameraPreview = MutableLiveData(CameraRequest(false))
    var pauseCameraImageProcessing = false

    /*
        Camera consent
     */

    open fun getCameraConsentGiven(): Single<Boolean> {
        return preferencesManager.restoreOrDefault(KEY_CAMERA_CONSENT_GIVEN, false)
    }

    private fun setCameraConsentGiven(cameraConsentGiven: Boolean) {
        preferencesManager.persist(KEY_CAMERA_CONSENT_GIVEN, cameraConsentGiven)
            .andThen(update(showCameraPreview, CameraRequest(cameraConsentGiven, false)))
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    open fun onCameraConsentRequired() {

    }

    open fun onCameraConsentGiven() {
        setCameraConsentGiven(true)
    }

    open fun onCameraConsentDenied() {
        setCameraConsentGiven(false)
    }

    /*
        Camera permission
     */

    open fun onCameraPermissionRequired() {

    }

    open fun onCameraPermissionGiven() {
        updateAsSideEffect(showCameraPreview, CameraRequest(true))
    }

    open fun onCameraPermissionDenied() {
        updateAsSideEffect(showCameraPreview, CameraRequest(false))
    }

    /*
        Camera image processing
     */

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (!shouldProcessCameraImages()) {
            imageProxy.close()
            return
        }
        imageProcessingDisposable = processCameraImage(imageProxy)
            .doOnError { Timber.w("Unable to process camera image: %s", it.toString()) }
            .onErrorComplete()
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { imageProxy.close() }
            .subscribeOn(Schedulers.computation())
            .subscribe()
        modelDisposable.add(imageProcessingDisposable)
    }

    /**
     * Indicates if new camera images should be processed. Should return false if an image
     * is currently processed or any UI is overlaying the camera preview.
     */
    @CallSuper
    protected open fun shouldProcessCameraImages(): Boolean {
        if (imageProcessingDisposable != null && !imageProcessingDisposable!!.isDisposed) {
            return false // still processing last image
        } else if (isLoading.value == true) {
            return false // something is still loading, e.g. a check-in request
        } else if (errors.value?.isNotEmpty() == true) {
            return false // an error is currently visible
        } else if (pauseCameraImageProcessing) {
            return false // processing paused
        }
        return true
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processCameraImage(imageProxy: ImageProxy): Completable {
        return Maybe.fromCallable { imageProxy.image }
            .map { image -> InputImage.fromMediaImage(image!!, imageProxy.imageInfo.rotationDegrees) }
            .flatMapObservable { image -> detectBarcodes(image) }
            .flatMapMaybe { barcode -> Maybe.fromCallable { barcode.rawValue } }
            .flatMapCompletable { barcodeData -> processBarcode(barcodeData) }
    }

    private fun detectBarcodes(image: InputImage): Observable<Barcode> {
        return Observable.create { emitter ->
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        emitter.onNext(barcode)
                    }
                    emitter.onComplete()
                }
                .addOnFailureListener { emitter.tryOnError(it) }
        }
    }

    protected abstract fun processBarcode(barcodeData: String): Completable

    open fun shouldShowCameraPreview(): LiveData<CameraRequest> {
        return showCameraPreview
    }

    data class CameraRequest(
        val showCamera: Boolean,
        val onlyIfPossible: Boolean = false
    )

    companion object {
        const val BARCODE_DATA_KEY = "barcode_data"
    }

}