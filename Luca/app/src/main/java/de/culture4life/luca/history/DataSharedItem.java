package de.culture4life.luca.history;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.concurrent.TimeUnit;

public class DataSharedItem extends HistoryItem {

    @SerializedName("days")
    @Expose
    private int days = (int) TimeUnit.MILLISECONDS.toDays(HistoryManager.SHARE_DATA_DURATION);

    public DataSharedItem() {
        super(HistoryItem.TYPE_CONTACT_DATA_REQUEST);
    }

    public DataSharedItem(@NonNull String tan, int days) {
        this();
        this.relatedId = tan;
        this.days = days;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    @Override
    public String toString() {
        return "DataSharedItem{" +
                "days=" + days +
                "} " + super.toString();
    }

}
