package de.culture4life.luca.ui.registration;

import androidx.annotation.NonNull;

import java.util.Objects;

public class VerificationException extends Exception {

    public VerificationException(String message) {
        super(message);
    }

    public VerificationException(String message, Throwable cause) {
        super(message, cause);
    }

    @NonNull
    @Override
    public String getMessage() {
        return Objects.requireNonNull(super.getMessage());
    }

}
