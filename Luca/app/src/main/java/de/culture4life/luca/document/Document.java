package de.culture4life.luca.document;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import androidx.annotation.IntDef;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class Document {

    public static class Procedure {

        public enum Type {
            RAPID_ANTIGEN_TEST,
            PCR_TEST,
            VACCINATION_COMIRNATY,
            VACCINATION_JANNSEN,
            VACCINATION_MODERNA,
            VACCINATION_VAXZEVRIA,
            VACCINATION_SPUTNIK_V,
            RECOVERY,
            UNKNOWN;
        }

        @Expose
        @SerializedName("name")
        private Type type;

        @Expose
        @SerializedName("timestamp")
        private long timestamp;

        @Expose
        @SerializedName("totalSeriesOfDoses")
        private int totalSeriesOfDoses;

        @Expose
        @SerializedName("doseNumber")
        private int doseNumber;

        public Procedure(Type type, long timestamp, int doseNumber, int totalSeriesOfDoses) {
            this.type = type;
            this.timestamp = timestamp;
            this.doseNumber = doseNumber;
            this.totalSeriesOfDoses = totalSeriesOfDoses;
        }

        public Type getType() {
            return type;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getTotalSeriesOfDoses() {
            return totalSeriesOfDoses;
        }

        public int getDoseNumber() {
            return doseNumber;
        }

    }

    public static final long MAXIMUM_RECOVERY_VALIDITY = TimeUnit.DAYS.toMillis(30 * 6);
    public static final long MAXIMUM_APPOINTMENT_VALIDITY = TimeUnit.HOURS.toMillis(2);
    public static final long TIME_UNTIL_VACCINATION_IS_VALID = TimeUnit.DAYS.toMillis(15);
    public static final long MAXIMUM_VACCINATION_VALIDITY = TimeUnit.DAYS.toMillis(365);
    public static final long TIME_UNTIL_RECOVERY_IS_VALID = TimeUnit.DAYS.toMillis(15);
    public static final long MAXIMUM_FAST_TEST_VALIDITY = TimeUnit.DAYS.toMillis(2);
    public static final long MAXIMUM_NEGATIVE_PCR_TEST_VALIDITY = TimeUnit.DAYS.toMillis(3);
    public static final long MAXIMUM_POSITIVE_PCR_TEST_VALIDITY = MAXIMUM_RECOVERY_VALIDITY;

    @IntDef({TYPE_UNKNOWN, TYPE_FAST, TYPE_PCR, TYPE_VACCINATION, TYPE_APPOINTMENT, TYPE_GREEN_PASS, TYPE_RECOVERY})
    @Retention(SOURCE)
    public @interface Type {

    }

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_FAST = 1;
    public static final int TYPE_PCR = 2;
    public static final int TYPE_VACCINATION = 3;
    public static final int TYPE_APPOINTMENT = 4;
    @Deprecated
    public static final int TYPE_GREEN_PASS = 5;
    public static final int TYPE_RECOVERY = 6;

    @IntDef({OUTCOME_UNKNOWN, OUTCOME_POSITIVE, OUTCOME_NEGATIVE, OUTCOME_PARTIALLY_IMMUNE, OUTCOME_FULLY_IMMUNE})
    @Retention(SOURCE)
    public @interface Outcome {

    }

    public static final int OUTCOME_UNKNOWN = 0;
    public static final int OUTCOME_POSITIVE = 1;
    public static final int OUTCOME_NEGATIVE = 2;
    public static final int OUTCOME_PARTIALLY_IMMUNE = 3;
    public static final int OUTCOME_FULLY_IMMUNE = 4;

    @Expose
    @SerializedName("id")
    private String id;

    @Type
    @Expose
    @SerializedName("type")
    private int type = TYPE_UNKNOWN;

    @Outcome
    @Expose
    @SerializedName("outcome")
    private int outcome = OUTCOME_UNKNOWN;

    @Expose
    @SerializedName("testingTimestamp")
    private long testingTimestamp;

    @Expose
    @SerializedName("resultTimestamp")
    private long resultTimestamp;

    @Expose
    @SerializedName("importTimestamp")
    private long importTimestamp;

    @Expose
    @SerializedName("validityStartTimestamp")
    private long validityStartTimestamp;

    @Expose
    @SerializedName("expirationTimestamp")
    private long expirationTimestamp;

    @Expose
    @SerializedName("labName")
    private String labName;

    @Expose
    @SerializedName("labDoctorName")
    private String labDoctorName;

    @Expose
    @SerializedName("firstName")
    private String firstName;

    @Expose
    @SerializedName("lastName")
    private String lastName;

    @Expose
    @SerializedName("dateOfBirth")
    private long dateOfBirth;

    @Expose
    @SerializedName("provider")
    private String provider;

    @Expose
    @SerializedName("verified")
    private boolean verified = false;

    @Expose
    @SerializedName("encodedData")
    private String encodedData;

    @Expose
    @SerializedName("hashableEncodedData")
    private String hashableEncodedData;

    @Expose
    @SerializedName("procedures")
    private ArrayList<Procedure> procedures;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Type
    public int getType() {
        return type;
    }

    public void setType(@Type int type) {
        this.type = type;
    }

    @Outcome
    public int getOutcome() {
        return outcome;
    }

    public void setOutcome(@Outcome int outcome) {
        this.outcome = outcome;
    }

    public long getTestingTimestamp() {
        return testingTimestamp;
    }

    public void setTestingTimestamp(long testingTimestamp) {
        this.testingTimestamp = testingTimestamp;
    }

    public long getResultTimestamp() {
        return resultTimestamp;
    }

    public void setResultTimestamp(long resultTimestamp) {
        this.resultTimestamp = resultTimestamp;
    }

    public long getImportTimestamp() {
        return importTimestamp;
    }

    public void setImportTimestamp(long importTimestamp) {
        this.importTimestamp = importTimestamp;
    }

    public String getLabName() {
        return labName;
    }

    public void setLabName(String labName) {
        this.labName = labName;
    }

    public void setLabDoctorName(String labDoctorName) {
        this.labDoctorName = labDoctorName;
    }

    public String getLabDoctorName() {
        return labDoctorName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public long getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(long dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getEncodedData() {
        return encodedData;
    }

    public void setEncodedData(String encodedData) {
        this.encodedData = encodedData;
    }

    public String getHashableEncodedData() {
        return hashableEncodedData;
    }

    public void setHashableEncodedData(String hashableEncodedData) {
        this.hashableEncodedData = hashableEncodedData;
    }

    public void setValidityStartTimestamp(long validityStartTimestamp) {
        this.validityStartTimestamp = validityStartTimestamp;
    }

    public void setExpirationTimestamp(long expirationTimestamp) {
        this.expirationTimestamp = expirationTimestamp;
    }

    public ArrayList<Procedure> getProcedures() {
        return procedures;
    }

    public void setProcedures(ArrayList<Procedure> procedures) {
        this.procedures = procedures;
    }

    /**
     * @return timestamp from which the document becomes valid. This is immediately for tests and
     *         after two weeks for vaccinations.
     */
    public long getValidityStartTimestamp() {
        if (validityStartTimestamp != 0) {
            return validityStartTimestamp;
        } else if (type == TYPE_VACCINATION && outcome == OUTCOME_FULLY_IMMUNE) {
            return getTestingTimestamp() + TIME_UNTIL_VACCINATION_IS_VALID;
        } else if (type == TYPE_RECOVERY && outcome == OUTCOME_FULLY_IMMUNE) {
            return getTestingTimestamp() + TIME_UNTIL_RECOVERY_IS_VALID;
        } else if (type == TYPE_PCR && outcome == OUTCOME_POSITIVE) {
            return getTestingTimestamp() + TIME_UNTIL_RECOVERY_IS_VALID;
        } else {
            return getTestingTimestamp();
        }
    }

    /**
     * The timestamp after which the document should be treated as invalid. Depends on {@link #type}
     * and {@link #testingTimestamp} or the values set directly from a certificate.
     */
    public long getExpirationTimestamp() {
        if (expirationTimestamp != 0) {
            return expirationTimestamp;
        }
        return getTestingTimestamp() + getExpirationDuration(type);
    }

    /**
     * The duration in milliseconds after which a document with the specified {@link Type} should be
     * treated as invalid.
     */
    public long getExpirationDuration(@Type int type) {
        switch (type) {
            case TYPE_FAST:
                return MAXIMUM_FAST_TEST_VALIDITY;
            case TYPE_PCR:
                if (outcome == OUTCOME_POSITIVE) {
                    return MAXIMUM_POSITIVE_PCR_TEST_VALIDITY;
                } else {
                    return MAXIMUM_NEGATIVE_PCR_TEST_VALIDITY;
                }
            case TYPE_VACCINATION:
                return MAXIMUM_VACCINATION_VALIDITY;
            case TYPE_RECOVERY:
                return MAXIMUM_RECOVERY_VALIDITY;
            case TYPE_APPOINTMENT:
                return MAXIMUM_APPOINTMENT_VALIDITY;
            default:
                return -1;
        }
    }

    /**
     * @return true if this document is a valid recovery certificate. This is the case when it is a
     *  positive PCR test older than 14 days but no more than 6 months.
     */
    public boolean isValidRecovery() {
        if (type == TYPE_RECOVERY || (type == TYPE_PCR && outcome == OUTCOME_POSITIVE)) {
            long now = System.currentTimeMillis();
            return now > getValidityStartTimestamp() && now < getExpirationTimestamp();
        }
        return false;
    }

    @Override
    public String toString() {
        return "Document{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", outcome=" + outcome +
                ", testingTimestamp=" + testingTimestamp +
                ", resultTimestamp=" + resultTimestamp +
                ", importTimestamp=" + importTimestamp +
                ", validityStartTimestamp=" + validityStartTimestamp +
                ", expirationTimestamp=" + expirationTimestamp +
                ", labName='" + labName + '\'' +
                ", labDoctorName='" + labDoctorName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", provider=" + provider +
                ", encodedData='" + encodedData + '\'' +
                ", hashableEncodedData='" + hashableEncodedData + '\'' +
                ", procedures=" + procedures +
                '}';
    }

}
