package de.culture4life.luca.document;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

public class Documents extends ArrayList<Document> {

    public Documents() {
    }

    public Documents(@NonNull Collection<? extends Document> documents) {
        super(documents);
    }

}
