package de.culture4life.luca.testing;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class TestResultTest {

    private TestResult testResult = new TestResult();

    @Test
    public void getExpirationTime_forFastTest_isTwoDays() {
        testResult.setType(TestResult.TYPE_FAST);
        Assert.assertEquals(testResult.getExpirationTimestamp(), TimeUnit.DAYS.toMillis(2));
    }

    @Test
    public void getExpirationTime_forPcrTest_isThreeDays() {
        testResult.setType(TestResult.TYPE_PCR);
        Assert.assertEquals(testResult.getExpirationTimestamp(), TimeUnit.DAYS.toMillis(3));
    }
}