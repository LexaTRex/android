package de.culture4life.luca.network.pojo;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

public class DocumentProviderDataList extends ArrayList<DocumentProviderData> {

    public DocumentProviderDataList() {
    }

    public DocumentProviderDataList(@NonNull Collection<? extends DocumentProviderData> c) {
        super(c);
    }

}
