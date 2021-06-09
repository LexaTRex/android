package de.culture4life.luca.testing;

public class TestResultVerificationException extends TestResultImportException {

    public enum Reason {
        NAME_MISMATCH,
        INVALID_SIGNATURE,
        PROCEDURES_EMPTY,
        MIXED_TYPES_IN_PROCEDURES,
        OUTCOME_UNKNOWN,
        UNKNOWN
    }

    private final Reason reason;

    public TestResultVerificationException(Reason reason) {
        super(getDefaultMessage(reason));
        this.reason = reason;
    }

    public TestResultVerificationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public TestResultVerificationException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public TestResultVerificationException(Reason reason, Throwable cause) {
        super(getDefaultMessage(reason), cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    private static String getDefaultMessage(Reason reason) {
        switch (reason) {
            case NAME_MISMATCH:
                return "Name mismatch";
            case INVALID_SIGNATURE:
                return "Invalid signature";
            case PROCEDURES_EMPTY:
                return "Procedures size in baercode is empty";
            case MIXED_TYPES_IN_PROCEDURES:
                return "Vaccination and non-vaccination types are mixed in procedures";
            case OUTCOME_UNKNOWN:
                return "The outcome is unknown";
            case UNKNOWN:
            default:
                return "Unknown verification error";
        }
    }

}
