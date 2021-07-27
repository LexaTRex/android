package de.culture4life.luca.ui.venue.children

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VenueChildrenViewModelTest {
    @Test
    fun isValidChildName_normalNames_isTrue() {
        assertTrue(VenueChildrenViewModel.isValidChildName("Erika"))
        assertTrue(VenueChildrenViewModel.isValidChildName("Jean-Pierre"))
        assertTrue(VenueChildrenViewModel.isValidChildName("Erika Maria Musterfrau"))
    }

    @Test
    fun isValidChildName_notRealNames_isFalse() {
        assertFalse(VenueChildrenViewModel.isValidChildName(""))
        assertFalse(VenueChildrenViewModel.isValidChildName(" "))
        assertFalse(VenueChildrenViewModel.isValidChildName("/*()"))
    }
}