package de.culture4life.luca.document;

public class DocumentParsingException extends Exception {

    public DocumentParsingException() {
        super("The document could not be parsed");
    }

    public DocumentParsingException(String message) {
        super(message);
    }

    public DocumentParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentParsingException(Throwable cause) {
        super(cause);
    }

}
