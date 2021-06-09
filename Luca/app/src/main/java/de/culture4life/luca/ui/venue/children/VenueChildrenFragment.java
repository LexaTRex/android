package de.culture4life.luca.ui.venue.children;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import de.culture4life.luca.R;
import de.culture4life.luca.ui.BaseFragment;
import de.culture4life.luca.ui.UiUtil;
import de.culture4life.luca.ui.dialog.BaseDialogFragment;

import io.reactivex.rxjava3.core.Completable;

public class VenueChildrenFragment extends BaseFragment<VenueChildrenViewModel> {

    private ImageView backImageView;
    private TextView childAddingDescriptionTextView;
    private ChildListAdapter childListAdapter;
    private ListView childListView;
    private MaterialButton addChildButton;

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_adding_children;
    }

    @Override
    protected Class<VenueChildrenViewModel> getViewModelClass() {
        return VenueChildrenViewModel.class;
    }

    @Override
    protected Completable initializeViews() {
        return super.initializeViews()
                .andThen(Completable.fromAction(() -> {
                    initializeChildItemsViews();
                    initializeAddChildViews();
                }));
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.restoreChildren().subscribe();
    }

    private void initializeAddChildViews() {
        backImageView = getView().findViewById(R.id.back);
        backImageView.setOnClickListener(view -> viewModel.openVenueDetailsView());
        addChildButton = getView().findViewById(R.id.primaryActionButton);
        addChildButton.setOnClickListener(view -> showAddChildDialog());
    }

    private void initializeChildItemsViews() {
        childAddingDescriptionTextView = getView().findViewById(R.id.childAddingDescriptionTextView);
        childListView = getView().findViewById(R.id.childListView);

        childListAdapter = new ChildListAdapter(getContext(), childListView.getId(), viewModel);
        childListView.setAdapter(childListAdapter);

        childListView = getView().findViewById(R.id.childListView);
        View paddingView = new View(getContext());
        paddingView.setMinimumHeight((int) UiUtil.convertDpToPixel(16, getContext()));
        childListView.addHeaderView(paddingView);

        observe(viewModel.getChildren(), this::updateChildItemsList);
    }

    private void showAddChildDialog() {
        ViewGroup viewGroup = getActivity().findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.child_name_dialog, viewGroup, false);

        new BaseDialogFragment(new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.venue_children_title)
                .setMessage(R.string.venue_children_add_description)
                .setView(dialogView)
                .setPositiveButton(R.string.action_add, (dialog, i) -> {
                    TextInputLayout childNameTextInputLayout = dialogView.findViewById(R.id.childNameTextInputLayout);
                    String childName = childNameTextInputLayout.getEditText().getText().toString();
                    viewModel.addChild(childName).subscribe();
                })
                .setNegativeButton(R.string.action_cancel, (dialog, i) -> dialog.cancel()))
                .show();
    }

    private void updateChildItemsList(ChildListItemContainer children) {
        if (children.isEmpty()) {
            childAddingDescriptionTextView.setText(R.string.venue_children_empty_list_description);
            childListAdapter.setChildItems(children);
            childListView.setVisibility(View.GONE);
        } else {
            childAddingDescriptionTextView.setText(R.string.venue_children_list_description);
            childListView.setVisibility(View.VISIBLE);
            childListAdapter.setChildItems(children);
        }
    }

}
