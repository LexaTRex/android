package de.culture4life.luca.ui.myluca;

import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import de.culture4life.luca.R;
import de.culture4life.luca.ui.UiUtil;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

public class MyLucaListAdapter extends RecyclerView.Adapter<MyLucaListAdapter.MyLucaViewHolder> {

    interface MyLucaListClickListener {

        void onDelete(@NonNull MyLucaListItem myLucaListItem);

    }

    class MyLucaViewHolder extends RecyclerView.ViewHolder {

        ViewGroup topContent;
        CardView cardView;
        TextView titleTextView;
        ImageView itemTitleImageView;
        ImageView barcodeImageView;
        TextView providerTextView;
        Button deleteItemButton;
        ViewGroup collapseLayout;
        ViewGroup collapsedContent;

        public MyLucaViewHolder(@NonNull ViewGroup itemView) {
            super(itemView);
            topContent = itemView.findViewById(R.id.topContent);
            cardView = itemView.findViewById(R.id.cardView);
            titleTextView = itemView.findViewById(R.id.itemTitleTextView);
            itemTitleImageView = itemView.findViewById(R.id.itemTitleImageView);
            barcodeImageView = itemView.findViewById(R.id.qrCodeImageView);
            providerTextView = itemView.findViewById(R.id.providerTextView);
            deleteItemButton = itemView.findViewById(R.id.deleteItemButton);
            collapseLayout = itemView.findViewById(R.id.collapseLayout);
            collapsedContent = itemView.findViewById(R.id.collapsedContent);
        }

    }

    private final MyLucaListClickListener clickListener;

    private List<MyLucaListItem> items = new ArrayList<>();


    public MyLucaListAdapter(MyLucaListClickListener listener) {
        this.clickListener = listener;
    }

    @Override
    public MyLucaListAdapter.MyLucaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.my_luca_list_item, parent, false);
        return new MyLucaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyLucaListAdapter.MyLucaViewHolder holder, int position) {
        MyLucaListItem item = getItem(position);

        int topPadding = position == 0 ? (int) UiUtil.convertDpToPixel(8, holder.itemView.getContext()) : 0;
        holder.itemView.setPadding(holder.itemView.getPaddingLeft(), topPadding, holder.itemView.getPaddingRight(), holder.itemView.getPaddingBottom());

        holder.cardView.setCardBackgroundColor(item.getColor());
        holder.titleTextView.setText(item.getTitle());
        holder.itemTitleImageView.setImageResource(item.getImageResource());
        holder.barcodeImageView.setImageBitmap(item.getBarcode());
        holder.providerTextView.setText(item.getProvider());
        holder.providerTextView.setVisibility(TextUtils.isEmpty(item.getProvider()) ? View.GONE : View.VISIBLE);
        holder.collapseLayout.setVisibility((item.isExpanded()) ? View.VISIBLE : View.GONE);
        holder.deleteItemButton.setText(item.getDeleteButtonText());

        setupDynamicContent(item.getTopContent(), holder.topContent);
        setupDynamicContent(item.getCollapsedContent(), holder.collapsedContent);

        holder.itemView.setOnClickListener(v -> {
            item.toggleExpanded();
            notifyItemChanged(position);
        });
        holder.deleteItemButton.setOnClickListener(v -> clickListener.onDelete(item));
    }

    private MyLucaListItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(@NonNull List<MyLucaListItem> items) {
        this.items.clear();
        this.items.addAll(items);
        notifyDataSetChanged();
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
            LayoutInflater layoutInflater = LayoutInflater.from(container.getContext());
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
