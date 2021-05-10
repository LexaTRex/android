package de.culture4life.luca.testing;

public class TestResultAlreadyImportedException extends TestResultImportException {

    public TestResultAlreadyImportedException() {
        super("A test result with this ID has already been imported");
    }

    public TestResultAlreadyImportedException(String message) {
        super(message);
    }

    public TestResultAlreadyImportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestResultAlreadyImportedException(Throwable cause) {
        super(cause);
    }

}
