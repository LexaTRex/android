package de.culture4life.luca.history;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CheckOutItem extends HistoryItem {

    @SerializedName("children")
    @Expose
    private List<String> children;

    public CheckOutItem() {
        super(HistoryItem.TYPE_CHECK_OUT);
    }

    public List<String> getChildren() {
        return children;
    }

    public void setChildren(List<String> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "CheckInItem{" +
                "children=" + children +
                "} " + super.toString();
    }

}
