package de.culture4life.luca.document;

public class DocumentAlreadyImportedException extends DocumentImportException {

    public DocumentAlreadyImportedException() {
        super("The document has already been imported");
    }

    public DocumentAlreadyImportedException(String message) {
        super(message);
    }

    public DocumentAlreadyImportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentAlreadyImportedException(Throwable cause) {
        super(cause);
    }

}
