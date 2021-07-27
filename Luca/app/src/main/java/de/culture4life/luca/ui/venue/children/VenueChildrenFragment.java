package de.culture4life.luca.ui.venue.children;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import de.culture4life.luca.R;
import de.culture4life.luca.ui.BaseFragment;
import de.culture4life.luca.ui.UiUtil;
import de.culture4life.luca.ui.dialog.BaseDialogFragment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

        BaseDialogFragment baseDialogFragment = new BaseDialogFragment(new MaterialAlertDialogBuilder(getContext())
                .setView(dialogView)
                .setTitle(R.string.venue_children_add)
                .setPositiveButton(R.string.action_add, (dialog, i) -> {
                    // will be overwritten later on
                })
                .setNegativeButton(R.string.action_cancel, (dialog, i) -> dialog.dismiss()));

        baseDialogFragment.show();

        // ensure dialog is being created
        getActivity().getSupportFragmentManager().executePendingTransactions();

        AlertDialog alertDialog = (AlertDialog) baseDialogFragment.getDialog();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            TextInputLayout childNameTextInputLayout = dialogView.findViewById(R.id.childNameTextInputLayout);
            validateChildName(alertDialog, childNameTextInputLayout);
        });
    }

    private void validateChildName(@NonNull AlertDialog alertDialog, TextInputLayout childNameTextInputLayout) {
        String childName = childNameTextInputLayout.getEditText().getText().toString();
        if (VenueChildrenViewModel.isValidChildName(childName)) {
            viewDisposable.add(viewModel.addChild(childName)
                    .onErrorComplete()
                    .doFinally(alertDialog::dismiss)
                    .subscribe());
        } else {
            childNameTextInputLayout.setError(getString(R.string.venue_children_add_validation_error));
        }
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
