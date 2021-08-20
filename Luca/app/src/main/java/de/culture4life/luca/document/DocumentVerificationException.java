package de.culture4life.luca.document;

public class DocumentVerificationException extends DocumentImportException {

    public enum Reason {
        NAME_MISMATCH,
        INVALID_SIGNATURE,
        PROCEDURES_EMPTY,
        MIXED_TYPES_IN_PROCEDURES,
        DATE_OF_BIRTH_TOO_OLD_FOR_CHILD,
        OUTCOME_UNKNOWN,
        TIMESTAMP_IN_FUTURE,
        UNKNOWN
    }

    private final Reason reason;

    public DocumentVerificationException(Reason reason) {
        super(getDefaultMessage(reason));
        this.reason = reason;
    }

    public DocumentVerificationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public DocumentVerificationException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public DocumentVerificationException(Reason reason, Throwable cause) {
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
            case DATE_OF_BIRTH_TOO_OLD_FOR_CHILD:
                return "The date of birth is too old for being a child";
            case OUTCOME_UNKNOWN:
                return "The outcome is unknown";
            case TIMESTAMP_IN_FUTURE:
                return "The timestamp of this document is in the future";
            case UNKNOWN:
            default:
                return "Unknown verification error";
        }
    }

}
