package de.culture4life.luca.dataaccess;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import de.culture4life.luca.network.pojo.NotifyingHealthDepartment;

/**
 * Model storing recent trace IDs and their computed {@link #hashedTraceId}, allowing the Guest app
 * to match them to hashed trace IDs accessed by a health department. This allows users to be
 * notified in case data has been accessed by a health department.
 *
 * @see DataAccessManager#fetchRecentlyAccessedTraceData()
 * @see <a href="https://www.luca-app.de/securityoverview/processes/tracing_find_contacts.html#notifying-guests-about-data-access">Security
 * Overview: Notifying Guests about Data Access</a>
 */
public class AccessedTraceData {

    /**
     * The number of available warning levels, starting from 1
     */
    public static final int NUMBER_OF_WARNING_LEVELS = 4;

    @Expose
    @SerializedName("hashedTracingId")
    private String hashedTraceId;

    @Expose
    @SerializedName("tracingId")
    private String traceId;

    @Expose
    @SerializedName("warningLevel")
    private int warningLevel;

    @Expose
    @SerializedName("locationName")
    private String locationName;

    @Expose
    @SerializedName("healthDepartment")
    private NotifyingHealthDepartment healthDepartment;

    @Expose
    @SerializedName("accessTimestamp")
    private long accessTimestamp;

    @Expose
    @SerializedName("checkInTimestamp")
    private long checkInTimestamp;

    @Expose
    @SerializedName("checkOutTimestamp")
    private long checkOutTimestamp;

    @Expose
    @SerializedName("isNew")
    private boolean isNew;

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

    public int getWarningLevel() {
        return warningLevel;
    }

    public void setWarningLevel(int warningLevel) {
        this.warningLevel = warningLevel;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public NotifyingHealthDepartment getHealthDepartment() {
        return healthDepartment;
    }

    public void setHealthDepartment(NotifyingHealthDepartment healthDepartment) {
        this.healthDepartment = healthDepartment;
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

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    public boolean getIsNew() {
        return isNew;
    }

    @Override
    public String toString() {
        return "AccessedTraceData{" +
                "hashedTraceId='" + hashedTraceId + '\'' +
                ", traceId='" + traceId + '\'' +
                ", warningLevel=" + warningLevel +
                ", locationName='" + locationName + '\'' +
                ", healthDepartment=" + healthDepartment +
                ", accessTimestamp=" + accessTimestamp +
                ", checkInTimestamp=" + checkInTimestamp +
                ", checkOutTimestamp=" + checkOutTimestamp +
                ", isNew=" + isNew +
                '}';
    }

}
