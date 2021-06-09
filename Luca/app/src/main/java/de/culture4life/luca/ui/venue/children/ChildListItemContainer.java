package de.culture4life.luca.ui.venue.children;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;

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
