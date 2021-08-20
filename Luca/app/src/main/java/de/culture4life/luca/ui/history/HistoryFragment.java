package de.culture4life.luca.ui.history;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import de.culture4life.luca.R;
import de.culture4life.luca.dataaccess.AccessedTraceData;
import de.culture4life.luca.history.HistoryManager;
import de.culture4life.luca.ui.BaseFragment;
import de.culture4life.luca.ui.UiUtil;
import de.culture4life.luca.ui.dialog.BaseDialogFragment;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;

public class HistoryFragment extends BaseFragment<HistoryViewModel> {

    private ImageView accessedDataImageView;
    private TextView emptyTitleTextView;
    private TextView emptyDescriptionTextView;
    private ImageView emptyImageView;
    private ListView historyListView;
    private HistoryListAdapter historyListAdapter;
    private MaterialButton shareHistoryButton;

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_history;
    }

    @Override
    protected Class<HistoryViewModel> getViewModelClass() {
        return HistoryViewModel.class;
    }

    @Override
    protected Completable initializeViews() {
        return super.initializeViews()
                .andThen(Completable.fromAction(() -> {
                    initializeHistoryItemsViews();
                    initializeShareHistoryViews();
                    initializeAccessedDataViews();
                    initializeEmptyStateViews();
                }));
    }

    private void initializeHistoryItemsViews() {
        historyListView = getView().findViewById(R.id.historyListView);
        View paddingView = new View(getContext());
        paddingView.setMinimumHeight((int) UiUtil.convertDpToPixel(16, getContext()));
        paddingView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        historyListView.addHeaderView(paddingView);

        historyListAdapter = new HistoryListAdapter(getContext(), historyListView.getId());
        historyListAdapter.setItemClickHandler(new HistoryListAdapter.ItemClickHandler() {
            @Override
            public void showAdditionalTitleDetails(@NonNull HistoryListItem item) {
                showHistoryItemDetailsDialog(item, item.getAdditionalTitleDetails());
            }

            @Override
            public void showAdditionalDescriptionDetails(@NonNull HistoryListItem item) {
                showHistoryItemDetailsDialog(item, item.getAdditionalDescriptionDetails());
            }
        });
        historyListView.setAdapter(historyListAdapter);
        observe(viewModel.getHistoryItems(), items -> historyListAdapter.setHistoryItems(items));
    }

    private void initializeShareHistoryViews() {
        shareHistoryButton = getView().findViewById(R.id.primaryActionButton);
        shareHistoryButton.setOnClickListener(button -> showShareHistorySelectionDialog());
        observe(viewModel.getHistoryItems(), items -> shareHistoryButton.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE));
        observe(viewModel.getTracingTanEvent(), tracingTanEvent -> {
            if (!tracingTanEvent.hasBeenHandled()) {
                showShareHistoryTanDialog(tracingTanEvent.getValueAndMarkAsHandled());
            }
        });
    }

    private void initializeAccessedDataViews() {
        accessedDataImageView = getView().findViewById(R.id.accessedDataImageView);
        accessedDataImageView.setOnClickListener(v -> viewModel.onShowAccessedDataRequested());

        observe(viewModel.getNewAccessedData(), accessedDataEvent -> {
            if (!accessedDataEvent.hasBeenHandled()) {
                accessedDataImageView.setImageResource(R.drawable.ic_eye_notification);
                accessedDataImageView.clearColorFilter();
                showAccessedDataDialog(accessedDataEvent.getValueAndMarkAsHandled());
            }
        });
    }

    private void initializeEmptyStateViews() {
        emptyTitleTextView = getView().findViewById(R.id.emptyTitleTextView);
        emptyDescriptionTextView = getView().findViewById(R.id.emptyDescriptionTextView);
        emptyImageView = getView().findViewById(R.id.emptyImageView);
        emptyDescriptionTextView.setText(getString(R.string.history_empty_description, HistoryManager.KEEP_DATA_DAYS));

        observe(viewModel.getHistoryItems(), items -> {
            int emptyStateVisibility = items.isEmpty() ? View.VISIBLE : View.GONE;
            int contentVisibility = !items.isEmpty() ? View.VISIBLE : View.GONE;
            emptyTitleTextView.setVisibility(emptyStateVisibility);
            emptyDescriptionTextView.setVisibility(emptyStateVisibility);
            emptyImageView.setVisibility(emptyStateVisibility);
            historyListView.setVisibility(contentVisibility);
            shareHistoryButton.setVisibility(contentVisibility);
        });
    }

    private void showHistoryItemDetailsDialog(@NonNull HistoryListItem item, String additionalDetails) {
        new BaseDialogFragment(new MaterialAlertDialogBuilder(getContext())
                .setTitle(item.getTitle())
                .setMessage(additionalDetails)
                .setPositiveButton(R.string.action_ok, (dialogInterface, i) -> dialogInterface.cancel())
                .setNeutralButton(R.string.action_copy, (dialogInterface, i) -> {
                    String content = item.getTitle() + "\n" + item.getTime() + "\n" + additionalDetails;
                    ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("luca", content);
                    clipboard.setPrimaryClip(clip);
                }))
                .show();
    }

    private void showShareHistorySelectionDialog() {
        ViewGroup viewGroup = getActivity().findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.days_selection_dialog, viewGroup, false);

        int maximumDays = (int) TimeUnit.MILLISECONDS.toDays(HistoryManager.SHARE_DATA_DURATION);
        String[] items = new String[maximumDays];
        Observable.range(1, maximumDays)
                .map(String::valueOf)
                .toList().blockingGet().toArray(items);
        TextView message = dialogView.findViewById(R.id.messageTextView);
        message.setText(getString(R.string.history_share_selection_description, HistoryManager.KEEP_DATA_DAYS));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.day_selection_item, items);
        AutoCompleteTextView autoCompleteTextView = dialogView.findViewById(R.id.dayInputAutoCompleteTextView);
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setText(String.valueOf(maximumDays), false);

        new BaseDialogFragment(new MaterialAlertDialogBuilder(getContext())
                .setView(dialogView)
                .setTitle(getString(R.string.history_share_selection_title))
                .setPositiveButton(R.string.history_share_selection_action, (dialogInterface, i) -> {
                    int selectedDays = Integer.parseInt(autoCompleteTextView.getText().toString());
                    showShareHistoryConfirmationDialog(selectedDays);
                })
                .setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> dialogInterface.cancel()))
                .show();
    }

    private void showShareHistoryConfirmationDialog(int days) {
        new BaseDialogFragment(new MaterialAlertDialogBuilder(getContext())
                .setTitle(getString(R.string.history_share_confirmation_title))
                .setMessage(getFormattedString(R.string.history_share_confirmation_description, days))
                .setPositiveButton(R.string.history_share_confirmation_action, (dialogInterface, i) -> viewModel.onShareHistoryRequested(days))
                .setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> dialogInterface.cancel()))
                .show();
    }

    private void showShareHistoryTanDialog(@NonNull String tracingTan) {
        BaseDialogFragment dialogFragment = new BaseDialogFragment(new MaterialAlertDialogBuilder(getContext())
                .setTitle(getString(R.string.history_share_tan_title))
                .setMessage(getString(R.string.history_share_tan_description, tracingTan))
                .setPositiveButton(R.string.action_ok, (dialogInterface, i) -> dialogInterface.dismiss()));
        dialogFragment.setCancelable(false);
        dialogFragment.show();
    }

    private void showAccessedDataDialog(@NonNull List<AccessedTraceData> accessedTraceDataList) {
        if (accessedTraceDataList.isEmpty()) {
            return;
        }
        BaseDialogFragment dialogFragment = new BaseDialogFragment(new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.accessed_data_dialog_title)
                .setMessage(R.string.accessed_data_dialog_description)
                .setPositiveButton(R.string.accessed_data_dialog_action_show, (dialog, which) -> viewModel.onShowAccessedDataRequested())
                .setNeutralButton(R.string.accessed_data_dialog_action_dismiss, (dialogInterface, i) -> dialogInterface.cancel()));

        dialogFragment.setCancelable(false);
        dialogFragment.show();
    }

}
