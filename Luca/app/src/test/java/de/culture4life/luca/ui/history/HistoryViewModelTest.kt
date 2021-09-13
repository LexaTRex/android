package de.culture4life.luca.ui.history

import androidx.test.runner.AndroidJUnit4
import de.culture4life.luca.LucaUnitTest
import de.culture4life.luca.dataaccess.AccessedTraceData
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [28])
@RunWith(AndroidJUnit4::class)
class HistoryViewModelTest : LucaUnitTest() {

    private val items = listOf(
        HistoryListItem(),
        HistoryListItem().apply {
            accessedTraceData = listOf(
                AccessedTraceData().apply {
                    warningLevel = 1
                }
            )
        }
    )

    @Test
    fun filterHistoryListItems_forWarningLevel_filtersCorrectly() {
        Assert.assertEquals(1, HistoryViewModel.filterHistoryListItems(items, 1).size)
        Assert.assertEquals(0, HistoryViewModel.filterHistoryListItems(items, 3).size)
        Assert.assertEquals(2, HistoryViewModel.filterHistoryListItems(items, -1).size)
    }

}
