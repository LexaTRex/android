package de.culture4life.luca.network.pojo;

import com.google.gson.annotations.SerializedName;

import de.culture4life.luca.util.SerializationUtil;
import de.culture4life.luca.util.TimeUtil;

/**
 * Check-out containing only a trace ID and a timestamp, indicating the time a Check-in associated
 * with the trace ID has ended.
 *
 * @see <a href="https://www.luca-app.de/securityoverview/processes/guest_checkout.html#checkout-process">Security
 * Overview: Guest Checkout</a>
 */
public class CheckOutRequestData {

    @SerializedName("traceId")
    private String traceId;

    @SerializedName("timestamp")
    private long roundedUnixTimestamp;

    public CheckOutRequestData() {
    }

    public CheckOutRequestData(byte[] traceId) {
        this.traceId = SerializationUtil.serializeToBase64(traceId).blockingGet();
        this.roundedUnixTimestamp = TimeUtil.getCurrentUnixTimestamp()
                .flatMap(TimeUtil::roundUnixTimestampDownToMinute)
                .blockingGet();
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public long getRoundedUnixTimestamp() {
        return roundedUnixTimestamp;
    }

    public void setRoundedUnixTimestamp(long roundedUnixTimestamp) {
        this.roundedUnixTimestamp = roundedUnixTimestamp;
    }

    @Override
    public String toString() {
        return "CheckOutData{" +
                "traceId='" + traceId + '\'' +
                ", roundedUnixTimestamp=" + roundedUnixTimestamp +
                '}';
    }

}
