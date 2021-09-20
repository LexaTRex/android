package de.culture4life.luca.ui.accesseddata

import android.app.Application
import androidx.lifecycle.MutableLiveData
import de.culture4life.luca.ui.BaseViewModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber

class AccessedDataViewModel(application: Application) : BaseViewModel(application) {

    private val historyManager = this.application.historyManager
    private val dataAccessManager = this.application.dataAccessManager
    val accessedDataItems = MutableLiveData<List<AccessedDataListItem>>()

    override fun initialize(): Completable {
        return super.initialize()
            .andThen(
                Completable.mergeArray(
                    historyManager.initialize(application),
                    dataAccessManager.initialize(application)
                )
            ).andThen(invokeAccessedDataUpdate())
    }

    private fun invokeAccessedDataUpdate(): Completable {
        return Completable.fromAction {
            modelDisposable.add(updateAccessedDataItems()
                .doOnSubscribe { updateAsSideEffect(isLoading, true) }
                .doFinally { updateAsSideEffect(isLoading, false) }
                .subscribeOn(Schedulers.io())
                .subscribe(
                    { Timber.i("Updated accessed data") },
                    { throwable -> Timber.w("Unable to update accessed data: %s", throwable.toString()) }
                ))
        }
    }

    private fun updateAccessedDataItems(): Completable {
        return dataAccessManager.getOrRestoreAccessedData()
            .flattenAsObservable { it.traceData }
            .flatMapSingle { dataAccessManager.createAccessDataListItem(it) }
            .toList()
            .flatMapCompletable { items -> update(accessedDataItems, items) }
    }

}