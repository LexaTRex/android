package de.culture4life.luca.testing;

public class TestResultExpiredException extends TestResultImportException {

    public TestResultExpiredException() {
        super("The test has expired");
    }

    public TestResultExpiredException(String message) {
        super(message);
    }

    public TestResultExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestResultExpiredException(Throwable cause) {
        super(cause);
    }

}
