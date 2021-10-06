package de.culture4life.luca.ui.history;

import static de.culture4life.luca.history.HistoryManager.createCsv;
import static de.culture4life.luca.ui.accesseddata.AccessedDataDetailFragment.KEY_ACCESSED_DATA_LIST_ITEM;
import static de.culture4life.luca.ui.history.HistoryFragment.NO_WARNING_LEVEL_FILTER;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import de.culture4life.luca.R;
import de.culture4life.luca.dataaccess.AccessedTraceData;
import de.culture4life.luca.dataaccess.DataAccessManager;
import de.culture4life.luca.history.CheckOutItem;
import de.culture4life.luca.history.DataSharedItem;
import de.culture4life.luca.history.HistoryItem;
import de.culture4life.luca.history.HistoryManager;
import de.culture4life.luca.history.MeetingEndedItem;
import de.culture4life.luca.ui.BaseViewModel;
import de.culture4life.luca.ui.ViewError;
import de.culture4life.luca.ui.ViewEvent;
import de.culture4life.luca.ui.accesseddata.AccessedDataFragment;
import de.culture4life.luca.util.TimeUtil;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;


public class HistoryViewModel extends BaseViewModel {

    private static final int TAN_CHARS_PER_SECTION = 4;

    private final HistoryManager historyManager;
    private final DataAccessManager dataAccessManager;

    private final MutableLiveData<List<HistoryListItem>> historyItems = new MutableLiveData<>();
    private final MutableLiveData<ViewEvent<String>> tracingTanEvent = new MutableLiveData<>();
    private final MutableLiveData<ViewEvent<List<AccessedTraceData>>> newAccessedData = new MutableLiveData<>();

