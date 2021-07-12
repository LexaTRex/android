package de.culture4life.luca.ui.myluca;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.zxing.EncodeHintType;

import net.glxn.qrgen.android.QRCode;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.culture4life.luca.R;
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

    protected final List<Pair<String, String>> topContent = new ArrayList<>();
    protected final List<Pair<String, String>> collapsedContent = new ArrayList<>();

    protected String title;
    protected String provider;
    protected Bitmap barcode;
    protected long timestamp;
    protected long resultTimestamp;
    protected String deleteButtonText;
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

    protected static String getReadableDate(@NonNull Context context, long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(context.getString(R.string.date_format), Locale.GERMANY);
        return getReadableTime(dateFormat, timestamp);
    }

    protected static String getReadableTime(@NonNull Context context, long timestamp) {
        SimpleDateFormat timeFormat = new SimpleDateFormat(context.getString(R.string.time_format), Locale.GERMANY);
        return getReadableTime(timeFormat, timestamp);
    }

    protected static String getReadableTime(@NonNull SimpleDateFormat readableDateFormat, long timestamp) {
        return readableDateFormat.format(new Date(timestamp));
    }

    protected static String getReadableProvider(@NonNull Context context, @Nullable String provider) {
        if (TextUtils.isEmpty(provider)) {
            return context.getString(R.string.unknown);
        } else {
            return provider;
        }
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

    public String getProvider() {
        return provider;
    }

    public Bitmap getBarcode() {
        return barcode;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getResultTimestamp() {
        return resultTimestamp;
    }

    public String getDeleteButtonText() {
        return deleteButtonText;
    }

    public List<Pair<String, String>> getTopContent() {
        return topContent;
    }

    public List<Pair<String, String>> getCollapsedContent() {
        return collapsedContent;
    }

    protected void addTopContent(@NonNull String label, @NonNull String text) {
        topContent.add(new Pair(label, text));
    }

    protected void addCollapsedContent(@NonNull String label, @NonNull String text) {
        collapsedContent.add(new Pair(label, text));
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
