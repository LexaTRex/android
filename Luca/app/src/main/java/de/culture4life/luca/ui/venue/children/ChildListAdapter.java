package de.culture4life.luca.ui.venue.children;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import de.culture4life.luca.R;

import java.util.List;

import androidx.annotation.NonNull;

public class ChildListAdapter extends ArrayAdapter<ChildListItem> {

    private final VenueChildrenViewModel viewModel;

    public ChildListAdapter(@NonNull Context context, int resource, VenueChildrenViewModel viewModel) {
        super(context, resource);
        this.viewModel = viewModel;
    }

    public void setChildItems(@NonNull List<ChildListItem> items) {
        if (shouldUpdateDataSet(items)) {
            clear();
            addAll(items);
            notifyDataSetChanged();
        }
    }

    private boolean shouldUpdateDataSet(@NonNull List<ChildListItem> items) {
        if (items.size() != getCount()) {
            return true;
        }
        for (int itemIndex = 0; itemIndex < getCount(); itemIndex++) {
            if (!items.contains(getItem(itemIndex))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.child_list_item, container, false);
        }

        ChildListItem childItem = getItem(position);

        CheckBox checkBox = convertView.findViewById(R.id.includeChildCheckBox);
        checkBox.setChecked(childItem.isChecked());

        checkBox.setOnClickListener(view -> {
            childItem.toggleIsChecked();
            viewModel.persistChildrenAsSideEffect();
        });

        TextView nameTextView = convertView.findViewById(R.id.childNameTextView);
        nameTextView.setText(childItem.getName());

        ImageView removeChildImageView = convertView.findViewById(R.id.removeChildImageView);
        removeChildImageView.setOnClickListener(view -> viewModel.removeChild(childItem)
                .onErrorComplete()
                .subscribe());

        return convertView;
    }

}
