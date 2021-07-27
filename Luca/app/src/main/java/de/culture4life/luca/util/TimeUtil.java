package de.culture4life.luca.util;

import android.content.Context;

import de.culture4life.luca.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.core.Single;

public final class TimeUtil {

    private TimeUtil() {
    }

    public static Single<Long> getCurrentUnixTimestamp() {
        return convertToUnixTimestamp(System.currentTimeMillis());
    }

    public static Single<Long> convertFromUnixTimestamp(long unixTimestamp) {
        return Single.just(unixTimestamp)
                .map(TimeUnit.SECONDS::toMillis);
    }

    public static Single<Long> convertToUnixTimestamp(long timestampInMilliseconds) {
        return Single.just(timestampInMilliseconds)
                .map(TimeUnit.MILLISECONDS::toSeconds);
    }

    public static Single<Long> roundUnixTimestampDownToMinute(long unixTimestamp) {
        return Single.just(unixTimestamp)
                .map(TimeUnit.SECONDS::toMinutes)
                .map(TimeUnit.MINUTES::toSeconds);
    }

    public static Single<byte[]> encodeUnixTimestamp(long unixTimestamp) {
        return Single.fromCallable(() -> {
            final ByteBuffer byteBuffer = ByteBuffer.allocate(4);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putInt((int) unixTimestamp);
            return byteBuffer.array();
        });
    }

    public static Single<Long> decodeUnixTimestamp(byte[] encodedUnixTimestamp) {
        return Single.fromCallable(() -> {
            final ByteBuffer byteBuffer = ByteBuffer.wrap(encodedUnixTimestamp);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            return (long) byteBuffer.getInt();
        });
    }

    public static Single<String> getReadableDurationWithPlural(long duration, @NonNull Context context) {
        return Single.fromCallable(() -> {
            TimeUnit timeUnit;
            int unitStringResource;
            if (duration < TimeUnit.MINUTES.toMillis(1)) {
                timeUnit = TimeUnit.SECONDS;
                unitStringResource = R.plurals.time_plural_seconds;
            } else if (duration < TimeUnit.HOURS.toMillis(1)) {
                timeUnit = TimeUnit.MINUTES;
                unitStringResource = R.plurals.time_plural_minutes;
            } else if (duration < TimeUnit.DAYS.toMillis(1)) {
                timeUnit = TimeUnit.HOURS;
                unitStringResource = R.plurals.time_plural_hours;
            } else {
                timeUnit = TimeUnit.DAYS;
                unitStringResource = R.plurals.time_plural_days;
            }
            int amount = (int) timeUnit.convert(duration, TimeUnit.MILLISECONDS);
            if (duration - timeUnit.toMillis(amount) > 0) {
                amount++;
            }
            return context.getResources().getQuantityString(unitStringResource, amount, amount);
        });
    }

    public static Single<Long> getStartOfDayTimestamp() {
        return Single.fromCallable(System::currentTimeMillis)
                .map(TimeUnit.MILLISECONDS::toDays)
                .map(TimeUnit.DAYS::toMillis);
    }

}
