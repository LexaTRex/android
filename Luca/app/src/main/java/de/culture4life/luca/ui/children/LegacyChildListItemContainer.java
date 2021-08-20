package de.culture4life.luca.ui.children;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.culture4life.luca.children.Child;
import de.culture4life.luca.children.Children;

@Deprecated
public class LegacyChildListItemContainer extends ArrayList<LegacyChildListItem> {

    public LegacyChildListItemContainer() {
    }

    public LegacyChildListItemContainer(@NonNull Collection<? extends LegacyChildListItem> children) {
        super(children);
    }

    public List<String> getNames() {
        Iterator<LegacyChildListItem> iterator = iterator();
        List<String> namesList = new ArrayList<>();
        while (iterator.hasNext()) {
            LegacyChildListItem child = iterator.next();
            namesList.add(child.getName());
        }
        return namesList;
    }

    public Children toChildren(String lastName) {
        Children children = new Children();
        for (LegacyChildListItem item : this) {
            children.add(Child.Companion.from(item.getName(), lastName));
        }
        return children;
    }

}
