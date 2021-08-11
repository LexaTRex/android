package de.culture4life.luca.ui.venue.children;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ChildListItemContainer extends ArrayList<ChildListItem> {

    public ChildListItemContainer() {
    }

    public ChildListItemContainer(@NonNull Collection<? extends ChildListItem> children) {
        super(children);
    }

    public List<String> getNames() {
        Iterator<ChildListItem> iterator = iterator();
        List<String> namesList = new ArrayList<>();
        while (iterator.hasNext()) {
            ChildListItem child = iterator.next();
            namesList.add(child.getName());
        }
        return namesList;
    }

}
