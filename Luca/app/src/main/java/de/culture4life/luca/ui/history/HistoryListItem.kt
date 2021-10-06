package de.culture4life.luca.ui.history

import androidx.annotation.DrawableRes
import de.culture4life.luca.dataaccess.AccessedTraceData

data class HistoryListItem(
    var timestamp: Long = 0,
    var time: String? = null,
    var title: String? = null,
    var relatedId: String? = null,
    var description: String? = null,
    var additionalTitleDetails: String? = null,
    @DrawableRes var titleIconResourceId: Int = 0,
    @DrawableRes var descriptionIconResourceId: Int = 0,
    var accessedTraceData: List<AccessedTraceData> = listOf(),
    var isPrivateMeeting: Boolean = false,
    var guests: List<String> = listOf()
) {

    fun containsWarningLevel(warningLevel: Int): Boolean {
        return accessedTraceData.any {
            it.warningLevel == warningLevel
        }
    }

}
