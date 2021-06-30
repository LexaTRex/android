package de.culture4life.luca.network.pojo;

import java.util.ArrayList;
import java.util.Collection;

import androidx.annotation.NonNull;

public class DocumentProviderDataList extends ArrayList<DocumentProviderData> {

    public DocumentProviderDataList() {
    }

    public DocumentProviderDataList(@NonNull Collection<? extends DocumentProviderData> c) {
        super(c);
    }

}
