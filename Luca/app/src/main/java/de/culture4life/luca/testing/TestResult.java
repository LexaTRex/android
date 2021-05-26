package de.culture4life.luca.testing;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import androidx.annotation.IntDef;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class TestResult {

    public static class Procedure {

        @Expose
        @SerializedName("name")
        private de.culture4life.luca.testing.provider.baercode.Procedure.Type name;

        @Expose
        @SerializedName("timestamp")
        private long timestamp;

        public Procedure(de.culture4life.luca.testing.provider.baercode.Procedure.Type name, long timestamp) {
            this.name = name;
            this.timestamp = timestamp;
        }

        public de.culture4life.luca.testing.provider.baercode.Procedure.Type getName() {
            return name;
        }

        public long getTimestamp() {
            return timestamp;
        }

    }

    public static final long MAXIMUM_FAST_TEST_VALIDITY = TimeUnit.DAYS.toMillis(2);
    public static final long MAXIMUM_PCR_TEST_VALIDITY = TimeUnit.DAYS.toMillis(3);
    public static final long TIME_UNTIL_VACCINATION_IS_VALID = TimeUnit.DAYS.toMillis(14);
    public static final long MAXIMUM_VACCINATION_VALIDITY = TimeUnit.DAYS.toMillis(365);

    @IntDef({TYPE_UNKNOWN, TYPE_FAST, TYPE_PCR, TYPE_VACCINATION})
    @Retention(SOURCE)
    public @interface Type {

    }

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_FAST = 1;
    public static final int TYPE_PCR = 2;
    public static final int TYPE_VACCINATION = 3;

    @IntDef({OUTCOME_UNKNOWN, OUTCOME_POSITIVE, OUTCOME_NEGATIVE, OUTCOME_PARTIALLY_VACCINATED, OUTCOME_FULLY_VACCINATED})
    @Retention(SOURCE)
    public @interface Outcome {

    }

    public static final int OUTCOME_UNKNOWN = 0;
    public static final int OUTCOME_POSITIVE = 1;
    public static final int OUTCOME_NEGATIVE = 2;
    public static final int OUTCOME_PARTIALLY_VACCINATED = 3;
    public static final int OUTCOME_FULLY_VACCINATED = 4;

    @Expose
    @SerializedName("id")
    private String id;

    @Type
    @Expose
    @SerializedName("type")
    private int type;

    @Outcome
    @Expose
    @SerializedName("outcome")
    private int outcome;

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

    public ArrayList<Procedure> getProcedures() {
        return procedures;
    }

    public void setProcedures(ArrayList<Procedure> procedures) {
        this.procedures = procedures;
    }

    /**
     * @return timestamp from which the test result becomes valid. This is immediately for tests and
     *         after two weeks for vaccinations.
     */
    public long getValidityStartTimestamp() {
        if (type == TYPE_VACCINATION && outcome == OUTCOME_FULLY_VACCINATED) {
            return getTestingTimestamp() + TIME_UNTIL_VACCINATION_IS_VALID;
        } else {
            return getTestingTimestamp();
        }
    }

    /**
     * The timestamp after which the test result should be treated as invalid. Depends on {@link
     * #type} and {@link #testingTimestamp}.
     */
    public long getExpirationTimestamp() {
        return getTestingTimestamp() + getExpirationDuration(type);
    }

    /**
     * The duration in milliseconds after which a test result with the specified {@link Type} should
     * be treated as invalid.
     */
    public static long getExpirationDuration(@Type int type) {
        switch (type) {
            case TYPE_FAST:
                return MAXIMUM_FAST_TEST_VALIDITY;
            case TYPE_PCR:
                return MAXIMUM_PCR_TEST_VALIDITY;
            case TYPE_VACCINATION:
                return MAXIMUM_VACCINATION_VALIDITY;
            default:
                return -1;
        }
    }

    @Override
    public String toString() {
        return "TestResult{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", outcome=" + outcome +
                ", testingTimestamp=" + testingTimestamp +
                ", resultTimestamp=" + resultTimestamp +
                ", importTimestamp=" + importTimestamp +
                ", labName='" + labName + '\'' +
                ", labDoctorName='" + labDoctorName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", encodedData='" + encodedData + '\'' +
                '}';
    }

}
