package de.culture4life.luca.testing;

public class TestResultParsingException extends Exception {

    public TestResultParsingException() {
    }

    public TestResultParsingException(String message) {
        super(message);
    }

    public TestResultParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestResultParsingException(Throwable cause) {
        super(cause);
    }

}
