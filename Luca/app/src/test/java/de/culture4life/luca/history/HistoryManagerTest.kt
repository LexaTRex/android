package de.culture4life.luca.history

import androidx.test.runner.AndroidJUnit4
import de.culture4life.luca.LucaUnitTest
import de.culture4life.luca.checkin.CheckInData
import de.culture4life.luca.children.ChildrenManager
import de.culture4life.luca.crypto.CryptoManager
import de.culture4life.luca.network.NetworkManager
import de.culture4life.luca.preference.PreferencesManager
import de.culture4life.luca.registration.RegistrationManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.*
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class HistoryManagerTest : LucaUnitTest() {
    private val preferencesManager = PreferencesManager()
    private val networkManager = NetworkManager()
    private val cryptoManager = CryptoManager(preferencesManager, networkManager)
    private val registrationManager = RegistrationManager(preferencesManager, networkManager, cryptoManager)
    private val childrenManager = ChildrenManager(preferencesManager, registrationManager)
    private val historyManager by lazy { HistoryManager(preferencesManager, childrenManager).apply {
        initialize(application).blockingAwait()
    }}
    private val checkInData = CheckInData().apply {
        traceId = "asdf"
        locationId = UUID.randomUUID()
    }

    @Test
    fun items_initially_hasNoValues() {
        historyManager.items.test().assertNoValues()
    }

    @Test
    fun items_afterAddingCheckInItem_hasValue() {
        historyManager.addCheckInItem(checkInData)
            .andThen(historyManager.items)
            .test().assertValueCount(1)
    }

    @Test
    fun items_afterClearItems_hasNoValues() {
        historyManager.addCheckInItem(checkInData)
            .andThen(historyManager.clearItems())
            .andThen(historyManager.items)
            .map { historyItem -> historyItem.type }
            .test()
            .assertValue(HistoryItem.TYPE_DATA_DELETED)
            .assertValueCount(1)
    }

    @Test
    fun deleteOldItems_hasOldItems_removesOldItems() {
        checkInData.timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(29)
        historyManager.addCheckInItem(checkInData)
            .andThen(historyManager.deleteOldItems())
            .andThen(historyManager.items)
            .test()
            .assertNoValues()
    }

    @Test
    fun deleteOldItems_noOldItems_keepsCurrentItems() {
        checkInData.timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(23)
        historyManager.addCheckInItem(checkInData)
            .andThen(historyManager.deleteOldItems())
            .andThen(historyManager.items)
            .test().assertValueCount(1)
    }

    @Test
    fun createOrderedList_withItems_isCorrect() {
        val listString = HistoryManager.createOrderedList(listOf("A", "B"))
        assertEquals("1\t\t\tA\n2\t\t\tB", listString)
    }

    @Test
    fun createUnorderedList_withItems_isCorrect() {
        val listString = HistoryManager.createUnorderedList(listOf("A", "B"))
        assertEquals("- A\n- B", listString)
    }
}