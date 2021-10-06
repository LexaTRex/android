package de.culture4life.luca.children

import androidx.test.runner.AndroidJUnit4
import de.culture4life.luca.LucaUnitTest
import de.culture4life.luca.crypto.CryptoManager
import de.culture4life.luca.genuinity.GenuinityManager
import de.culture4life.luca.network.NetworkManager
import de.culture4life.luca.preference.PreferencesManager
import de.culture4life.luca.registration.RegistrationManager
import de.culture4life.luca.ui.children.LegacyChildListItem
import de.culture4life.luca.ui.children.LegacyChildListItemContainer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class ChildrenManagerTest : LucaUnitTest() {

    private val preferencesManager = PreferencesManager()
    private val networkManager = NetworkManager()
    private val genuinityManager = GenuinityManager(preferencesManager, networkManager)
    private val cryptoManager = CryptoManager(preferencesManager, networkManager, genuinityManager)
    private val registrationManager = RegistrationManager(preferencesManager, networkManager, cryptoManager)
    private val childrenManager = ChildrenManager(preferencesManager, registrationManager)

    @Before
    fun setUp() {
        preferencesManager.initialize(application)
        networkManager.initialize(application)
        cryptoManager.initialize(application)
        registrationManager.initialize(application)
        childrenManager.initialize(application)
            .blockingAwait()
    }

    @Test
    fun getChildren_initially_returnsEmptyList() {
        childrenManager.getChildren()
            .map { childListItems: Children -> childListItems.size }
            .test().assertValue(0)
    }

    private val child = Child("Child", "Name")

    @Test
    fun getChildren_withOneChild_returnsIt() {
        childrenManager.addChild(child)
            .andThen(childrenManager.getChildren())
            .map { childListItems -> childListItems[0].firstName }
            .test().assertValue(child.firstName)
    }

    @Test
    fun getCheckedInChildren_initially_returnsEmptyList() {
        childrenManager.getCheckedInChildren()
            .map { childListItems -> childListItems.size }
            .test().assertValue(0)
    }

    @Test
    fun getCheckedInChildren_withCheckedInChild_returnsIt() {
        childrenManager.addChild(child)
            .andThen(childrenManager.checkIn(child))
            .andThen(childrenManager.getCheckedInChildren())
            .map { childListItems -> childListItems[0].firstName }
            .test().assertValue(child.firstName)
    }

    @Test
    fun getCheckedInChildren_withCheckedInAndCheckedOutChild_returnsEmptyList() {
        childrenManager.addChild(child)
            .andThen(childrenManager.checkOut(child))
            .andThen(childrenManager.getCheckedInChildren())
            .map { childListItems -> childListItems.size }
            .test().assertValue(0)
    }

    @Test
    fun isCheckedIn_initially_isFalse() {
        childrenManager.isCheckedIn(child).test().assertValue(false)
    }

    @Test
    fun isCheckedIn_afterCheckIn_isTrue() {
        childrenManager.checkIn(child)
            .andThen(childrenManager.isCheckedIn(child))
            .test().assertValue(true)
    }

    @Test
    fun isCheckedIn_afterCheckInAndOut_isFalse() {
        childrenManager.checkIn(child)
            .andThen(childrenManager.checkOut(child))
            .andThen(childrenManager.isCheckedIn(child))
            .test().assertValue(false)
    }

    @Test
    fun isCheckedIn_afterDeletingChildren_isFalse() {
        childrenManager.checkIn(child)
            .andThen(childrenManager.persistChildren(Children()))
            .andThen(childrenManager.isCheckedIn(child))
            .test().assertValue(false)
    }

    @Test
    fun migrate_withLegacyChild_migratesToNewFormat() {
        val children = LegacyChildListItemContainer().apply { add(LegacyChildListItem("Legacy Child")) }
        preferencesManager.persist(ChildrenManager.LEGACY_KEY_CHILDREN, children)
            .andThen(childrenManager.migrate())
            .andThen(childrenManager.getChildren())
            .map { it[0].firstName }
            .test().assertValue("Legacy Child")
    }

    @Test
    fun isValidChildName_normalNames_isTrue() {
        assertTrue(ChildrenManager.isValidChildName("Erika"))
        assertTrue(ChildrenManager.isValidChildName("Jean-Pierre"))
        assertTrue(ChildrenManager.isValidChildName("Erika Maria Musterfrau"))
    }

    @Test
    fun isValidChildName_notRealNames_isFalse() {
        assertFalse(ChildrenManager.isValidChildName(""))
        assertFalse(ChildrenManager.isValidChildName(" "))
        assertFalse(ChildrenManager.isValidChildName("/*()"))
    }

    @Test
    fun isValidChildName_validChildObject_isTrue() {
        assertTrue(ChildrenManager.isValidChildName(Child("Max", "Mustermann")))
    }

    @Test
    fun isValidChildName_invalidChildObject_isFalse() {
        assertFalse(ChildrenManager.isValidChildName(Child("Max", "")))
    }

    @Test
    fun containsChild_emptyList_isFalse() {
        childrenManager.containsChild(child).test().assertValue(false)
    }

    @Test
    fun containsChild_inList_isTrue() {
        childrenManager.addChild(child)
            .andThen(childrenManager.containsChild(child))
            .test().assertValue(true)
    }

    @Test
    fun containsChild_inListWithAnotherChild_isFalse() {
        childrenManager.addChild(Child("Another", "Child"))
            .andThen(childrenManager.containsChild(child))
            .test().assertValue(false)
    }

}