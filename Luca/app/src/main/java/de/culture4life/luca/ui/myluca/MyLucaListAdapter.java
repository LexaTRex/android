package de.culture4life.luca.ui.myluca;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.culture4life.luca.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

public class MyLucaListAdapter extends ArrayAdapter<MyLucaListItem> {

    interface MyLucaListClickListener {

        void onDelete(int position);

    }

    private final MyLucaListClickListener clickListener;

    public MyLucaListAdapter(@NonNull Context context, int resource, MyLucaListClickListener listener) {
        super(context, resource);
        this.clickListener = listener;
    }

    public void setHistoryItems(@NonNull List<MyLucaListItem> items) {
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

        CardView cardView = convertView.findViewById(R.id.cardView);
        TextView titleTextView = convertView.findViewById(R.id.itemTitleTextView);
        TextView descriptionTextView = convertView.findViewById(R.id.itemDescriptionTextView);
        TextView timeTextView = convertView.findViewById(R.id.itemTimeTextView);
        ImageView barcodeImageView = convertView.findViewById(R.id.qrCodeImageView);
        Button deleteTestResultButton = convertView.findViewById(R.id.deleteTestResultButton);
        ViewGroup collapseLayout = convertView.findViewById(R.id.collapseLayout);
        TextView testLabDoctorTitle = convertView.findViewById(R.id.testTypeHeader);
        TextView testLabDoctorName = convertView.findViewById(R.id.testLabDoctorName);
        TextView testLab = convertView.findViewById(R.id.testLab);

        cardView.setCardBackgroundColor(item.getColor());
        titleTextView.setText(item.getTitle());
        descriptionTextView.setText(item.getType());
        timeTextView.setText(item.getTime());
        barcodeImageView.setImageBitmap(item.getBarcode());
        collapseLayout.setVisibility((item.isExpanded()) ? View.VISIBLE : View.GONE);
        convertView.setOnClickListener(v -> {
            item.toggleExpanded();
            notifyDataSetChanged();
        });
        testLabDoctorName.setText(item.getTestLabDoctorName());
        testLabDoctorTitle.setVisibility(TextUtils.isEmpty(item.getTestLabDoctorName()) ? View.GONE : View.VISIBLE);

        testLab.setText(item.getDescription());
        deleteTestResultButton.setOnClickListener(v -> this.clickListener.onDelete(position));

        setupProcedures(convertView, item);

        return convertView;
    }

    private void setupProcedures(View convertView, MyLucaListItem item) {
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout proceduresContainer = convertView.findViewById(R.id.proceduresContainer);
        proceduresContainer.removeAllViews();
        if (item.getProcedures() != null) {
            for (MyLucaListItem.Procedure procedure : item.getProcedures()) {
                View procedureView = layoutInflater.inflate(R.layout.my_luca_vaccination_procedure, null);
                ((TextView) procedureView.findViewById(R.id.vaccination_name)).setText(procedure.getName());
                ((TextView) procedureView.findViewById(R.id.vaccination_date)).setText(procedure.getDate());
                proceduresContainer.addView(procedureView);
            }
        }
    }

}
