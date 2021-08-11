package de.culture4life.luca.crypto;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Arrays;

import de.culture4life.luca.util.SerializationUtil;

/**
 * An opaque identifier derived from a Guest’s user ID and tracing secret during Guest Check-In. It
 * is used to identify Check-Ins by an Infected Guest after that Guest shared their tracing secret
 * with the Health Department.
 */
public class TraceIdWrapper {

    private long timestamp;
    private byte[] traceId;

    public TraceIdWrapper() {
    }

    public TraceIdWrapper(long timestamp, byte[] traceId) {
        this.timestamp = timestamp;
        this.traceId = traceId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getTraceId() {
        return traceId;
    }

    public void setTraceId(byte[] traceId) {
        this.traceId = traceId;
    }

    @Override
    public String toString() {
        return "TraceIdWrapper{" +
                "timestamp=" + timestamp +
                ", traceId=" + Arrays.toString(traceId) +
                '}';
    }

    public static class TypeAdapter implements JsonSerializer<TraceIdWrapper>, JsonDeserializer<TraceIdWrapper> {

        @Override
        public JsonElement serialize(TraceIdWrapper traceIdWrapper, Type type, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("ts", context.serialize(traceIdWrapper.getTimestamp()));
            jsonObject.add("id", context.serialize(SerializationUtil.serializeToBase64(traceIdWrapper.traceId).blockingGet()));
            return jsonObject;
        }

        @Override
        public TraceIdWrapper deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            long timestamp = jsonObject.get("ts").getAsLong();
            String encodedTraceId = jsonObject.get("id").getAsString();
            byte[] traceId = SerializationUtil.deserializeFromBase64(encodedTraceId).blockingGet();
            return new TraceIdWrapper(timestamp, traceId);
        }

    }

}
