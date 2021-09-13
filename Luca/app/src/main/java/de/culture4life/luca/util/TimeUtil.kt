package de.culture4life.luca.util

import android.content.Context
import de.culture4life.luca.R
import io.reactivex.rxjava3.core.Single
import org.joda.time.format.DateTimeFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

object TimeUtil {

    @JvmStatic
    fun getCurrentUnixTimestamp(): Single<Long> {
        return convertToUnixTimestamp(System.currentTimeMillis())
    }

    @JvmStatic
    fun convertFromUnixTimestamp(unixTimestamp: Long): Single<Long> {
        return Single.just(unixTimestamp)
            .map { TimeUnit.SECONDS.toMillis(it) }
    }

    @JvmStatic
    fun convertToUnixTimestamp(timestampInMilliseconds: Long): Single<Long> {
        return Single.just(timestampInMilliseconds)
            .map { TimeUnit.MILLISECONDS.toSeconds(it) }
    }

    @JvmStatic
    fun roundUnixTimestampDownToMinute(unixTimestamp: Long): Single<Long> {
        return Single.just(unixTimestamp)
            .map { TimeUnit.SECONDS.toMinutes(it) }
            .map { TimeUnit.MINUTES.toSeconds(it) }
    }

    @JvmStatic
    fun encodeUnixTimestamp(unixTimestamp: Long): Single<ByteArray> {
        return Single.fromCallable {
            val byteBuffer = ByteBuffer.allocate(4)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            byteBuffer.putInt(unixTimestamp.toInt())
            byteBuffer.array()
        }
    }

    @JvmStatic
    fun decodeUnixTimestamp(encodedUnixTimestamp: ByteArray): Single<Long> {
        return Single.fromCallable {
            val byteBuffer = ByteBuffer.wrap(encodedUnixTimestamp)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            byteBuffer.int.toLong()
        }
    }

    @JvmStatic
    fun getReadableDurationWithPlural(duration: Long, context: Context): Single<String> {
        return Single.fromCallable {
            val timeUnit: TimeUnit
            val unitStringResource: Int
            if (duration < TimeUnit.MINUTES.toMillis(1)) {
                timeUnit = TimeUnit.SECONDS
                unitStringResource = R.plurals.time_plural_seconds
            } else if (duration < TimeUnit.HOURS.toMillis(1)) {
                timeUnit = TimeUnit.MINUTES
                unitStringResource = R.plurals.time_plural_minutes
            } else if (duration < TimeUnit.DAYS.toMillis(1)) {
                timeUnit = TimeUnit.HOURS
                unitStringResource = R.plurals.time_plural_hours
            } else {
                timeUnit = TimeUnit.DAYS
                unitStringResource = R.plurals.time_plural_days
            }
            var amount = timeUnit.convert(duration, TimeUnit.MILLISECONDS).toInt()
            if (duration - timeUnit.toMillis(amount.toLong()) > 0) {
                amount++
            }
            context.resources.getQuantityString(unitStringResource, amount, amount)
        }
    }

    @JvmStatic
    fun getStartOfCurrentDayTimestamp(): Single<Long> {
        return Single.fromCallable { System.currentTimeMillis() }
            .flatMap { getStartOfDayTimestamp(it) }
    }

    @JvmStatic
    fun getStartOfDayTimestamp(timestamp: Long): Single<Long> {
        return Single.just(timestamp - timestamp % (60 * 60 * 24 * 1000))
    }

    @JvmStatic
    fun getReadableTime(context: Context, timestamp: Long): String {
        return context.getReadableTime(timestamp)
    }

}

fun Context.getReadableTime(timestamp: Long): String {
    return DateTimeFormat.forPattern(getString(R.string.time_format)).print(timestamp)
}

fun Context.getReadableDate(timestamp: Long): String {
    return DateTimeFormat.forPattern(getString(R.string.date_format)).print(timestamp)
}