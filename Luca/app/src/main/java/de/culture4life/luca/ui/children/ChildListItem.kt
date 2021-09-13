package de.culture4life.luca.ui.children

import de.culture4life.luca.children.Child

data class ChildListItem(
    val child: Child,
    var isCheckedIn: Boolean
) {

    fun toggleIsChecked() {
        isCheckedIn = !isCheckedIn
    }

}
