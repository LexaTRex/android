package de.culture4life.luca.ui.venue.children;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ChildListItem {

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("checked")
    @Expose
    private boolean checked;

    public ChildListItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void toggleIsChecked() {
        checked = !checked;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    public String toString() {
        return name;
    }

}
