package de.culture4life.luca.dataaccess;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Model storing recent trace IDs and their computed {@link #hashedTraceId}, allowing the Guest app
 * to match them to hashed trace IDs accessed by a health department. This allows users to be
 * notified in case data has been accessed by a health department.
 *
 * @see DataAccessManager#fetchRecentlyAccessedTraceData()
 * @see <a href="https://www.luca-app.de/securityoverview/processes/tracing_find_contacts.html#notifying-guests-about-data-access">Security
 *         Overview: Notifying Guests about Data Access</a>
 */
public class AccessedTraceData {

    @Expose
    @SerializedName("hashedTracingId")
    private String hashedTraceId;

    @Expose
    @SerializedName("tracingId")
    private String traceId;

    @Expose
    @SerializedName("locationName")
    private String locationName;

    @Expose
    @SerializedName("healthDepartmentId")
    private String healthDepartmentId;

    @Expose
    @SerializedName("healthDepartmentName")
    private String healthDepartmentName;

    @Expose
    @SerializedName("accessTimestamp")
    private long accessTimestamp;

    @Expose
    @SerializedName("checkInTimestamp")
    private long checkInTimestamp;

    @Expose
    @SerializedName("checkOutTimestamp")
    private long checkOutTimestamp;

    public String getHashedTraceId() {
        return hashedTraceId;
    }

    public void setHashedTraceId(String hashedTraceId) {
        this.hashedTraceId = hashedTraceId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getHealthDepartmentId() {
        return healthDepartmentId;
    }

    public void setHealthDepartmentId(String healthDepartmentId) {
        this.healthDepartmentId = healthDepartmentId;
    }

    public String getHealthDepartmentName() {
        return healthDepartmentName;
    }

    public void setHealthDepartmentName(String healthDepartmentName) {
        this.healthDepartmentName = healthDepartmentName;
    }

    public long getAccessTimestamp() {
        return accessTimestamp;
    }

    public void setAccessTimestamp(long accessTimestamp) {
        this.accessTimestamp = accessTimestamp;
    }

    public long getCheckInTimestamp() {
        return checkInTimestamp;
    }

    public void setCheckInTimestamp(long checkInTimestamp) {
        this.checkInTimestamp = checkInTimestamp;
    }

    public long getCheckOutTimestamp() {
        return checkOutTimestamp;
    }

    public void setCheckOutTimestamp(long checkOutTimestamp) {
        this.checkOutTimestamp = checkOutTimestamp;
    }

    @Override
    public String toString() {
        return "AccessedTraceData{" +
                "hashedTraceId='" + hashedTraceId + '\'' +
                ", traceId='" + traceId + '\'' +
                ", locationName='" + locationName + '\'' +
                ", healthDepartmentId='" + healthDepartmentId + '\'' +
                ", healthDepartmentName='" + healthDepartmentName + '\'' +
                ", accessTimestamp=" + accessTimestamp +
                ", checkInTimestamp=" + checkInTimestamp +
                ", checkOutTimestamp=" + checkOutTimestamp +
                '}';
    }

}
