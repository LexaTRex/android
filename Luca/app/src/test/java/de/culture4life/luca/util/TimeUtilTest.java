package de.culture4life.luca.util;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import de.culture4life.luca.LucaUnitTest;

import static de.culture4life.luca.document.Document.TIME_UNTIL_VACCINATION_IS_VALID;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class TimeUtilTest extends LucaUnitTest {

    @Test
    public void convertToUnixTimestamp() {
        TimeUtil.convertToUnixTimestamp(1601481612123L)
                .test()
                .assertValue(1601481612L);
    }

    @Test
    public void roundUnixTimestampDownToMinute() {
        TimeUtil.roundUnixTimestampDownToMinute(1601481612L)
                .test()
                .assertValue(1601481600L);
    }

    @Test
    public void encodeUnixTimestamp() {
        TimeUtil.encodeUnixTimestamp(1601481600L)
                .map(bytes -> ByteBuffer.wrap(bytes).getInt())
                .test()
                .assertValue(-2136247201);
    }

    @Test
    public void getReadableDurationWithPlural_evenDurations_expectedEvenAmounts() {
        TimeUtil.getReadableDurationWithPlural(TimeUnit.SECONDS.toMillis(0), application)
                .test().assertValue("0 seconds");

        TimeUtil.getReadableDurationWithPlural(TimeUnit.SECONDS.toMillis(1), application)
                .test().assertValue("1 second");

        TimeUtil.getReadableDurationWithPlural(TimeUnit.SECONDS.toMillis(2), application)
                .test().assertValue("2 seconds");

        TimeUtil.getReadableDurationWithPlural(TimeUnit.MINUTES.toMillis(2), application)
                .test().assertValue("2 minutes");

        TimeUtil.getReadableDurationWithPlural(TimeUnit.HOURS.toMillis(2), application)
                .test().assertValue("2 hours");

        TimeUtil.getReadableDurationWithPlural(TimeUnit.DAYS.toMillis(2), application)
                .test().assertValue("2 days");
    }

    @Test
    public void getReadableDurationWithPlural_unevenDurations_expectedRoundedAmounts() {
        long duration = TimeUnit.MINUTES.toMillis(2) + TimeUnit.SECONDS.toMillis(30);
        TimeUtil.getReadableDurationWithPlural(duration, application)
                .test().assertValue("3 minutes");

        duration = TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(30);
        TimeUtil.getReadableDurationWithPlural(duration, application)
                .test().assertValue("3 hours");

        duration = TimeUnit.DAYS.toMillis(2) + TimeUnit.HOURS.toMillis(12);
        TimeUtil.getReadableDurationWithPlural(duration, application)
                .test().assertValue("3 days");
    }

    @Test
    public void getReadableDurationWithPlural_vaccinationValidity_expectedDuration() {
        long testingTimestamp = 1626220800000L; // Wed Jul 14 2021 02:00:00 GMT+0200
        long validityStartTimestamp = testingTimestamp + TIME_UNTIL_VACCINATION_IS_VALID;
        long currentTimestamp = testingTimestamp + TimeUnit.HOURS.toMillis(2);
        long durationUntilValid = validityStartTimestamp - currentTimestamp;

        TimeUtil.getReadableDurationWithPlural(durationUntilValid, application)
                .test().assertValue("15 days");

        currentTimestamp = validityStartTimestamp - TimeUnit.HOURS.toMillis(2);
        durationUntilValid = validityStartTimestamp - currentTimestamp;

        TimeUtil.getReadableDurationWithPlural(durationUntilValid, application)
                .test().assertValue("2 hours");

    }

}