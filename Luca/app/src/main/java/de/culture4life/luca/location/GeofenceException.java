package de.culture4life.luca.location;

import androidx.annotation.Nullable;

import com.google.android.gms.location.GeofenceStatusCodes;

public class GeofenceException extends Exception {

    @Nullable
    private Integer errorCode;

    public GeofenceException(int errorCode) {
        super(GeofenceStatusCodes.getStatusCodeString(errorCode));
        this.errorCode = errorCode;
    }

    public GeofenceException(String message) {
        super(message);
    }

    public GeofenceException(String message, Throwable cause) {
        super(message, cause);
    }

    @Nullable
    public Integer getErrorCode() {
        return errorCode;
    }

}
