package de.culture4life.luca.ui.accesseddata

import android.content.Context
import de.culture4life.luca.R
import de.culture4life.luca.dataaccess.AccessedTraceData
import de.culture4life.luca.dataaccess.NotificationTexts
import de.culture4life.luca.util.getReadableDate
import de.culture4life.luca.util.getReadableTime
import java.io.Serializable


data class AccessedDataListItem(
    val traceId: String,
    val warningLevel: Int,
    val title: String?,
    val message: String?,
    val detailedMessage: String?,
    val bannerText: String?,
    val checkInTimeRange: String,
    val accessTime: String,
    val isNew: Boolean,
    val accessorName: String?,
    val locationName: String,
) : Serializable {
    companion object {

        @JvmStatic
        fun from(context: Context, item: AccessedTraceData, texts: NotificationTexts): AccessedDataListItem {
            with(context) {
                return AccessedDataListItem(
                    item.traceId,
                    item.warningLevel,
                    texts.title,
                    texts.shortMessage,
                    texts.message,
                    texts.banner,
                    getString(
                        R.string.accessed_data_time,
                        getReadableTime(item.checkInTimestamp),
                        getReadableTime(item.checkOutTimestamp)
                    ),
                    getReadableDate(item.accessTimestamp),
                    item.isNew,
                    item.healthDepartment.name,
                    item.locationName,
                )
            }
        }

    }

}