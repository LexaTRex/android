package de.culture4life.luca.ui.history

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import de.culture4life.luca.ui.BaseViewModel
import de.culture4life.luca.ui.history.MeetingHistoryDetailFragment.Companion.KEY_PRIVATE_MEETING_ITEM
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe

class MeetingHistoryDetailViewModel(application: Application) : BaseViewModel(application) {

    val privateMeetingItem = MutableLiveData<MeetingHistoryItem>()

    override fun processArguments(arguments: Bundle?): Completable {
        return super.processArguments(arguments)
            .andThen(Maybe.fromCallable { arguments?.getSerializable(KEY_PRIVATE_MEETING_ITEM) as MeetingHistoryItem? })
            .flatMapCompletable { updateIfRequired(privateMeetingItem, it) }
    }

}