package de.culture4life.luca.document;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DocumentTest {

    private Document document;

    @Before
    public void setUp() {
        document = new Document();
    }

    @Test
    public void getExpirationTimestamp_differentTestingTimestamps_differentExpirationTimestamps() {
        document.setTestingTimestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2));
        long firstExpirationTimestamp = document.getExpirationTimestamp();

        document.setTestingTimestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
        long secondExpirationTimestamp = document.getExpirationTimestamp();

        assertTrue(firstExpirationTimestamp < secondExpirationTimestamp);
    }

    @Test
    public void getExpirationTimestamp_differentTypes_differentExpirationTimestamps() {
        document.setType(Document.TYPE_FAST);
        long fastExpirationTimestamp = document.getExpirationTimestamp();

        document.setType(Document.TYPE_PCR);
        long pcrExpirationTimestamp = document.getExpirationTimestamp();

        assertTrue(fastExpirationTimestamp < pcrExpirationTimestamp);
    }

    @Test
    public void validFrom_vaccination_isLaterThanTestValidFrom() {
        document.setType(Document.TYPE_VACCINATION);
        document.setOutcome(Document.OUTCOME_FULLY_IMMUNE);
        long vaccinationValidFrom = document.getValidityStartTimestamp();

        document.setType(Document.TYPE_PCR);
        long testValidFrom = document.getValidityStartTimestamp();

        assertTrue(testValidFrom < vaccinationValidFrom);
    }

    @Test
    public void getExpirationDuration_typeFast_isTwoDays() {
        assertEquals(TimeUnit.DAYS.toMillis(2), Document.getExpirationDuration(Document.TYPE_FAST));
    }

    @Test
    public void getExpirationDuration_typePcr_isThreeDays() {
        assertEquals(TimeUnit.DAYS.toMillis(3), Document.getExpirationDuration(Document.TYPE_PCR));
    }

    @Test
    public void getExpirationDuration_typeVaccination_isOneYear() {
        assertEquals(TimeUnit.DAYS.toMillis(365), Document.getExpirationDuration(Document.TYPE_VACCINATION));
    }

}