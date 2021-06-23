package de.culture4life.luca.document;

public class DocumentImportException extends Exception {

    public DocumentImportException() {
        super("The document could not be imported");
    }

    public DocumentImportException(String message) {
        super(message);
    }

    public DocumentImportException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentImportException(Throwable cause) {
        super(cause);
    }

}
