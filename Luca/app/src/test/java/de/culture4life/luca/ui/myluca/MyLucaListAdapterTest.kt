package de.culture4life.luca.ui.myluca

import androidx.test.runner.AndroidJUnit4
import de.culture4life.luca.LucaUnitTest
import de.culture4life.luca.children.Child
import de.culture4life.luca.document.Document
import de.culture4life.luca.registration.Person
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [28])
@RunWith(AndroidJUnit4::class)
class MyLucaListAdapterTest : LucaUnitTest() {
    private val document = Document().apply {
        firstName = "Erika"
        lastName = "Mustermann"
        encodedData = "asdf"
    }
    private val item = TestResultItem(application, document)

    @Test
    fun isFrom_correctPerson_returnsTrue() {
        assertTrue(MyLucaListAdapter.isFrom(item, Person("Erika", "Mustermann")))
    }

    @Test
    fun isFrom_documentWithoutName_returnsTrueForAdult() {
        item.document.apply {
            firstName = null
            lastName = null
        }
        assertTrue(MyLucaListAdapter.isFrom(item, Person("Erika", "Mustermann")))
    }

    @Test
    fun isFrom_documentWithoutName_returnsFalseForChild() {
        item.document.apply {
            firstName = null
            lastName = null
        }
        assertFalse(MyLucaListAdapter.isFrom(item, Child("Erika", "Mustermann")))
    }

    @Test
    fun isFrom_otherPerson_returnsFalse() {
        assertFalse(MyLucaListAdapter.isFrom(item, Person("Janine", "Mustermann")))
    }

    @Test
    fun isFrom_personNameDifferentCase_returnsTrue() {
        assertTrue(MyLucaListAdapter.isFrom(item, Person("eRiKa", "mUsTeRmAnN")))
    }

    @Test
    fun isFrom_titlesAreFiltered_returnTrue() {
        assertTrue(MyLucaListAdapter.isFrom(item, Person("Prof. Dr. Erika", "Mustermann")))
    }

    @Test
    fun isFrom_specialCharsRemoved_returnTrue() {
        assertTrue(MyLucaListAdapter.isFrom(item, Person("Erikâa", "Mustêermann")))
    }

    @Test
    fun sortAndPairItems_correctPerson_returnsHeaderAndItem() {
        val items =
            MyLucaListAdapter.sortAndPairItems(listOf(item), listOf(Person("Erika", "Mustermann")))
        assertEquals(items[0].sectionHeader, "Erika Mustermann")
        assertEquals(items[1].items[0], item)
    }

    @Test
    fun sortAndPairItems_personWithChildren_returnsHeadersAndItem() {
        val persons = listOf(Person("Erika", "Mustermann"), Child("Child", "Name"))
        val items = MyLucaListAdapter.sortAndPairItems(listOf(item), persons)
        assertEquals(items[0].sectionHeader, "Erika Mustermann")
        assertEquals(items[1].items[0], item)
        assertEquals(items[2].sectionHeader, "Child Name")
    }

    @Test
    fun sortAndPairItems_wrongPerson_returnsEmptyList() {
        val items =
            MyLucaListAdapter.sortAndPairItems(listOf(item), listOf(Person("Anyone", "Else")))
        assertEquals(0, items.size)
    }
}