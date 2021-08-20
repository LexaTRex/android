package de.culture4life.luca.ui.children;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Deprecated
public class LegacyChildListItem {

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("checked")
    @Expose
    private boolean checked;

    public LegacyChildListItem(String name) {
        this.name = name;
    }

    public LegacyChildListItem(String name, boolean checked) {
        this.name = name;
        this.checked = checked;
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
