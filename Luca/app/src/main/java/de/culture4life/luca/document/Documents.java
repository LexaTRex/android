package de.culture4life.luca.document;

import java.util.ArrayList;
import java.util.Collection;

import androidx.annotation.NonNull;

public class Documents extends ArrayList<Document> {

    public Documents() {
    }

    public Documents(@NonNull Collection<? extends Document> documents) {
        super(documents);
    }

}
