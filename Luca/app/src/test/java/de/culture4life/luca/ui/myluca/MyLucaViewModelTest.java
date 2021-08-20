package de.culture4life.luca.ui.myluca;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import de.culture4life.luca.document.Document;

public class MyLucaViewModelTest {

    private final Document DOCUMENT_PERSON_1 = new TestDocument("Max", "Musterman", 0);
    private final Document DOCUMENT_PERSON_2 = new TestDocument("Erika", "Musterfrau", 0);

    @Test
    public void hasNonMatchingBirthDate_noMatches_returnsFalse() {
        assertFalse(MyLucaViewModel.hasNonMatchingBirthDate(DOCUMENT_PERSON_1, DOCUMENT_PERSON_2));
    }

    @Test
    public void hasNonMatchingBirthDate_matchesSameBirthday_returnsFalse() {
        assertFalse(MyLucaViewModel.hasNonMatchingBirthDate(DOCUMENT_PERSON_1, DOCUMENT_PERSON_1));
    }

    @Test
    public void hasNonMatchingBirthDate_matchesDifferentTimestampSameDay_returnsFalse() {
        Document documentWithDifferentDOB = new TestDocument(DOCUMENT_PERSON_1);
        documentWithDifferentDOB.setDateOfBirth(TimeUnit.HOURS.toMillis(12));
        assertFalse(MyLucaViewModel.hasNonMatchingBirthDate(DOCUMENT_PERSON_1, documentWithDifferentDOB));
    }

    @Test
    public void hasNonMatchingBirthDate_matchesDifferentBirthday_returnsTrue() {
        Document documentWithDifferentDOB = new TestDocument(DOCUMENT_PERSON_1);
        documentWithDifferentDOB.setDateOfBirth(TimeUnit.DAYS.toMillis(1));
        assertTrue(MyLucaViewModel.hasNonMatchingBirthDate(DOCUMENT_PERSON_1, documentWithDifferentDOB));
    }

    @Test
    public void hasNonMatchingBirthDate_withEmptyMyLucaListItems_returnsFalse() {
        DOCUMENT_PERSON_1.setType(Document.TYPE_VACCINATION);
        Document document = new TestDocument(DOCUMENT_PERSON_1);
        document.setDateOfBirth(TimeUnit.DAYS.toMillis(1));
        assertFalse(MyLucaViewModel.hasNonMatchingBirthDate(DOCUMENT_PERSON_1, new ArrayList<>()));
    }

    @Test
    public void hasNonMatchingBirthDate_withSameMyLucaListItem_returnsTrue() {
        DOCUMENT_PERSON_1.setType(Document.TYPE_VACCINATION);
        Document document = new TestDocument(DOCUMENT_PERSON_1.getFirstName(), DOCUMENT_PERSON_1.getLastName(), TimeUnit.DAYS.toMillis(1));
        ArrayList<MyLucaListItem> myLucaItems = new ArrayList<>();
        VaccinationItem mockItem = Mockito.mock(VaccinationItem.class);
        Mockito.doReturn(document).when(mockItem).getDocument();
        myLucaItems.add(mockItem);
        assertTrue(MyLucaViewModel.hasNonMatchingBirthDate(DOCUMENT_PERSON_1, myLucaItems));
    }

    static class TestDocument extends Document {

        public TestDocument(String firstName, String lastName, long dateOfBirth) {
            setFirstName(firstName);
            setLastName(lastName);
            setDateOfBirth(dateOfBirth);
        }

        public TestDocument(Document document) {
            setFirstName(document.getFirstName());
            setLastName(document.getLastName());
            setDateOfBirth(document.getDateOfBirth());
        }

    }

}
