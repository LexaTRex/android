package de.culture4life.luca.document;

public class DocumentExpiredException extends DocumentImportException {

    public DocumentExpiredException() {
        super("The document has expired");
    }

    public DocumentExpiredException(String message) {
        super(message);
    }

    public DocumentExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentExpiredException(Throwable cause) {
        super(cause);
    }

}
