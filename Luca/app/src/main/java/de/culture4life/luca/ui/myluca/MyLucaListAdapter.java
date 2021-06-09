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
import de.culture4life.luca.testing.TestResult;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
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

        CardView cardView = convertView.findViewById(R.id.cardView);
        TextView titleTextView = convertView.findViewById(R.id.itemTitleTextView);
        TextView descriptionTextView = convertView.findViewById(R.id.itemDescriptionTextView);
        TextView timeTextView = convertView.findViewById(R.id.itemTimeTextView);
        ImageView itemTitleImageView = convertView.findViewById(R.id.itemTitleImageView);
        ImageView barcodeImageView = convertView.findViewById(R.id.qrCodeImageView);
        Button deleteTestResultButton = convertView.findViewById(R.id.deleteItemButton);
        ViewGroup collapseLayout = convertView.findViewById(R.id.collapseLayout);
        TextView firstPropertyTextView = convertView.findViewById(R.id.firstPropertyTextView);
        TextView secondPropertyTextView = convertView.findViewById(R.id.secondPropertyTextView);
        TextView secondPropertyLabelTextView = convertView.findViewById(R.id.secondPropertyLabelTextView);

        cardView.setCardBackgroundColor(item.getColor());
        titleTextView.setText(item.getTitle());
        descriptionTextView.setText(item.getDescription());
        timeTextView.setText(item.getTime());
        itemTitleImageView.setVisibility(View.GONE);
        barcodeImageView.setImageBitmap(item.getBarcode());
        collapseLayout.setVisibility((item.isExpanded()) ? View.VISIBLE : View.GONE);
        convertView.setOnClickListener(v -> {
            item.toggleExpanded();
            notifyDataSetChanged();
        });

        deleteTestResultButton.setOnClickListener(v -> clickListener.onDelete(item));

        if (item instanceof TestResultItem) {
            TestResultItem testResultItem = (TestResultItem) item;
            firstPropertyTextView.setText(testResultItem.getTestResult().getLabName());
            if (testResultItem.testResult.getType() == TestResult.TYPE_APPOINTMENT) {
                secondPropertyTextView.setText(testResultItem.getTestResult().getFirstName());
            } else {
                secondPropertyTextView.setText(testResultItem.getTestResult().getLabDoctorName());
                secondPropertyLabelTextView.setVisibility(TextUtils.isEmpty(secondPropertyTextView.getText()) ? View.GONE : View.VISIBLE);
                setupTestResultProcedures(convertView, testResultItem);
            }
        } else if (item instanceof GreenPassItem) {
            GreenPassItem greenPassItem = (GreenPassItem) item;
            firstPropertyTextView.setText(greenPassItem.getTestResult().getLabName());
            secondPropertyLabelTextView.setVisibility(View.GONE);
            secondPropertyTextView.setVisibility(View.GONE);
            itemTitleImageView.setImageResource(item.getImageResource());
            itemTitleImageView.setVisibility(View.VISIBLE);
        }

        return convertView;
    }

    private void setupTestResultProcedures(View convertView, TestResultItem item) {
        LinearLayout proceduresContainer = convertView.findViewById(R.id.proceduresContainer);
        proceduresContainer.removeAllViews();
        if (item.getTestProcedures() == null || item.getTestProcedures().isEmpty()) {
            return;
        }

        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        for (TestResultItem.TestProcedure procedure : item.getTestProcedures()) {
            View procedureView = layoutInflater.inflate(R.layout.my_luca_vaccination_procedure, null);
            ((TextView) procedureView.findViewById(R.id.vaccination_name)).setText(procedure.getName());
            ((TextView) procedureView.findViewById(R.id.vaccination_date)).setText(procedure.getDate());
            proceduresContainer.addView(procedureView);
        }

    }

}
