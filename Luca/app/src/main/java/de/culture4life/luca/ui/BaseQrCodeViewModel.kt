package de.culture4life.luca.ui

import android.annotation.SuppressLint
import android.app.Application
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import de.culture4life.luca.notification.LucaNotificationManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber

abstract class BaseQrCodeViewModel(application: Application) : BaseViewModel(application),
    ImageAnalysis.Analyzer {

    protected var notificationManager: LucaNotificationManager =
        this.application.notificationManager

    private val scanner by lazy { BarcodeScanning.getClient() }

    private var imageProcessingDisposable: Disposable? = null

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (imageProcessingDisposable != null && !imageProcessingDisposable!!.isDisposed) {
            Timber.v("Not processing new camera image, still processing previous one")
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

    protected abstract fun canProcessImage(): Boolean

    protected abstract fun processBarcode(barcodeData: String): Completable

    @SuppressLint("UnsafeOptInUsageError")
    private fun processCameraImage(imageProxy: ImageProxy): Completable {
        return Maybe.fromCallable { imageProxy.image }
            .filter { canProcessImage() }
            .map { image ->
                InputImage.fromMediaImage(image!!, imageProxy.imageInfo.rotationDegrees)
            }
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

    companion object {
        const val BARCODE_DATA_KEY = "barcode_data"
    }

}