package de.culture4life.luca.network.pojo;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains a list of trace IDs that have been accessed by a given health department. The trace IDs
 * are hashed and truncated to 16 bytes requiring matching to occur locally.
 *
 * @see <a href="https://www.luca-app.de/securityoverview/processes/tracing_find_contacts.html#notifying-guests-about-data-access">Security
 * Overview: Notifying Guests about Data Access</a>
 */
public class AccessedHashedTraceIdsData {

    @SerializedName("healthDepartment")
    private HealthDepartment healthDepartment;

    @SerializedName("hashedTraceIds")
    private List<String> hashedTraceIds = new ArrayList<>();

    public HealthDepartment getHealthDepartment() {
        return healthDepartment;
    }

    public void setHealthDepartment(HealthDepartment healthDepartment) {
        this.healthDepartment = healthDepartment;
    }

    public List<String> getHashedTraceIds() {
        return hashedTraceIds;
    }

    public void setHashedTraceIds(List<String> hashedTraceIds) {
        this.hashedTraceIds = hashedTraceIds;
    }

    @Override
    public String toString() {
        return "AccessedHashedTraceIdsData{" +
                "healthDepartment=" + healthDepartment +
                ", hashedTraceIds=" + hashedTraceIds +
                '}';
    }

}
