package de.culture4life.luca.util

import androidx.test.runner.AndroidJUnit4
import de.culture4life.luca.LucaUnitTest
import de.culture4life.luca.document.Document
import de.culture4life.luca.util.TimeUtil.convertToUnixTimestamp
import de.culture4life.luca.util.TimeUtil.encodeUnixTimestamp
import de.culture4life.luca.util.TimeUtil.getReadableDurationWithPlural
import de.culture4life.luca.util.TimeUtil.roundUnixTimestampDownToMinute
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

@Config(sdk = [28])
@RunWith(AndroidJUnit4::class)
class TimeUtilTest : LucaUnitTest() {
    @Test
    fun convertToUnixTimestamp() {
        convertToUnixTimestamp(1601481612123L)
            .test()
            .assertValue(1601481612L)
    }

    @Test
    fun roundUnixTimestampDownToMinute() {
        roundUnixTimestampDownToMinute(1601481612L)
            .test()
            .assertValue(1601481600L)
    }

    @Test
    fun encodeUnixTimestamp() {
        encodeUnixTimestamp(1601481600L)
            .map { ByteBuffer.wrap(it).int }
            .test()
            .assertValue(-2136247201)
    }

    @Test
    fun readableDurationWithPlural_evenDurations_expectedEvenAmounts() {
            getReadableDurationWithPlural(TimeUnit.SECONDS.toMillis(0), application)
                .test().assertValue("0 seconds")
            getReadableDurationWithPlural(TimeUnit.SECONDS.toMillis(1), application)
                .test().assertValue("1 second")
            getReadableDurationWithPlural(TimeUnit.SECONDS.toMillis(2), application)
                .test().assertValue("2 seconds")
            getReadableDurationWithPlural(TimeUnit.MINUTES.toMillis(2), application)
                .test().assertValue("2 minutes")
            getReadableDurationWithPlural(TimeUnit.HOURS.toMillis(2), application)
                .test().assertValue("2 hours")
            getReadableDurationWithPlural(TimeUnit.DAYS.toMillis(2), application)
                .test().assertValue("2 days")
        }

    @Test
    fun readableDurationWithPlural_unevenDurations_expectedRoundedAmounts() {
            var duration = TimeUnit.MINUTES.toMillis(2) + TimeUnit.SECONDS.toMillis(30)
            getReadableDurationWithPlural(duration, application)
                .test().assertValue("3 minutes")
            duration = TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(30)
            getReadableDurationWithPlural(duration, application)
                .test().assertValue("3 hours")
            duration = TimeUnit.DAYS.toMillis(2) + TimeUnit.HOURS.toMillis(12)
            getReadableDurationWithPlural(duration, application)
                .test().assertValue("3 days")
        }

    // Wed Jul 14 2021 02:00:00 GMT+0200
    @Test
    fun readableDurationWithPlural_vaccinationValidity_expectedDuration() {
            val testingTimestamp = 1626220800000L // Wed Jul 14 2021 02:00:00 GMT+0200
            val validityStartTimestamp = testingTimestamp + Document.TIME_UNTIL_VACCINATION_IS_VALID
            var currentTimestamp = testingTimestamp + TimeUnit.HOURS.toMillis(2)
            var durationUntilValid = validityStartTimestamp - currentTimestamp
            getReadableDurationWithPlural(durationUntilValid, application)
                .test().assertValue("15 days")
            currentTimestamp = validityStartTimestamp - TimeUnit.HOURS.toMillis(2)
            durationUntilValid = validityStartTimestamp - currentTimestamp
            getReadableDurationWithPlural(durationUntilValid, application)
                .test().assertValue("2 hours")
        }
}