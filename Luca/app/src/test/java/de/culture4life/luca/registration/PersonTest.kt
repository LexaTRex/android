package de.culture4life.luca.registration

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonTest {
    @Test
    fun equals_samePerson_returnsTrue() {
        assertEquals(Person("a", "b"), Person("a", "b"))
    }
}
