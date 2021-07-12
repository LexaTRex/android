package de.culture4life.luca.ui.myluca;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.culture4life.luca.R;
import de.culture4life.luca.ui.myluca.viewholders.MultipleMyLucaItemViewHolder;
import de.culture4life.luca.ui.myluca.viewholders.SingleMyLucaItemViewHolder;

public class MyLucaListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface MyLucaListClickListener {

        void onDelete(@NonNull MyLucaListItem myLucaListItem);

    }

    public interface MyLucaListItemExpandListener {

        void onExpand();

    }

    public static final int SINGLE_ITEM_VIEW_HOLDER = 0;
    public static final int MULTIPLE_ITEM_VIEW_HOLDER = 1;

    private final MyLucaListClickListener clickListener;
    private final Fragment fragment;

    private final List<MyLucaListItemsWrapper> items = new ArrayList<>();
    private final Map<Integer, Integer> viewPagerPositionMap = new HashMap<Integer, Integer>();

    public MyLucaListAdapter(MyLucaListClickListener listener, Fragment fragment) {
        this.clickListener = listener;
        this.fragment = fragment;
    }

    @Override
    public RecyclerView.@NotNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == SINGLE_ITEM_VIEW_HOLDER) {
            ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.my_luca_list_item_container, parent, false);
            return new SingleMyLucaItemViewHolder(view);
        } else {
            ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.my_luca_list_items_viewpager, parent, false);
            return new MultipleMyLucaItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        List<MyLucaListItem> items = getItem(position).getItems();
        if (items.isEmpty()) return;

        if (viewHolder.getItemViewType() == SINGLE_ITEM_VIEW_HOLDER) {
            SingleMyLucaItemViewHolder holder = (SingleMyLucaItemViewHolder) viewHolder;
            MyLucaListItem item = items.get(0);

            holder.getConstraintLayoutContainer().removeAllViews();
            View.OnClickListener expandClickListener = (v -> {
                item.toggleExpanded();
                notifyItemChanged(position);
            });
            View.OnClickListener deleteClickListener = (v -> clickListener.onDelete(item));

            SingleLucaItemView singleLucaItemView = new SingleLucaItemView(holder.itemView.getContext(), null, 0, item);
            holder.getConstraintLayoutContainer().addView(singleLucaItemView);
            singleLucaItemView.setListeners(expandClickListener, deleteClickListener);
        } else {
            MyLucaListItemExpandListener expandClickListener = new MyLucaListItemExpandListener() {
                @Override
                public void onExpand() {
                    for (int i = 0; i < items.size(); i++) {
                        MyLucaListItem item = items.get(i);
                        item.toggleExpanded();
                    }
                    notifyItemChanged(position);
                }

            };
            Integer hashCode = items.hashCode();
            MyLucaItemViewPager viewPagerAdapter = new MyLucaItemViewPager(this.fragment, items, expandClickListener, clickListener, position);
            MultipleMyLucaItemViewHolder multipleHolder = (MultipleMyLucaItemViewHolder) viewHolder;
            multipleHolder.getViewPager().setAdapter(viewPagerAdapter);
            ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int positionInViewPager) {
                    multipleHolder.getPageIndicator().setSelected(positionInViewPager);
                    viewPagerPositionMap.put(hashCode, positionInViewPager);
                    super.onPageSelected(position);
                }
            };
            multipleHolder.getViewPager().unregisterOnPageChangeCallback(pageChangeCallback);
            multipleHolder.getViewPager().registerOnPageChangeCallback(pageChangeCallback);
            int viewPagerStarPos = viewPagerPositionMap.containsKey(hashCode) ? viewPagerPositionMap.get(hashCode) : items.size() - 1;
            multipleHolder.getViewPager().setCurrentItem(viewPagerStarPos, false);
            multipleHolder.getPageIndicator().setCount(items.size());
        }
    }

    private MyLucaListItemsWrapper getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(@NonNull List<MyLucaListItem> items) {
        this.items.clear();
        List<MyLucaListItemsWrapper> sortedList = sortAndPairItems(items);
        this.items.addAll(sortedList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        MyLucaListItemsWrapper item = this.items.get(position);
        if (item.hasMultipleItems()) {
            return MULTIPLE_ITEM_VIEW_HOLDER;
        } else {
            return SINGLE_ITEM_VIEW_HOLDER;
        }
    }

    private static List<MyLucaListItemsWrapper> sortAndPairItems(List<MyLucaListItem> list) {
        List<MyLucaListItem> vaccinationItems = new ArrayList<>();
        List<MyLucaListItemsWrapper> sortedList = new ArrayList<MyLucaListItemsWrapper>();
        for (int i = 0; i < list.size(); i++) {
            MyLucaListItem currentListItem = list.get(i);
            if (currentListItem.getClass().equals(VaccinationItem.class)) {
                vaccinationItems.add(currentListItem);
            } else {
                sortedList.add(new MyLucaListItemsWrapper(currentListItem));
            }
        }
        if (!vaccinationItems.isEmpty())
            sortedList.add(new MyLucaListItemsWrapper(vaccinationItems));

        Collections.sort(sortedList, (first, second) -> Long.compare(second.getTimeStamp(), first.getTimeStamp()));
        return sortedList;
    }
}