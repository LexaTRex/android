package de.culture4life.luca.testing.provider.baercode;

import com.fasterxml.jackson.databind.JsonNode;

import androidx.annotation.NonNull;

/**
 * Procedure of a Baercode. Can be either a Covid test or a vaccination
 */
public class Procedure {

    public enum Type {
        ANTIGEN_FAST_TEST(1),
        PCR_TEST(2),
        VACCINATION_COMIRNATY(3),
        VACCINATION_JANNSEN(4),
        VACCINATION_MODERNA(5),
        VACCINATION_VAXZEVRIA(6);

        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Type from(int typeId) {
            for (Type type : values()) {
                if (type.value == typeId) {
                    return type;
                }
            }
            throw new IllegalArgumentException(String.format("Procedure type %d does not exist", typeId));
        }
    }


    private final Type type;
    private final long timestamp;

    public Procedure(@NonNull Type type, long timestamp) {
        this.type = type;
        this.timestamp = timestamp;
    }

    public static Procedure from(@NonNull JsonNode dataItems) {
        return new Procedure(
                Type.from(dataItems.get(0).asInt()),
                dataItems.get(1).asLong() * 1000
        );
    }

    /**
     * @return true if the procedure is a vaccination, false otherwise
     */
    public boolean isVaccination() {
        return type.value >= Type.VACCINATION_COMIRNATY.value;
    }

    /**
     * @return the required count of procedures to be considered as valid or covid-safe
     */
    public int getRequiredCount() {
        if (isVaccination()) {
            if (type != Type.VACCINATION_JANNSEN) {
                return 2;
            }
        }
        return 1;
    }

    public Type getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
