package de.culture4life.luca.dataaccess;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * List of {@link AccessedTraceData} used for matching local trace IDs to the list of accessed
 * hashed trace IDs by a given health department.
 *
 * @see <a href="https://www.luca-app.de/securityoverview/processes/tracing_find_contacts.html#notifying-guests-about-data-access">Security
 * Overview: Notifying Guests about Data Access</a>
 */
public class AccessedData {

    @Expose
    @SerializedName("tracingData")
    private List<AccessedTraceData> traceData = new ArrayList<>();

    public List<AccessedTraceData> getTraceData() {
        return traceData;
    }

    public void setTraceData(List<AccessedTraceData> traceData) {
        this.traceData = traceData;
    }

    public void addData(List<AccessedTraceData> traceIds) {
        this.traceData.addAll(traceIds);
    }

    @Override
    public String toString() {
        return "AccessedData{" +
                "traceData=" + traceData +
                '}';
    }

}
