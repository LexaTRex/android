package de.culture4life.luca.ui.account

import android.app.Application
import de.culture4life.luca.R
import de.culture4life.luca.ui.BaseViewModel

class AccountViewModel(application: Application) : BaseViewModel(application) {
    fun openHealthDepartmentKeyView() {
        navigationController.navigate(R.id.action_accountFragment_to_healthDepartmentKeyFragment)
    }
}