    private ViewError dataSharingError;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        historyManager = this.application.getHistoryManager();
        dataAccessManager = this.application.getDataAccessManager();
    }

    @Override
    public Completable initialize() {
        return super.initialize()
                .andThen(Completable.mergeArray(
                        historyManager.initialize(application),
                        dataAccessManager.initialize(application)
                ))
                .andThen(invokeHistoryUpdate())
                .andThen(invokeNewAccessedDataUpdate());
    }

    private Completable invokeHistoryUpdate() {
        return Completable.fromAction(() -> modelDisposable.add(updateHistoryItems()
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Timber.i("Updated history"),
                        throwable -> Timber.w("Unable to update history: %s", throwable.toString())
                )));
    }

    private Completable invokeNewAccessedDataUpdate() {
        return Completable.fromAction(() -> modelDisposable.add(showNewAccessedDataIfAvailable()
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Timber.i("Updated accessed data"),
                        throwable -> Timber.w("Unable to update accessed data: %s", throwable.toString())
                )));
    }

    @Override
    public Completable keepDataUpdated() {
        return Completable.mergeArray(
                super.keepDataUpdated(),
                observeHistoryChanges()
        );
    }

    private Completable observeHistoryChanges() {
        return historyManager.getNewItems()
                .throttleLatest(1, TimeUnit.SECONDS)
                .switchMapCompletable(historyItem -> updateHistoryItems());
    }

    private Completable showNewAccessedDataIfAvailable() {
        return dataAccessManager.getAccessedTraceDataNotYetInformedAbout()
                .toList()
                .filter(accessedTraceData -> !accessedTraceData.isEmpty())
                .flatMapCompletable(this::informAboutDataAccess);
    }

    private Completable informAboutDataAccess(List<AccessedTraceData> accessedTraceData) {
        return update(newAccessedData, new ViewEvent<>(accessedTraceData))
                .andThen(dataAccessManager.markAllAccessedTraceDataAsInformedAbout());
    }

    private Completable updateHistoryItems() {
        return loadHistoryItems()
                .toList()
                .flatMapCompletable(items -> update(historyItems, items));
    }

    private Observable<HistoryListItem> loadHistoryItems() {
        // get all available items
        Observable<HistoryItem> cachedItems = historyManager.getItems().cache();

        // find items that belong to each other
        Observable<Pair<HistoryItem, HistoryItem>> cachedStartAndEndItems = cachedItems
                .flatMapMaybe(endItem -> getHistoryStartItem(endItem, cachedItems)
                        .map(startItem -> new Pair<>(startItem, endItem)))
                .cache();

        // find items that don't belong to another item
        Observable<HistoryItem> cachedSingleItems = cachedStartAndEndItems
                .flatMap(historyItemPair -> Observable.just(historyItemPair.first, historyItemPair.second))
                .toList()
                .flatMapObservable(startAndEndItemsList -> cachedItems
                        .filter(historyItem -> !startAndEndItemsList.contains(historyItem)));

        // convert and merge items
        return Observable.mergeArray(
                cachedStartAndEndItems.flatMapMaybe(this::createHistoryViewItem),
                cachedSingleItems.flatMapMaybe(this::createHistoryViewItem)
        ).sorted((first, second) -> Long.compare(second.getTimestamp(), first.getTimestamp()));
    }

    /**
     * Attempts to find the start item for the specified end item.
     *
     * @param endItem      the item to find the matching start item for
     * @param historyItems all available items, sorted by timestamp (descending)
     */
    private Maybe<HistoryItem> getHistoryStartItem(@NonNull HistoryItem endItem, @NonNull Observable<HistoryItem> historyItems) {
        return Maybe.fromCallable(() -> {
            // get the matching start item type
            if (endItem.getType() == HistoryItem.TYPE_CHECK_OUT) {
                return HistoryItem.TYPE_CHECK_IN;
            } else if (endItem.getType() == HistoryItem.TYPE_MEETING_ENDED) {
                return HistoryItem.TYPE_MEETING_STARTED;
            } else {
                return null;
            }
        }).flatMap(startItemType -> historyItems.filter(historyItem -> historyItem.getType() == startItemType)
                .filter(historyItem -> historyItem.getTimestamp() < endItem.getTimestamp())
                .firstElement());
    }

    private Maybe<HistoryListItem> createHistoryViewItem(@NonNull Pair<HistoryItem, HistoryItem> historyItemPair) {
        return Maybe.zip(createHistoryViewItem(historyItemPair.first), createHistoryViewItem(historyItemPair.second), (start, end) -> {
            end.setTime(TimeUtil.getReadableDurationWithPlural(end.getTimestamp() - start.getTimestamp(), application).blockingGet());
            HistoryListItem merged = new HistoryListItem();
            merged.setTitle(end.getTitle());
            merged.setDescription(end.getDescription());
            merged.setAdditionalTitleDetails(end.getAdditionalTitleDetails());
            merged.setTime(application.getString(R.string.history_time_merged, start.getTime(), end.getTime()));
            merged.setTimestamp(end.getTimestamp());
            merged.setTitleIconResourceId(end.getTitleIconResourceId());
            merged.setRelatedId(end.getRelatedId());
            merged.setAccessedTraceData(end.getAccessedTraceData());
            merged.setPrivateMeeting(end.isPrivateMeeting());
            merged.setGuests(end.getGuests());
            return merged;
        });
    }

    private Maybe<HistoryListItem> createHistoryViewItem(@NonNull HistoryItem historyItem) {
        return Maybe.fromCallable(() -> {
            HistoryListItem item = new HistoryListItem();
            item.setTimestamp(historyItem.getTimestamp());
            item.setTime(application.getString(R.string.history_time, TimeUtil.getReadableTime(application, historyItem.getTimestamp())));
            item.setRelatedId(historyItem.getRelatedId());
            switch (historyItem.getType()) {
                case HistoryItem.TYPE_CHECK_IN: {
                    item.setTitle(historyItem.getDisplayName());
                    break;
                }
                case HistoryItem.TYPE_CHECK_OUT: {
                    CheckOutItem checkOutItem = (CheckOutItem) historyItem;

                    item.setTitle(checkOutItem.getDisplayName());

                    List<AccessedTraceData> accessedTraceData = dataAccessManager.getPreviouslyAccessedTraceData(checkOutItem.getRelatedId())
                            .toList().blockingGet();
                    boolean accessed = !accessedTraceData.isEmpty();
                    item.setAccessedTraceData(accessedTraceData);
                    item.setAdditionalTitleDetails(application.getString(R.string.history_check_out_details, checkOutItem.getRelatedId()));
                    item.setTitleIconResourceId(accessed ? R.drawable.ic_eye : R.drawable.ic_information_outline);

                    List<String> children = checkOutItem.getChildren();
                    if (children != null && !children.isEmpty()) {
                        String currentDescription = item.getDescription();
                        StringBuilder builder = new StringBuilder();
                        if (currentDescription != null) {
                            builder = builder.append(currentDescription)
                                    .append(System.lineSeparator());
                        }
                        String childrenCsv = createCsv(children);
                        builder = builder.append(application.getString(R.string.history_children_title, childrenCsv));
                        item.setDescription(builder.toString());
                    }
                    break;
                }
                case HistoryItem.TYPE_MEETING_STARTED: {
                    item.setTitle(application.getString(R.string.history_meeting_started_title));
                    break;
                }
                case HistoryItem.TYPE_MEETING_ENDED: {
                    MeetingEndedItem meetingEndedItem = (MeetingEndedItem) historyItem;
                    item.setTitle(application.getString(R.string.history_meeting_ended_title));
                    item.setPrivateMeeting(true);
                    item.setAdditionalTitleDetails(application.getString(R.string.history_check_out_details, item.getRelatedId()));
                    item.setTitleIconResourceId(R.drawable.ic_information_outline);
                    if (meetingEndedItem.getGuests().isEmpty()) {
                        item.setDescription(application.getString(R.string.history_meeting_empty_description));
                    } else {
                        String guestCsv = createCsv(meetingEndedItem.getGuests());
                        item.setDescription(application.getString(R.string.history_meeting_not_empty_description, guestCsv));
                        item.setGuests(meetingEndedItem.getGuests());
                    }
                    break;
                }
                case HistoryItem.TYPE_CONTACT_DATA_REQUEST: {
                    DataSharedItem dataSharedItem = (DataSharedItem) historyItem;
                    item.setTitle(application.getString(R.string.history_data_shared_title));
                    item.setAdditionalTitleDetails(application.getString(R.string.history_data_shared_description, dataSharedItem.getDays()));
                    item.setTitleIconResourceId(R.drawable.ic_information_outline);
                    break;
                }
                default: {
                    Timber.w("Unknown history item type: %d", historyItem.getType());
                    return null;
                }
            }
            return item;
        }).doOnError(throwable -> Timber.w("Unable to create history view item for %s: %s", historyItem, throwable.toString()))
                .onErrorComplete();
    }

    public void onShareHistoryRequested(int days) {
        Timber.d("onShareHistoryRequested() called with: days = [%s]", days);
        modelDisposable.add(application.getRegistrationManager()
                .transferUserData(days)
                .doOnSubscribe(disposable -> {
                    updateAsSideEffect(isLoading, true);
                    removeError(dataSharingError);
                })
                .map(String::toUpperCase)
                .map(tracingTan -> {
                    if (tracingTan.contains("-")) {
                        return tracingTan;
                    }
                    StringBuilder tracingTanBuilder = new StringBuilder(tracingTan);
                    for (int i = 1; i < tracingTan.length() / TAN_CHARS_PER_SECTION; i++) {
                        int hyphenPosition = tracingTanBuilder.lastIndexOf("-") + TAN_CHARS_PER_SECTION + 1;
                        tracingTanBuilder.insert(hyphenPosition, "-");
                    }
                    return tracingTanBuilder.toString();
                })
                .flatMapCompletable(tracingTan -> Completable.mergeArray(
                        update(tracingTanEvent, new ViewEvent<>(tracingTan)),
                        historyManager.addDataSharedItem(tracingTan, days)
                ))
                .doOnError(throwable -> {
                    dataSharingError = createErrorBuilder(throwable)
                            .withTitle(R.string.error_request_failed_title)
                            .withResolveAction(Completable.fromAction(() -> onShareHistoryRequested(days)))
                            .withResolveLabel(R.string.action_retry)
                            .build();
                    addError(dataSharingError);
                })
                .doFinally(() -> updateAsSideEffect(isLoading, false))
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Timber.d("Received data sharing TAN"),
                        throwable -> Timber.w("Unable to get data sharing TAN: %s", throwable.toString())
                ));
    }

    public static List<HistoryListItem> filterHistoryListItems(List<HistoryListItem> items, int warningLevelFilter) {
        if (warningLevelFilter != NO_WARNING_LEVEL_FILTER) {
            return items.stream()
                    .filter(item -> item.containsWarningLevel(warningLevelFilter))
                    .map(item -> filterAccessedTraceData(item, warningLevelFilter))
                    .collect(Collectors.toList());
        } else {
            return items;
        }
    }

    private static HistoryListItem filterAccessedTraceData(HistoryListItem item, int warningLevelFilter) {
        item.setAccessedTraceData(item.getAccessedTraceData().stream()
                .filter(traceData -> traceData.getWarningLevel() == warningLevelFilter)
                .collect(Collectors.toList()));
        return item;
    }

    private static List<AccessedTraceData> filterTraceData(List<AccessedTraceData> items, int warningLevelFilter) {
        if (warningLevelFilter != NO_WARNING_LEVEL_FILTER) {
            return items.stream()
                    .filter(item -> item.getWarningLevel() == warningLevelFilter)
                    .collect(Collectors.toList());
        } else {
            return items;
        }
    }

    public void onShowAccessedDataRequested() {
        onShowAccessedDataRequested(new ArrayList(), NO_WARNING_LEVEL_FILTER);
    }

    public void onShowAccessedDataRequested(@NonNull List<AccessedTraceData> accessedTraceData, int warningLevelFilter) {
        Bundle bundle = new Bundle();
        List<AccessedTraceData> filteredData = filterTraceData(accessedTraceData, warningLevelFilter);
        if (!filteredData.isEmpty()) {
            AccessedTraceData firstItem = filteredData.get(0);
            if (filteredData.size() == 1) {
                modelDisposable.add(dataAccessManager.createAccessDataListItem(filteredData.get(0))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(dataListItem -> {
                            if (isCurrentDestinationId(R.id.historyFragment)) {
                                bundle.putSerializable(KEY_ACCESSED_DATA_LIST_ITEM, dataListItem);
                                navigationController.navigate(R.id.action_historyFragment_to_accessedDataDetailFragment, bundle);
                            }
                        }));
            } else {
                bundle.putString(AccessedDataFragment.KEY_TRACE_ID, firstItem.getTraceId());
                bundle.putInt(HistoryFragment.KEY_WARNING_LEVEL_FILTER, warningLevelFilter);
                navigationController.navigate(R.id.action_historyFragment_to_accessedDataFragment, bundle);
            }
        }
    }

    public LiveData<List<HistoryListItem>> getHistoryItems() {
        return historyItems;
    }

    public LiveData<ViewEvent<String>> getTracingTanEvent() {
        return tracingTanEvent;
    }

    public LiveData<ViewEvent<List<AccessedTraceData>>> getNewAccessedData() {
        return newAccessedData;
    }

}
