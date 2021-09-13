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
import java.util.ArrayList;
import java.util.List;

import de.culture4life.luca.R;
import de.culture4life.luca.document.Document;
import io.reactivex.rxjava3.core.Single;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public abstract class MyLucaListItem implements Serializable {

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_TEST_RESULT = 1;
    public static final int TYPE_APPOINTMENT = 2;
    @Deprecated
    public static final int TYPE_GREEN_PASS = 3;

    @IntDef({TYPE_UNKNOWN, TYPE_TEST_RESULT, TYPE_APPOINTMENT, TYPE_GREEN_PASS})
    @Retention(SOURCE)
    public @interface Type {

    }

    @Type
    protected final int type;

    protected final Document document;

    protected final List<Pair<String, String>> topContent = new ArrayList<>();
    protected final List<Pair<String, String>> collapsedContent = new ArrayList<>();

    protected String sectionHeader;
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

    public MyLucaListItem(@Type int type, Document document) {
        this.type = type;
        this.document = document;
    }

    protected static Single<Bitmap> generateQrCode(@NonNull String data) {
        return Single.fromCallable(() -> QRCode.from(data)
                .withSize(500, 500)
                .withHint(EncodeHintType.MARGIN, 0)
                .bitmap());
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

    public Document getDocument() {
        return document;
    }

    public String getSectionHeader() {
        return sectionHeader;
    }

    public void setSectionHeader(String sectionHeader) {
        this.sectionHeader = sectionHeader;
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

    protected void addTopContent(@NonNull String label, @Nullable String text) {
        topContent.add(new Pair(label, text));
    }

    protected void addCollapsedContent(@NonNull String label, @Nullable String text) {
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
