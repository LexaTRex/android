package de.culture4life.luca.ui.checkin

import android.app.Application
import androidx.lifecycle.MutableLiveData
import de.culture4life.luca.ui.BaseViewModel
import de.culture4life.luca.ui.ViewEvent

class QrCodeBottomSheetViewModel(app: Application) : BaseViewModel(app) {

    val onBottomSheetClosed: MutableLiveData<ViewEvent<Boolean>> = MutableLiveData()
    val onDebuggingCheckInRequested: MutableLiveData<ViewEvent<Boolean>> = MutableLiveData()

    fun onQrCodeBottomSheetClosed() {
        updateAsSideEffect(onBottomSheetClosed, ViewEvent(true))
    }

    fun onDebuggingCheckInRequested() {
        updateAsSideEffect(onDebuggingCheckInRequested, ViewEvent(true))
    }
    
}