package de.culture4life.luca.ui.history

import android.content.Context
import de.culture4life.luca.R
import java.io.Serializable

class MeetingHistoryItem(
    val title: String,
    val subtitle: String,
    val description: String,
    val guestsTitle: String,
    val guests: List<String>,
) : Serializable {
    companion object {

        @JvmStatic
        fun from(context: Context, item: HistoryListItem): MeetingHistoryItem {
            with(context) {
                return MeetingHistoryItem(
                    getString(R.string.meeting_heading),
                    getString(R.string.meeting_information_title),
                    getString(R.string.meeting_description_info),
                    getString(R.string.meeting_details_guests, item.guests.size),
                    item.guests
                )
            }
        }
    }

}