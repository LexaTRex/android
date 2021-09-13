package de.culture4life.luca.ui.history;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import de.culture4life.luca.R;
import de.culture4life.luca.dataaccess.AccessedTraceData;
import de.culture4life.luca.history.HistoryManager;
import de.culture4life.luca.ui.BaseFragment;
import de.culture4life.luca.ui.MainActivity;
import de.culture4life.luca.ui.dialog.BaseDialogFragment;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class HistoryFragment extends BaseFragment<HistoryViewModel> {

    public static final String KEY_WARNING_LEVEL_FILTER = "WarningLevel";
    public static final int NO_WARNING_LEVEL_FILTER = -1;

    private TextView emptyTitleTextView;
    private TextView emptyDescriptionTextView;
    private TextView headingTextView;
    private ImageView emptyImageView;
    private ListView historyListView;
    private HistoryListAdapter historyListAdapter;
    private MaterialButton shareHistoryButton;
    private int warningLevelFilter = NO_WARNING_LEVEL_FILTER;
    private ImageView deleteHistoryImageView;

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_history;
    }

    @Override
    protected Class<HistoryViewModel> getViewModelClass() {
        return HistoryViewModel.class;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            warningLevelFilter = getArguments().getInt(KEY_WARNING_LEVEL_FILTER, NO_WARNING_LEVEL_FILTER);
        }
    }

    @Override
    protected Completable initializeViews() {
        return super.initializeViews()
                .andThen(Completable.fromAction(() -> {
                    initializeHistoryItemsViews();
                    initializeShareHistoryViews();
                    initializeAccessedDataViews();
                    initializeDeleteHistoryListener();
                    initializeEmptyStateViews();
                }));
    }

    private void initializeDeleteHistoryListener() {
        deleteHistoryImageView = getView().findViewById(R.id.deleteHistoryImageView);
        deleteHistoryImageView.setOnClickListener(v -> showClearHistoryConfirmationDialog());
    }

    private void initializeHistoryItemsViews() {
        headingTextView = getView().findViewById(R.id.headingTextView);
        headingTextView.setText(getTitleFor(warningLevelFilter));
        historyListView = getView().findViewById(R.id.historyListView);
        historyListAdapter = new HistoryListAdapter(getContext(), historyListView.getId(), warningLevelFilter == NO_WARNING_LEVEL_FILTER);
        historyListAdapter.setItemClickHandler(new HistoryListAdapter.ItemClickHandler() {
            @Override
            public void showAccessedDataDetails(@NonNull HistoryListItem item) {
                viewModel.onShowAccessedDataRequested(item.getAccessedTraceData(), warningLevelFilter);
            }

            @Override
            public void showAdditionalDescriptionDetails(@NonNull HistoryListItem item) {
                showHistoryItemDetailsDialog(item, item.getAdditionalDescriptionDetails());
            }

            @Override
            public void showTraceInformation(@NonNull HistoryListItem item) {
                showHistoryItemDetailsDialog(item, item.getAdditionalTitleDetails());
            }
        });
        historyListView.setAdapter(historyListAdapter);
        observe(viewModel.getHistoryItems(), items -> historyListAdapter.setHistoryItems(HistoryViewModel.filterHistoryListItems(items, warningLevelFilter)));
    }

    private int getTitleFor(int warningLevelFilter) {
        switch (warningLevelFilter) {
            case 1:
                return R.string.accessed_data_dialog_title;
            case 2:
                return R.string.accessed_data_level_2_title;
            case 3:
                return R.string.accessed_data_level_3_title;
            case 4:
                return R.string.accessed_data_level_4_title;
            default:
                return R.string.navigation_history;
        }
    }

    private void initializeShareHistoryViews() {
        shareHistoryButton = getView().findViewById(R.id.primaryActionButton);
        shareHistoryButton.setOnClickListener(button -> showShareHistorySelectionDialog());
        observe(viewModel.getHistoryItems(), items -> {
            boolean hideButton = items.isEmpty() || warningLevelFilter != NO_WARNING_LEVEL_FILTER;
            shareHistoryButton.setVisibility(hideButton ? View.GONE : View.VISIBLE);
        });
        observe(viewModel.getTracingTanEvent(), tracingTanEvent -> {
            if (!tracingTanEvent.hasBeenHandled()) {
                showShareHistoryTanDialog(tracingTanEvent.getValueAndMarkAsHandled());
            }
        });
    }

    private void initializeAccessedDataViews() {
        observe(viewModel.getNewAccessedData(), accessedDataEvent -> {
            if (!accessedDataEvent.hasBeenHandled()) {
                ((MainActivity) getActivity()).updateHistoryBadge();
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
            deleteHistoryImageView.setVisibility(contentVisibility);
        });
    }

    private void showClearHistoryConfirmationDialog() {
        new BaseDialogFragment(new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.history_clear_title)
                .setMessage(R.string.history_clear_description)
                .setPositiveButton(R.string.history_clear_action, (dialog, which) -> {
                    application.getHistoryManager().clearItems()
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    () -> Timber.i("History cleared"),
                                    throwable -> Timber.w("Unable to clear history: %s", throwable.toString())
                            );
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss()))
                .show();
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
        if (accessedTraceDataList.isEmpty() || !hasWarningLevel1(accessedTraceDataList)) {
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

    private boolean hasWarningLevel1(@NonNull List<AccessedTraceData> accessedTraceDataList) {
        boolean hasWarningLevel1 = false;
        for (AccessedTraceData data : accessedTraceDataList) {
            if (data.getWarningLevel() == 1) {
                hasWarningLevel1 = true;
                break;
            }
        }

        return hasWarningLevel1;
    }
}
