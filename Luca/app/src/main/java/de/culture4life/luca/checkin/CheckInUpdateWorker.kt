package de.culture4life.luca.checkin

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import de.culture4life.luca.LucaApplication
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

class CheckInUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : RxWorker(appContext, workerParams) {

    override fun createWork(): Single<Result> {
        return Completable.defer {
            val application = applicationContext as LucaApplication
            val checkInManager = application.checkInManager
            checkInManager.initialize(application)
                .andThen(checkInManager.hasRecentTraceIds(false))
                .flatMapCompletable { hasYoungRecentTraceIds: Boolean ->
                    if (hasYoungRecentTraceIds) {
                        // only perform background update if foreground updates are not required
                        Completable.error(IllegalStateException("Foreground updates are required"))
                    } else {
                        checkInManager.updateCheckInDataIfNecessary(true)
                            .andThen(checkInManager.isCheckedIn
                                .flatMapCompletable { isCheckedIn: Boolean ->
                                    if (isCheckedIn) {
                                        Completable.complete()
                                    } else {
                                        checkInManager.deleteRecentTraceIds()
                                    }
                                })
                    }
                }
                .subscribeOn(Schedulers.io())
        }.andThen(Single.just(Result.success()))
            .onErrorReturnItem(Result.failure())
    }

}