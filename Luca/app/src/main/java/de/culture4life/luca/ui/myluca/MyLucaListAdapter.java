package de.culture4life.luca.ui.myluca;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import de.culture4life.luca.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import timber.log.Timber;

public class MyLucaListAdapter extends ArrayAdapter<MyLucaListItem> {

    interface MyLucaListClickListener {

        void onDelete(@NonNull MyLucaListItem myLucaListItem);

    }

    private final MyLucaListClickListener clickListener;

    public MyLucaListAdapter(@NonNull Context context, int resource, MyLucaListClickListener listener) {
        super(context, resource);
        this.clickListener = listener;
    }

    public void setItems(@NonNull List<MyLucaListItem> items) {
        Timber.d("setItems() called with: items = [%s]", items);
        if (shouldUpdateDataSet(items)) {
            clear();
            addAll(items);
            notifyDataSetChanged();
        }
    }

    private boolean shouldUpdateDataSet(@NonNull List<MyLucaListItem> items) {
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
            convertView = layoutInflater.inflate(R.layout.my_luca_list_item, container, false);
        }

        MyLucaListItem item = getItem(position);

        ViewGroup topContent = convertView.findViewById(R.id.topContent);
        CardView cardView = convertView.findViewById(R.id.cardView);
        TextView titleTextView = convertView.findViewById(R.id.itemTitleTextView);
        ImageView itemTitleImageView = convertView.findViewById(R.id.itemTitleImageView);
        ImageView barcodeImageView = convertView.findViewById(R.id.qrCodeImageView);
        Button deleteTestResultButton = convertView.findViewById(R.id.deleteItemButton);
        ViewGroup collapseLayout = convertView.findViewById(R.id.collapseLayout);
        ViewGroup collapsedContent = convertView.findViewById(R.id.collapsedContent);

        cardView.setCardBackgroundColor(item.getColor());
        titleTextView.setText(item.getTitle());
        itemTitleImageView.setImageResource(item.getImageResource());
        barcodeImageView.setImageBitmap(item.getBarcode());
        collapseLayout.setVisibility((item.isExpanded()) ? View.VISIBLE : View.GONE);
        deleteTestResultButton.setText(item.getDeleteButtonText());

        setupDynamicContent(item.getTopContent(), topContent);
        setupDynamicContent(item.getCollapsedContent(), collapsedContent);

        convertView.setOnClickListener(v -> {
            item.toggleExpanded();
            notifyDataSetChanged();
        });
        deleteTestResultButton.setOnClickListener(v -> clickListener.onDelete(item));

        return convertView;
    }

    private void setupDynamicContent(List<Pair<String, String>> content, ViewGroup topContent) {
        for (int i = 0; i < Math.max(topContent.getChildCount(), content.size()); i++) {
            ConstraintLayout labelAndTextView = (ConstraintLayout) topContent.getChildAt(i);
            if (content.size() > i) {
                Pair<String, String> labelAndText = content.get(i);
                addLabelAndText(topContent, labelAndTextView, labelAndText.first, labelAndText.second);
            } else {
                topContent.removeView(labelAndTextView);
            }
        }
    }

    private void addLabelAndText(ViewGroup container, ConstraintLayout labelAndTextView, String label, String text) {
        if (labelAndTextView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            labelAndTextView = (ConstraintLayout) layoutInflater.inflate(R.layout.my_luca_vaccination_procedure, container, false);
            container.addView(labelAndTextView);
        }
        TextView labelView = labelAndTextView.findViewById(R.id.vaccination_name);
        TextView textView = labelAndTextView.findViewById(R.id.vaccination_date);
        labelView.setText(label);
        textView.setText(text);
        setConstrainWidth(labelAndTextView, R.id.vaccination_name, !TextUtils.isEmpty(text));
    }

    private void setConstrainWidth(ConstraintLayout constraintLayout, int viewId, boolean isConstrained) {
        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);
        set.constrainedWidth(viewId, isConstrained);
        set.applyTo(constraintLayout);
    }

}
