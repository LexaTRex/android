package de.culture4life.luca.testing;

public class TestResultVerificationException extends TestResultImportException {

    public enum Reason {
        NAME_MISMATCH,
        TEST_EXPIRED,
        INVALID_SIGNATURE,
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
            case TEST_EXPIRED:
                return "Test expired";
            case INVALID_SIGNATURE:
                return "Invalid signature";
            default:
                return "Unknown verification error";
        }
    }

}
