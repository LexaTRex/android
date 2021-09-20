package de.culture4life.luca.dataaccess

import androidx.test.runner.AndroidJUnit4
import de.culture4life.luca.LucaUnitTest
import de.culture4life.luca.checkin.CheckInManager
import de.culture4life.luca.children.ChildrenManager
import de.culture4life.luca.crypto.CryptoManager
import de.culture4life.luca.history.HistoryManager
import de.culture4life.luca.location.GeofenceManager
import de.culture4life.luca.location.LocationManager
import de.culture4life.luca.network.NetworkManager
import de.culture4life.luca.network.pojo.NotifyingHealthDepartment
import de.culture4life.luca.notification.LucaNotificationManager
import de.culture4life.luca.preference.PreferencesManager
import de.culture4life.luca.registration.RegistrationManager
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.annotation.Config
import java.util.*
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class DataAccessManagerTest : LucaUnitTest() {
    private val preferencesManager = PreferencesManager()
    private val networkManager = NetworkManager()
    private val cryptoManager = CryptoManager(preferencesManager, networkManager)
    private val registrationManager = RegistrationManager(preferencesManager, networkManager, cryptoManager)
    private val childrenManager = ChildrenManager(preferencesManager, registrationManager)
    private val historyManager = HistoryManager(preferencesManager, childrenManager)
    private val notificationManager = Mockito.spy(LucaNotificationManager())
    private val checkInManager by lazy {
        Mockito.spy(
            CheckInManager(
                preferencesManager,
                networkManager,
                GeofenceManager(),
                LocationManager(),
                historyManager,
                cryptoManager,
                notificationManager
            )
        )
    }
    private val dataAccessManager by lazy {
        Mockito.spy(
            DataAccessManager(preferencesManager, networkManager, notificationManager, checkInManager, historyManager, cryptoManager)
        ).apply {
            initialize(application).blockingAwait()
        }
    }
    private val previouslyAccessedTraceData = AccessedTraceData().apply {
        isNew = true
        traceId = "qiqA2+SpnoioxRMWb7IDsw=="
        hashedTraceId = "HASH_VALUE_FOR_WARNING_LEVEL_1"
        warningLevel = 1
    }

    @Test
    fun update_successful_updatesLastUpdateTimestamp() {
        Mockito.`when`(dataAccessManager.fetchNotificationConfig()).thenReturn(Single.error(RuntimeException()))
        Mockito.`when`(dataAccessManager.fetchNewRecentlyAccessedTraceData()).thenReturn(Observable.empty())
        val previousDuration = dataAccessManager.durationSinceLastUpdate.blockingGet()
        dataAccessManager.update()
            .andThen(dataAccessManager.durationSinceLastUpdate)
            .test()
            .assertValue { it < previousDuration }
    }

    @Test
    fun update_unsuccessful_doesNotUpdateLastUpdateTimestamp() {
        Mockito.`when`(dataAccessManager.fetchNewRecentlyAccessedTraceData()).thenReturn(Observable.error(RuntimeException()))
        val previousDuration = dataAccessManager.durationSinceLastUpdate.blockingGet()
        val duration = dataAccessManager.update().onErrorComplete()
            .andThen(dataAccessManager.durationSinceLastUpdate)
            .blockingGet()
        Assert.assertEquals(duration.toFloat(), previousDuration.toFloat(), 20f)
    }

    @Test
    fun durationSinceLastUpdate_justUpdated_emitsLowDuration() {
        Mockito.`when`(dataAccessManager.fetchNotificationConfig()).thenReturn(Single.error(RuntimeException()))
        Mockito.`when`(dataAccessManager.fetchNewRecentlyAccessedTraceData()).thenReturn(Observable.empty())
        dataAccessManager.update()
            .andThen(dataAccessManager.durationSinceLastUpdate)
            .test()
            .assertValue { it < 1000 }
    }

    @Test
    fun durationSinceLastUpdate_neverUpdated_emitsHighDuration() {
        dataAccessManager.durationSinceLastUpdate
            .test()
            .assertValue { it > System.currentTimeMillis() - 1000 }
    }

    @Test
    fun nextRecommendedUpdateDelay_justUpdated_emitsUpdateInterval() {
        Mockito.`when`(dataAccessManager.durationSinceLastUpdate).thenReturn(Single.just(0L))
        dataAccessManager.nextRecommendedUpdateDelay
            .test()
            .assertValue(DataAccessManager.UPDATE_INTERVAL)
    }

    @Test
    fun nextRecommendedUpdateDelay_neverUpdated_emitsLowDelay() {
        Mockito.`when`(dataAccessManager.durationSinceLastUpdate).thenReturn(Single.just(System.currentTimeMillis()))
        dataAccessManager.nextRecommendedUpdateDelay
            .test()
            .assertValue(0L)
    }

    @Test
    @Throws(InterruptedException::class)
    fun processNewRecentlyAccessedTraceData_dataAvailable_performsProcessing() {
        val newAccessedTraceData = AccessedTraceData().apply {
            hashedTraceId = "LLJMzA/HqlS77qkpUGNJrA=="
            healthDepartment = createDummyHealthDepartment()
        }
        dataAccessManager.processNewRecentlyAccessedTraceData(listOf(newAccessedTraceData))
            .test()
            .await()
            .assertComplete()
        Mockito.verify(dataAccessManager, Mockito.times(1))
            .addToAccessedData(ArgumentMatchers.any())
        Mockito.verify(dataAccessManager, Mockito.times(1)).addHistoryItems(ArgumentMatchers.any())
        Mockito.verify(dataAccessManager, Mockito.times(1))
            .notifyUserAboutDataAccess(ArgumentMatchers.any())
    }

    @Test
    @Throws(InterruptedException::class)
    fun processNewRecentlyAccessedTraceData_noDataAvailable_performsNothing() {
        Mockito.`when`(dataAccessManager.fetchNotificationConfig()).thenReturn(Single.error(RuntimeException()))
        dataAccessManager.processNewRecentlyAccessedTraceData(emptyList())
            .test()
            .await()
            .assertComplete()
        Mockito.verify(dataAccessManager, Mockito.never()).addToAccessedData(ArgumentMatchers.any())
        Mockito.verify(dataAccessManager, Mockito.never()).addHistoryItems(ArgumentMatchers.any())
        Mockito.verify(dataAccessManager, Mockito.never())
            .notifyUserAboutDataAccess(ArgumentMatchers.any())
    }

    @Test
    fun notifyUserAboutDataAccess_validData_showsNotification() {
        Mockito.`when`(dataAccessManager.fetchNotificationConfig()).thenReturn(Single.error(RuntimeException()))
        val newAccessedTraceData = AccessedTraceData().apply {
            hashedTraceId = "LLJMzA/HqlS77qkpUGNJrA=="
        }
        dataAccessManager.notifyUserAboutDataAccess(listOf(newAccessedTraceData))
            .test()
            .assertComplete()
        Mockito.verify(notificationManager, Mockito.times(1))
            .showNotification(
                ArgumentMatchers.eq(LucaNotificationManager.NOTIFICATION_ID_DATA_ACCESS),
                ArgumentMatchers.any()
            )
    }

    @Test
    fun recentTraceIds_checkInsAvailable_emitsTraceIdsFromCheckIns() {
        val traceId = "9bZZ5Ak465V60PXv92aMFA=="
        Mockito.doReturn(Observable.just(traceId))
            .`when`(checkInManager)
            .archivedTraceIds
        dataAccessManager.recentTraceIds
            .test()
            .assertValues(traceId)
            .assertComplete()
    }

    @Test
    fun fetchRecentlyAccessedTraceData_noRecentTraceIds_completesEmpty() {
        Mockito.`when`(dataAccessManager.fetchHealthDepartments()).thenReturn(Observable.just(createDummyHealthDepartment()))
        Mockito.`when`(dataAccessManager.fetchUnprocessedChunks()).thenReturn(Observable.just(createDummyChunk()))
        Mockito.doReturn(Observable.empty<Any>()).`when`(dataAccessManager).recentTraceIds
        dataAccessManager.fetchRecentlyAccessedTraceData()
            .test()
            .assertNoValues()
            .assertComplete()
    }

    @Test
    fun fetchRecentlyAccessedTraceData_noRecentAccessedHashedTraceIds_completesEmpty() {
        Mockito.`when`(dataAccessManager.fetchUnprocessedChunks()).thenReturn(Observable.empty())
        Mockito.`when`(dataAccessManager.recentTraceIds).thenReturn(Observable.just("hCvt6FNlhomxbBmL50PYDw=="))
        dataAccessManager.fetchRecentlyAccessedTraceData()
            .test()
            .assertNoValues()
            .assertComplete()
    }

    @Test
    fun fetchRecentlyAccessedTraceData_noDataAccessed_completesEmpty() {
        val healthDepartment = createDummyHealthDepartment()
        Mockito.`when`(dataAccessManager.fetchHealthDepartments()).thenReturn(Observable.just(healthDepartment))
        val chunk = createDummyChunk()
        chunk.hashedTraceIds.add(
            dataAccessManager.getHashedTraceId(
                healthDepartment.id, 1, "99FmQcylJT5e/cyHOjT6Hw==", chunk.hashLength
            ).blockingGet()
        )
        Mockito.`when`(dataAccessManager.fetchUnprocessedChunks()).thenReturn(Observable.just(chunk))
        Mockito.`when`(dataAccessManager.recentTraceIds).thenReturn(Observable.just("hCvt6FNlhomxbBmL50PYDw=="))
        dataAccessManager.fetchRecentlyAccessedTraceData()
            .test()
            .assertNoValues()
            .assertComplete()
    }

    @Test
    fun fetchRecentlyAccessedTraceData_someDataAccessed_emitsAccessedData() {
        val healthDepartment = createDummyHealthDepartment()
        Mockito.`when`(dataAccessManager.fetchHealthDepartments()).thenReturn(Observable.just(healthDepartment))
        val chunk = createDummyChunk()
        chunk.hashedTraceIds.add(
            dataAccessManager.getHashedTraceId(
                healthDepartment.id, 1, "9bZZ5Ak465V60PXv92aMFA==", chunk.hashLength
            ).blockingGet()
        )
        chunk.hashedTraceIds.add(
            dataAccessManager.getHashedTraceId(
                healthDepartment.id, 1, "99FmQcylJT5e/cyHOjT6Hw==", chunk.hashLength
            ).blockingGet()
        )
        Mockito.`when`(dataAccessManager.fetchUnprocessedChunks()).thenReturn(Observable.just(chunk))
        Mockito.`when`(dataAccessManager.recentTraceIds).thenReturn(Observable.just("9bZZ5Ak465V60PXv92aMFA==", "hCvt6FNlhomxbBmL50PYDw=="))
        dataAccessManager.fetchRecentlyAccessedTraceData()
            .map { it.traceId }
            .test()
            .assertValues("9bZZ5Ak465V60PXv92aMFA==")
            .assertComplete()
    }

    @Test
    fun fetchNewRecentlyAccessedTraceData_someNewDataAccessed_emitsNewAccessedData() {
        val newAccessedTraceData = AccessedTraceData().apply {
            traceId = "LLJMzA/HqlS77qkpUGNJrA=="
            hashedTraceId = "SOME_HASH_VALUE"
        }
        Mockito.`when`(dataAccessManager.fetchRecentlyAccessedTraceData())
            .thenReturn(Observable.just(previouslyAccessedTraceData, newAccessedTraceData))
        Mockito.`when`(dataAccessManager.previouslyAccessedTraceData).thenReturn(Observable.just(previouslyAccessedTraceData))
        dataAccessManager.fetchNewRecentlyAccessedTraceData()
            .test()
            .assertValues(newAccessedTraceData)
            .assertComplete()
    }

    @Test
    fun fetchNewRecentlyAccessedTraceData_newWarningLevelForSameTraceId_emitsNewAccessedData() {
        val newAccessedTraceData = AccessedTraceData().apply {
            traceId = "qiqA2+SpnoioxRMWb7IDsw=="
            hashedTraceId = "HASH_VALUE_FOR_WARNING_LEVEL_2"
            warningLevel = 2
        }
        Mockito.`when`(dataAccessManager.fetchRecentlyAccessedTraceData())
            .thenReturn(Observable.just(previouslyAccessedTraceData, newAccessedTraceData))
        Mockito.`when`(dataAccessManager.previouslyAccessedTraceData).thenReturn(Observable.just(previouslyAccessedTraceData))
        dataAccessManager.fetchNewRecentlyAccessedTraceData()
            .test()
            .assertValues(newAccessedTraceData)
            .assertComplete()
    }

    @Test
    fun previouslyAccessedTraceData_noDataPreviouslyAccessed_completesEmpty() {
        dataAccessManager.previouslyAccessedTraceData
            .test()
            .assertNoValues()
            .assertComplete()
    }

    @Test
    fun previouslyAccessedTraceData_someDataPreviouslyAccessed_emitsPreviouslyAccessedData() {
        dataAccessManager.addToAccessedData(listOf(previouslyAccessedTraceData))
            .andThen(dataAccessManager.previouslyAccessedTraceData)
            .map { it.traceId }
            .test()
            .assertValues(previouslyAccessedTraceData.traceId)
            .assertComplete()
    }

    @Test
    fun accessedTraceDataNotYetInformedAbout_noDataAvailable_completesEmpty() {
        dataAccessManager.addToAccessedData(listOf(previouslyAccessedTraceData))
            .andThen(dataAccessManager.markAllAccessedTraceDataAsInformedAbout())
            .andThen(dataAccessManager.accessedTraceDataNotYetInformedAbout)
            .test()
            .assertNoValues()
            .assertComplete()
    }

    @Test
    fun accessedTraceDataNotYetInformedAbout_someDataAvailable_emitsData() {
        previouslyAccessedTraceData.accessTimestamp = System.currentTimeMillis() - 1000
        val newAccessedTraceData = AccessedTraceData().apply {
            traceId = "LLJMzA/HqlS77qkpUGNJrA=="
            accessTimestamp = System.currentTimeMillis() + 1000
        }
        dataAccessManager.addToAccessedData(listOf(previouslyAccessedTraceData))
            .andThen(dataAccessManager.markAllAccessedTraceDataAsInformedAbout())
            .andThen(dataAccessManager.addToAccessedData(listOf(newAccessedTraceData)))
            .andThen(dataAccessManager.accessedTraceDataNotYetInformedAbout)
            .test()
            .assertValues(newAccessedTraceData)
            .assertComplete()
    }

    @Test
    fun hashedTraceId_validTraceId_emitsExpectedHash() {
        dataAccessManager.getHashedTraceId(
            "8fa43091-261a-45f0-a893-548fc1271025",
            1,
            "9bZZ5Ak465V60PXv92aMFA==",
            16
        ).test().assertValue("F2nfDmOZi7Zygyxivv4A2g==")
    }

    @Test
    fun hashedTraceId_validTraceId_emitsExpectedHash2() {
        dataAccessManager.getHashedTraceId(
            "8fa43091-261a-45f0-a893-548fc1271025",
            1,
            "99FmQcylJT5e/cyHOjT6Hw==",
            16
        ).test().assertValue("YvYkpM4yQ+qM2RpI+C8XbQ==")
    }

    @Test
    fun hashedTraceId_distinctTraceIds_emitsDistinctHashes() {
        val firstHash = dataAccessManager.getHashedTraceId(
            "8fa43091-261a-45f0-a893-548fc1271025",
            1,
            "9bZZ5Ak465V60PXv92aMFA==",
            16
        )
        val secondHash = dataAccessManager.getHashedTraceId(
            "8fa43091-261a-45f0-a893-548fc1271025",
            1,
            "99FmQcylJT5e/cyHOjT6Hw==",
            16
        )
        Single.zip(firstHash, secondHash, { a, b -> a == b })
            .test()
            .assertValue(false)
    }

    @Test
    fun hashedTraceId_distinctHealthDepartments_emitsDistinctHashes() {
        val firstHash = dataAccessManager.getHashedTraceId(
            "8fa43091-261a-45f0-a893-548fc1271025",
            1,
            "9bZZ5Ak465V60PXv92aMFA==",
            16
        )
        val secondHash = dataAccessManager.getHashedTraceId(
            "de4c27f1-2bda-4d50-90cf-7489207de45c",
            1,
            "9bZZ5Ak465V60PXv92aMFA==",
            16
        )
        Single.zip(firstHash, secondHash, { a, b -> a == b })
            .test()
            .assertValue(false)
    }

    @Test
    fun hashedTraceId_distinctWarningLevels_emitsDistinctHashes() {
        val firstHash = dataAccessManager.getHashedTraceId(
            "8fa43091-261a-45f0-a893-548fc1271025",
            1,
            "9bZZ5Ak465V60PXv92aMFA==",
            16
        )
        val secondHash = dataAccessManager.getHashedTraceId(
            "8fa43091-261a-45f0-a893-548fc1271025",
            2,
            "9bZZ5Ak465V60PXv92aMFA==",
            16
        )
        Single.zip(firstHash, secondHash, { a, b -> a == b })
            .test()
            .assertValue(false)
    }

    @Test
    fun restoreAccessedData_noDataPreviouslyPersisted_emitsEmptyData() {
        dataAccessManager.restoreAccessedData()
            .map { it.traceData.size }
            .test()
            .assertValue(0)
    }

    @Test
    fun restoreAccessedData_someDataPreviouslyPersisted_emitsPersistedData() {
        val newAccessedTraceData = AccessedTraceData().apply {
            traceId = "9bZZ5Ak465V60PXv92aMFA=="
        }
        val newAccessedData = AccessedData().apply {
            traceData.add(newAccessedTraceData)
        }
        dataAccessManager.persistAccessedData(newAccessedData)
            .test()
            .assertComplete()
        dataAccessManager.restoreAccessedData()
            .map { it.traceData[0].traceId }
            .test()
            .assertValue(newAccessedTraceData.traceId)
    }

    @Test
    fun persistAccessedData_validData_persistsData() {
        val newAccessedTraceData = AccessedTraceData().apply {
            traceId = "traceId"
        }
        val newAccessedData = AccessedData().apply {
            traceData.add(newAccessedTraceData)
        }
        dataAccessManager.persistAccessedData(newAccessedData)
            .test()
            .assertComplete()
        dataAccessManager.orRestoreAccessedData
            .test()
            .assertValue(newAccessedData)
        dataAccessManager.restoreAccessedData()
            .map { it.traceData[0].traceId }
            .test()
            .assertValue(newAccessedTraceData.traceId)
    }

    @Test
    fun addToAccessedData_validData_updatesAccessedData() {
        val newAccessedTraceData = AccessedTraceData()
        dataAccessManager.addToAccessedData(listOf(newAccessedTraceData))
            .test()
            .assertComplete()
        dataAccessManager.orRestoreAccessedData
            .test()
            .assertValue { it.traceData.contains(newAccessedTraceData) }
        dataAccessManager.restoreAccessedData()
            .map { it.traceData.size }
            .test()
            .assertValue(1)
    }

    @Test
    fun updateIfNecessary_withCheckIns_callsUpdate() {
        Mockito.`when`(dataAccessManager.fetchNotificationConfig()).thenReturn(Single.error(RuntimeException()))
        Mockito.`when`(dataAccessManager.fetchRecentlyAccessedTraceData()).thenReturn(Observable.empty())
        Mockito.`when`(checkInManager.archivedTraceIds).thenReturn(Observable.just("anything"))
        dataAccessManager.updateIfNecessary().blockingAwait()
        Mockito.verify(dataAccessManager, Mockito.times(1)).update()
    }

    @Test
    fun updateIfNecessary_withoutCheckIns_doesNotCallUpdate() {
        Mockito.`when`(checkInManager.archivedTraceIds).thenReturn(Observable.empty())
        dataAccessManager.updateIfNecessary().blockingAwait()
        Mockito.verify(dataAccessManager, Mockito.never()).update()
    }

    private fun createDummyHealthDepartment(): NotifyingHealthDepartment {
        return NotifyingHealthDepartment(
            "8fa43091-261a-45f0-a893-548fc1271025",
            "Dummy Department",
            null,
            null
        )
    }

    private fun createDummyChunk(): NotificationDataChunk {
        return NotificationDataChunk(
            1,
            0,
            16,
            System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1),
            "3cYZ5Ak465V80PXv93aMFB==",
            ArrayList()
        )
    }

    @Test
    fun isNew_afterAdding_isTrue() {
        dataAccessManager.addToAccessedData(listOf(previouslyAccessedTraceData))
            .andThen(dataAccessManager.isNewNotification(previouslyAccessedTraceData.traceId))
            .test().assertValue(true)
    }

    @Test
    fun isNew_afterMarkingAsNotNew_isFalse() {
        dataAccessManager.addToAccessedData(listOf(previouslyAccessedTraceData))
            .andThen(dataAccessManager.markAsNotNew(previouslyAccessedTraceData.traceId, previouslyAccessedTraceData.warningLevel))
            .andThen(dataAccessManager.isNewNotification(previouslyAccessedTraceData.traceId))
            .test().assertValue(false)
    }

    @Test
    fun hasNewNotifications_initially_isFalse() {
        dataAccessManager.hasNewNotifications()
            .test().assertValue(false)
    }

    @Test
    fun hasNewNotifications_afterAddingOne_isTrue() {
        dataAccessManager.addToAccessedData(listOf(previouslyAccessedTraceData))
            .andThen(dataAccessManager.hasNewNotifications())
            .test().assertValue(true)
    }
}