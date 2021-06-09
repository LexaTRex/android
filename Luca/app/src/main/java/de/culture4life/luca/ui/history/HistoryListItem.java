package de.culture4life.luca.ui.history;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

public class HistoryListItem {

    protected String title;
    protected String description;
    protected String additionalTitleDetails;
    protected String additionalDescriptionDetails;
    protected String time;
    protected long timestamp;
    @DrawableRes
    protected int titleIconResourceId;
    @DrawableRes
    protected int descriptionIconResourceId;

    public HistoryListItem(Context context) {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public String getAdditionalTitleDetails() {
        return additionalTitleDetails;
    }

    public void setAdditionalTitleDetails(@Nullable String additionalTitleDetails) {
        this.additionalTitleDetails = additionalTitleDetails;
    }

    @Nullable
    public String getAdditionalDescriptionDetails() {
        return additionalDescriptionDetails;
    }

    public void setAdditionalDescriptionDetails(@Nullable String additionalDescriptionDetails) {
        this.additionalDescriptionDetails = additionalDescriptionDetails;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @DrawableRes
    public int getTitleIconResourceId() {
        return titleIconResourceId;
    }

    public void setTitleIconResourceId(@DrawableRes int titleIconResourceId) {
        this.titleIconResourceId = titleIconResourceId;
    }

    @DrawableRes
    public int getDescriptionIconResourceId() {
        return descriptionIconResourceId;
    }

    public void setDescriptionIconResourceId(@DrawableRes int descriptionIconResourceId) {
        this.descriptionIconResourceId = descriptionIconResourceId;
    }

}
