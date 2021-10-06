package de.culture4life.luca.dataaccess

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import de.culture4life.luca.LucaApplication
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

class DataAccessUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : RxWorker(appContext, workerParams) {

    override fun createWork(): Single<Result> {
        return Completable.defer {
            val application = applicationContext as LucaApplication
            val dataAccessManager = application.dataAccessManager
            dataAccessManager.initialize(application)
                .andThen(dataAccessManager.updateIfNecessary())
                .subscribeOn(Schedulers.io())
        }.andThen(Single.just(Result.success()))
            .onErrorReturnItem(Result.failure())
    }

}