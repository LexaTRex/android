package de.culture4life.luca.ui.myluca;

import com.google.zxing.EncodeHintType;

import android.content.Context;
import android.graphics.Bitmap;

import de.culture4life.luca.R;

import net.glxn.qrgen.android.QRCode;

import java.lang.annotation.Retention;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import io.reactivex.rxjava3.core.Single;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public abstract class MyLucaListItem {

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_TEST_RESULT = 1;
    public static final int TYPE_APPOINTMENT = 2;
    public static final int TYPE_GREEN_PASS = 3;

    @IntDef({TYPE_UNKNOWN, TYPE_TEST_RESULT, TYPE_APPOINTMENT, TYPE_GREEN_PASS})
    @Retention(SOURCE)
    public @interface Type {

    }

    @Type
    protected final int type;

    protected String title;
    protected String description;
    protected String time;
    protected Bitmap barcode;
    protected long timestamp;
    @ColorInt
    protected int color;
    @DrawableRes
    protected int imageResource;
    protected boolean isExpanded = false;

    public MyLucaListItem(@Type int type) {
        this.type = type;
    }

    protected static Single<Bitmap> generateQrCode(@NonNull String data) {
        return Single.fromCallable(() -> QRCode.from(data)
                .withSize(500, 500)
                .withHint(EncodeHintType.MARGIN, 0)
                .bitmap());
    }

    protected static String getReadableTime(@NonNull Context context, long timestamp) {
        return getReadableTime(getDateFormat(context), timestamp);
    }

    protected static String getReadableTime(@NonNull SimpleDateFormat readableDateFormat, long timestamp) {
        return readableDateFormat.format(new Date(timestamp));
    }

    protected static SimpleDateFormat getDateFormat(@NonNull Context context) {
        return new SimpleDateFormat(context.getString(R.string.venue_checked_in_time_format), Locale.GERMANY);
    }

    public void toggleExpanded() {
        isExpanded = !isExpanded;
    }

    public int getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTime() {
        return time;
    }

    public Bitmap getBarcode() {
        return barcode;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @ColorInt
    public int getColor() {
        return color;
    }

    @DrawableRes
    public int getImageResource() {
        return imageResource;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

}
