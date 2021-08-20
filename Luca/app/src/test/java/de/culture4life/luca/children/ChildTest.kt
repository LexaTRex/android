package de.culture4life.luca.children

import de.culture4life.luca.registration.Person
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChildTest {

    private fun createChildFrom(name: String, adultLastName: String): Child {
        return Child.from(name, adultLastName)
    }

    @Test
    fun createChildFrom_firstAndLastNames() {
        assertEquals(Child("Erika", "Mustermann"), createChildFrom("Erika Mustermann", "Mustermann"))
    }

    @Test
    fun createChildFrom_multipleFirstAndLastNames() {
        assertEquals(Child("Erika Maria", "Mustermann Musterfrau"),
            createChildFrom("Erika Maria Mustermann Musterfrau", "Mustermann Musterfrau"))
    }

    @Test
    fun createChildFrom_multipleFirstAndLastNamesWithSpaces() {
        assertEquals(Child("Erika Maria", "Mustermann Musterfrau"),
            createChildFrom("Erika Maria   Mustermann Musterfrau ", "Mustermann Musterfrau"))
    }

    @Test
    fun equals_sameChild_returnsTrue() {
        assertEquals(Child("a", "b"), Child("a", "b"))
    }

    @Test
    fun equals_sameNameForChildAndPerson_returnsFalse() {
        assertNotEquals(Person("a", "b"), Child("a", "b"))
    }

    @Test
    fun equals_differentNames_returnsFalse() {
        assertNotEquals(Child("a", "b"), Child("c", "d"))
    }

}