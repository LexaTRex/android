package de.culture4life.luca.testing;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestResultTest {

    private TestResult testResult;

    @Before
    public void setUp() {
        testResult = new TestResult();
    }

    @Test
    public void getExpirationTimestamp_differentTestingTimestamps_differentExpirationTimestamps() {
        testResult.setTestingTimestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2));
        long firstExpirationTimestamp = testResult.getExpirationTimestamp();

        testResult.setTestingTimestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
        long secondExpirationTimestamp = testResult.getExpirationTimestamp();

        assertTrue(firstExpirationTimestamp < secondExpirationTimestamp);
    }

    @Test
    public void getExpirationTimestamp_differentTypes_differentExpirationTimestamps() {
        testResult.setType(TestResult.TYPE_FAST);
        long fastExpirationTimestamp = testResult.getExpirationTimestamp();

        testResult.setType(TestResult.TYPE_PCR);
        long pcrExpirationTimestamp = testResult.getExpirationTimestamp();

        assertTrue(fastExpirationTimestamp < pcrExpirationTimestamp);
    }

    @Test
    public void validFrom_vaccination_isLaterThanTestValidFrom() {
        testResult.setType(TestResult.TYPE_VACCINATION);
        testResult.setOutcome(TestResult.OUTCOME_FULLY_VACCINATED);
        long vaccinationValidFrom = testResult.getValidityStartTimestamp();

        testResult.setType(TestResult.TYPE_PCR);
        long testValidFrom = testResult.getValidityStartTimestamp();

        assertTrue(testValidFrom < vaccinationValidFrom);
    }

    @Test
    public void getExpirationDuration_typeFast_isTwoDays() {
        assertEquals(TimeUnit.DAYS.toMillis(2), TestResult.getExpirationDuration(TestResult.TYPE_FAST));
    }

    @Test
    public void getExpirationDuration_typePcr_isThreeDays() {
        assertEquals(TimeUnit.DAYS.toMillis(3), TestResult.getExpirationDuration(TestResult.TYPE_PCR));
    }

    @Test
    public void getExpirationDuration_typeVaccination_isOneYear() {
        assertEquals(TimeUnit.DAYS.toMillis(365), TestResult.getExpirationDuration(TestResult.TYPE_VACCINATION));
    }

}