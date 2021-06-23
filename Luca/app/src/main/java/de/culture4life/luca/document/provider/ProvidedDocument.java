package de.culture4life.luca.document.provider;

import de.culture4life.luca.document.Document;

public abstract class ProvidedDocument {

    protected Document document;

    public ProvidedDocument() {
        this.document = new Document();
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

}
