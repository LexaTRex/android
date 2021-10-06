package de.culture4life.luca.ui.accesseddata

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import de.culture4life.luca.ui.BaseViewModel
import de.culture4life.luca.ui.accesseddata.AccessedDataDetailFragment.Companion.KEY_ACCESSED_DATA_LIST_ITEM
import de.culture4life.luca.util.addTo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber

class AccessedDataDetailViewModel(application: Application) : BaseViewModel(application) {

    private val dataAccessManager = this.application.dataAccessManager

    val accessedDataItem = MutableLiveData<AccessedDataListItem>()

    override fun initialize(): Completable {
        return super.initialize()
            .andThen(dataAccessManager.initialize(application))
    }

    fun onItemSeen(item: AccessedDataListItem) {
        dataAccessManager.markAsNotNew(item.traceId, item.warningLevel)
            .subscribeOn(Schedulers.io())
            .subscribe(
                { Timber.d("Item marked as not new: %s", item) },
                { Timber.w("Unable to mark item as not new: %s", it.toString()) }
            ).addTo(modelDisposable)
    }

    override fun processArguments(arguments: Bundle?): Completable {
        return super.processArguments(arguments)
            .andThen(Maybe.fromCallable { arguments?.getSerializable(KEY_ACCESSED_DATA_LIST_ITEM) as AccessedDataListItem? })
            .flatMapCompletable { updateIfRequired(accessedDataItem, it) }
    }

}