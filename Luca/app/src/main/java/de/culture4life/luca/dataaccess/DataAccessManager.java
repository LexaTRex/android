package de.culture4life.luca.dataaccess;

import static de.culture4life.luca.notification.LucaNotificationManager.NOTIFICATION_ID_DATA_ACCESS;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import de.culture4life.luca.BuildConfig;
import de.culture4life.luca.LucaApplication;
import de.culture4life.luca.Manager;
import de.culture4life.luca.R;
import de.culture4life.luca.checkin.CheckInData;
import de.culture4life.luca.checkin.CheckInManager;
import de.culture4life.luca.crypto.CryptoManager;
import de.culture4life.luca.history.HistoryItem;
import de.culture4life.luca.history.HistoryManager;
import de.culture4life.luca.network.NetworkManager;
import de.culture4life.luca.network.endpoints.LucaEndpointsV4;
import de.culture4life.luca.network.pojo.NotifyingHealthDepartment;
import de.culture4life.luca.notification.LucaNotificationManager;
import de.culture4life.luca.preference.PreferencesManager;
import de.culture4life.luca.ui.MainActivity;
import de.culture4life.luca.ui.accesseddata.AccessedDataListItem;
import de.culture4life.luca.util.TimeUtil;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;

/**
 * Periodically polls the luca server, checking if a user's data has been accessed by a health
 * department. The user is notified in case the data was accessed.
 *
 * @see <a href="https://www.luca-app.de/securityoverview/processes/tracing_find_contacts.html#notifying-guests-about-data-access">Security
 * Overview: Notifying Guests about Data Access</a>
 */
public class DataAccessManager extends Manager {

    private static final String UPDATE_TAG = "data_access_update";
    public static final long UPDATE_INTERVAL = BuildConfig.DEBUG ? PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS : TimeUnit.HOURS.toMillis(12);
    public static final long UPDATE_FLEX_PERIOD = BuildConfig.DEBUG ? PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS : TimeUnit.HOURS.toMillis(2);
    public static final long UPDATE_INITIAL_DELAY = TimeUnit.SECONDS.toMillis(10);

    public static final String LAST_UPDATE_TIMESTAMP_KEY = "last_accessed_data_update_timestamp";
    public static final String LAST_INFO_SHOWN_TIMESTAMP_KEY = "last_accessed_data_info_shown_timestamp";
    public static final String LAST_PREVIOUS_CHUNK_ID_KEY = "last_previous_chunk_id";
    public static final String ACCESSED_DATA_KEY = "accessed_data";

    private final PreferencesManager preferencesManager;
    private final NetworkManager networkManager;
    private final LucaNotificationManager notificationManager;
    private final CheckInManager checkInManager;
    private final HistoryManager historyManager;
    private final CryptoManager cryptoManager;

    private WorkManager workManager;

    @Nullable
    private AccessedData accessedData;

    @Nullable
    private Single<NotificationConfig> cachedNotificationConfig;

    public DataAccessManager(@NonNull PreferencesManager preferencesManager, @NonNull NetworkManager networkManager, @NonNull LucaNotificationManager notificationManager, @NonNull CheckInManager checkInManager, @NonNull HistoryManager historyManager, @NonNull CryptoManager cryptoManager) {
        this.preferencesManager = preferencesManager;
        this.networkManager = networkManager;
        this.notificationManager = notificationManager;
        this.checkInManager = checkInManager;
        this.historyManager = historyManager;
        this.cryptoManager = cryptoManager;
    }

    @Override
    protected Completable doInitialize(@NonNull Context context) {
        return Completable.mergeArray(
                preferencesManager.initialize(context),
                networkManager.initialize(context),
                notificationManager.initialize(context),
                checkInManager.initialize(context),
                historyManager.initialize(context),
                cryptoManager.initialize(context)
        ).andThen(Completable.fromAction(() -> {
            this.context = context;
            if (!LucaApplication.isRunningUnitTests()) {
                this.workManager = WorkManager.getInstance(context);
            }
        })).andThen(initializeUpdates());
    }

    /*
        Updates
     */

    private Completable initializeUpdates() {
        return Completable.fromAction(() -> managerDisposable.add(startUpdatingInRegularIntervals()
                .delaySubscription(UPDATE_INITIAL_DELAY, TimeUnit.MILLISECONDS, Schedulers.io())
                .doOnError(throwable -> Timber.e("Unable to start updating in regular intervals: %s", throwable.toString()))
                .onErrorComplete()
                .subscribe()));
    }

    private Completable startUpdatingInRegularIntervals() {
        return getNextRecommendedUpdateDelay()
                .flatMapCompletable(initialDelay -> Completable.fromAction(() -> {
                    if (workManager == null) {
                        managerDisposable.add(Observable.interval(initialDelay, UPDATE_INTERVAL, TimeUnit.MILLISECONDS, Schedulers.io())
                                .flatMapCompletable(tick -> updateIfNecessary()
                                        .doOnError(throwable -> Timber.w("Unable to update: %s", throwable.toString()))
                                        .onErrorComplete())
                                .subscribeOn(Schedulers.io())
                                .subscribe());
                    } else {
                        Constraints constraints = new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build();

                        WorkRequest updateWorkRequest = new PeriodicWorkRequest.Builder(
                                UpdateWorker.class,
                                UPDATE_INTERVAL, TimeUnit.MILLISECONDS,
                                UPDATE_FLEX_PERIOD, TimeUnit.MILLISECONDS
                        ).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                                .setConstraints(constraints)
                                .addTag(UPDATE_TAG)
                                .build();

                        workManager.cancelAllWorkByTag(UPDATE_TAG);
                        workManager.enqueue(updateWorkRequest);
                        Timber.d("Update work request submitted to work manager");
                    }
                }));
    }

    public Completable updateIfNecessary() {
        return getRecentTraceIds()
                .isEmpty()
                .flatMapCompletable(isEmpty -> isEmpty ? Completable.complete() : update());
    }

    public Completable update() {
        return fetchNewRecentlyAccessedTraceData()
                .doOnNext(traceData -> traceData.setIsNew(true))
                .toList()
                .flatMapCompletable(this::processNewRecentlyAccessedTraceData)
                .andThen(preferencesManager.persist(LAST_UPDATE_TIMESTAMP_KEY, System.currentTimeMillis()))
                .doOnSubscribe(disposable -> Timber.d("Updating accessed data"))
                .doOnComplete(() -> Timber.d("Accessed data update complete"))
                .doOnError(throwable -> Timber.w("Accessed data update failed: %s", throwable.toString()));
    }

    public Single<Long> getDurationSinceLastUpdate() {
        return preferencesManager.restoreOrDefault(LAST_UPDATE_TIMESTAMP_KEY, 0L)
                .map(lastUpdateTimestamp -> System.currentTimeMillis() - lastUpdateTimestamp);
    }

    public Single<Long> getNextRecommendedUpdateDelay() {
        return getDurationSinceLastUpdate()
                .map(durationSinceLastUpdate -> UPDATE_INTERVAL - durationSinceLastUpdate)
                .map(recommendedDelay -> Math.max(0, recommendedDelay))
                .doOnSuccess(recommendedDelay -> {
                    String readableDelay = TimeUtil.getReadableDurationWithPlural(recommendedDelay, context).blockingGet();
                    Timber.v("Recommended update delay: %s", readableDelay);
                });
    }

    public Completable processNewRecentlyAccessedTraceData(@NonNull List<AccessedTraceData> accessedTraceData) {
        return Completable.defer(() -> {
            if (accessedTraceData.isEmpty()) {
                // fetch the config anyway to not let anyone figure out
                // IP addresses of potentially infected users
                return getOrFetchNotificationConfig()
                        .onErrorComplete()
                        .ignoreElement();
            } else {
                return Completable.mergeArray(
                        addToAccessedData(accessedTraceData).subscribeOn(Schedulers.io()),
                        addHistoryItems(accessedTraceData).subscribeOn(Schedulers.io()),
                        notifyUserAboutDataAccess(accessedTraceData).subscribeOn(Schedulers.io())
                );
            }
        }).doOnSubscribe(disposable -> Timber.d("New accessed trace data: %s", accessedTraceData));
    }

    /**
     * Informs the user that health authorities have accessed data related to recent check-ins.
     */
    public Completable notifyUserAboutDataAccess(@NonNull List<AccessedTraceData> accessedTraceDataList) {
        return Observable.fromIterable(accessedTraceDataList)
                .sorted((access1, access2) -> Integer.compare(access2.getWarningLevel(), access1.getWarningLevel()))
                .flatMapCompletable(accessedTraceData -> getNotificationTexts(accessedTraceData)
                        .map(notificationTexts -> notificationManager.createDataAccessedNotificationBuilder(MainActivity.class)
                                .setContentTitle(notificationTexts.getTitle())
                                .setContentText(notificationTexts.getShortMessage())
                                .build())
                        .flatMapCompletable(notification -> {
                            int notificationId = NOTIFICATION_ID_DATA_ACCESS + accessedTraceData.getWarningLevel();
                            return notificationManager.showNotification(notificationId, notification);
                        }));
    }

    /*
        Chunks
     */

    /**
     * Fetches the most recent chunk of trace ID hashes, which should always be requested when updating the accessed data.
     */
    private Single<NotificationDataChunk> fetchCurrentChunk() {
        return networkManager.getLucaEndpointsV4()
                .flatMap(LucaEndpointsV4::getNotifications)
                .map(ResponseBody::bytes)
                .map(NotificationDataChunk.Factory::from)
                .doOnSuccess(chunk -> Timber.d("Fetched current chunk: %s", chunk));
    }

    /**
     * Attempts to fetch the chunk that is references as predecessor of the specified chunk.
     * <p>
     * Note that this chunk may already be deleted on the backend if it was older than two weeks,
     * in which case no error is emitted here.
     */
    private Maybe<NotificationDataChunk> fetchPreviousChunk(@NonNull NotificationDataChunk currentChunk) {
        return networkManager.getLucaEndpointsV4()
                .flatMap(lucaEndpointsV4 -> lucaEndpointsV4.getNotifications(currentChunk.getPreviousChunkId()))
                .map(ResponseBody::bytes)
                .map(NotificationDataChunk.Factory::from)
                .doOnSuccess(chunk -> Timber.d("Fetched old chunk: %s", chunk))
                .toMaybe()
                .onErrorResumeNext(throwable -> {
                    if (NetworkManager.isHttpException(throwable, HttpURLConnection.HTTP_NOT_FOUND)) {
                        return Maybe.empty(); // old chunks may be deleted after 14 days
                    } else {
                        return Maybe.error(throwable);
                    }
                })
                .doOnError(throwable -> Timber.w("Unable to fetch old chunk: %s", throwable.toString()));
    }

    private Maybe<NotificationDataChunk> fetchPreviousUnprocessedChunk(@NonNull NotificationDataChunk currentChunk) {
        return shouldFetchPreviousChunk(currentChunk)
                .flatMapMaybe(fetchPreviousChunk -> fetchPreviousChunk ? fetchPreviousChunk(currentChunk) : Maybe.empty());
    }

    private Observable<NotificationDataChunk> fetchPreviousUnprocessedChunks(@NonNull NotificationDataChunk currentChunk) {
        return fetchPreviousUnprocessedChunk(currentChunk)
                .flatMapObservable(previousChunk -> Observable.just(previousChunk)
                        .mergeWith(fetchPreviousUnprocessedChunk(previousChunk)));
    }

    /**
     * Fetches the current chunk and all it's predecessors that have not been processed before.
     */
    protected Observable<NotificationDataChunk> fetchUnprocessedChunks() {
        return fetchCurrentChunk()
                .flatMapObservable(chunk -> Observable.just(chunk)
                        .mergeWith(fetchPreviousUnprocessedChunks(chunk)))
                .doOnSubscribe(disposable -> Timber.d("Fetching unprocessed chunks"))
                .doOnError(throwable -> Timber.w("Unable to fetch unprocessed chunks: %s", throwable.toString()));
    }

    private Single<Boolean> shouldFetchPreviousChunk(@NonNull NotificationDataChunk chunk) {
        return preferencesManager.restoreIfAvailable(LAST_PREVIOUS_CHUNK_ID_KEY, String.class)
                .map(lastProcessedChunkId -> !chunk.getPreviousChunkId().equals(lastProcessedChunkId))
                .defaultIfEmpty(true)
                .doOnSuccess(shouldFetchPreviousChunk -> Timber.d("Should fetch chunk %s: %b", chunk.getPreviousChunkId(), shouldFetchPreviousChunk));
    }

    private Completable markChunksAsProcessed(Observable<NotificationDataChunk> chunks) {
        return chunks.sorted((first, second) -> Long.compare(second.getCreationTimestamp(), first.getCreationTimestamp()))
                .firstElement()
                .doOnSuccess(chunk -> Timber.d("Marking chunk as last processed: %s", chunk.getPreviousChunkId()))
                .flatMapCompletable(chunk -> preferencesManager.persist(LAST_PREVIOUS_CHUNK_ID_KEY, chunk.getPreviousChunkId()));
    }

    /*
        Accessed Trace Data
     */

    /**
     * Emits trace data that is related to the user and is part of a {@link NotificationDataChunk} that has not been processed yet.
     */
    public Observable<AccessedTraceData> fetchRecentlyAccessedTraceData() {
        Observable<NotifyingHealthDepartment> healthDepartments = fetchHealthDepartments().cache();
        Observable<Integer> warningLevels = Observable.range(1, AccessedTraceData.NUMBER_OF_WARNING_LEVELS).cache();
        Observable<String> traceIds = getRecentTraceIds().cache();
        Observable<NotificationDataChunk> chunks = fetchUnprocessedChunks().cache();

        return markChunksAsProcessed(chunks)
                .andThen(getAccessedTraceData(healthDepartments, warningLevels, traceIds, chunks));
    }

    private Observable<AccessedTraceData> getAccessedTraceData(Observable<NotifyingHealthDepartment> healthDepartments, Observable<Integer> warningLevels, Observable<String> traceIds, Observable<NotificationDataChunk> chunks) {
        return chunks.flatMap(
                chunk -> healthDepartments.flatMap(
                        healthDepartment -> warningLevels.flatMap(
                                warningLevel -> traceIds.flatMapMaybe(
                                        traceId -> getAccessedTraceData(healthDepartment, warningLevel, traceId, chunk)))));
    }

    private Maybe<AccessedTraceData> getAccessedTraceData(NotifyingHealthDepartment healthDepartment, int warningLevel, String traceId, NotificationDataChunk chunk) {
        return getHashedTraceId(healthDepartment.getId(), warningLevel, traceId, chunk.getHashLength())
                .filter(hashedTraceId -> chunk.getHashedTraceIds().contains(hashedTraceId))
                .map(hashedTraceId -> {
                    AccessedTraceData data = new AccessedTraceData();
                    data.setTraceId(traceId);
                    data.setHashedTraceId(hashedTraceId);
                    data.setAccessTimestamp(chunk.getCreationTimestamp());
                    data.setHealthDepartment(healthDepartment);
                    data.setWarningLevel(warningLevel);
                    data.setLocationName(getLocationName(data).blockingGet());
                    Pair<Long, Long> checkInAndOutTimestamps = getCheckInAndOutTimestamps(data).blockingGet();
                    data.setCheckInTimestamp(checkInAndOutTimestamps.first);
                    data.setCheckOutTimestamp(checkInAndOutTimestamps.second);
                    return data;
                });
    }

    /**
     * Emits trace data that has been accessed after the last time the accessed trace data has been
     * updated. So all data from {@link #fetchRecentlyAccessedTraceData()} without the data from
     * {@link #getPreviouslyAccessedTraceData()}.
     */
    public Observable<AccessedTraceData> fetchNewRecentlyAccessedTraceData() {
        return getPreviouslyAccessedTraceData()
                .map(AccessedTraceData::getTraceId)
                .toList()
                .doOnSuccess(previouslyAccessedTraceIds -> Timber.d("Previously accessed trace IDs: %s", previouslyAccessedTraceIds))
                .flatMapObservable(previouslyAccessedTraceIds -> fetchRecentlyAccessedTraceData()
                        .filter(accessedTraceData -> !previouslyAccessedTraceIds.contains(accessedTraceData.getTraceId())));
    }

    /**
     * Emits trace data that has been accessed before the last time the accessed trace data has been
     * updated.
     */
    public Observable<AccessedTraceData> getPreviouslyAccessedTraceData() {
        return getOrRestoreAccessedData()
                .map(AccessedData::getTraceData)
                .flatMapObservable(Observable::fromIterable);
    }

    /**
     * Emits trace data with the specified trace ID.
     */
    public Observable<AccessedTraceData> getPreviouslyAccessedTraceData(@NonNull String traceId) {
        return getPreviouslyAccessedTraceData()
                .filter(traceData -> traceData.getTraceId().equals(traceId));
    }

    /**
     * Emits trace data that has been accessed after the last time the user has been shown an info
     * about previously accessed data.
     */
    public Observable<AccessedTraceData> getAccessedTraceDataNotYetInformedAbout() {
        return preferencesManager.restoreOrDefault(LAST_INFO_SHOWN_TIMESTAMP_KEY, 0L)
                .flatMapObservable(lastInfoTimestamp -> getPreviouslyAccessedTraceData()
                        .filter(accessedTraceData -> accessedTraceData.getAccessTimestamp() > lastInfoTimestamp));
    }

    public Completable markAllAccessedTraceDataAsInformedAbout() {
        return preferencesManager.persist(LAST_INFO_SHOWN_TIMESTAMP_KEY, System.currentTimeMillis());
    }

    /*
        Accessed Data
     */

    public Single<AccessedData> getOrRestoreAccessedData() {
        return Maybe.fromCallable(() -> accessedData)
                .switchIfEmpty(restoreAccessedData());
    }

    public Single<AccessedData> restoreAccessedData() {
        return preferencesManager.restoreOrDefault(ACCESSED_DATA_KEY, new AccessedData())
                .doOnSuccess(restoredData -> this.accessedData = restoredData);
    }

    public Completable persistAccessedData(@NonNull AccessedData accessedData) {
        return preferencesManager.persist(ACCESSED_DATA_KEY, accessedData)
                .doOnSubscribe(disposable -> this.accessedData = accessedData);
    }

    /**
     * Emits true if there are is one or more unread access notification.
     */
    public Single<Boolean> hasNewNotifications() {
        return getPreviouslyAccessedTraceData()
                .filter(AccessedTraceData::getIsNew)
                .isEmpty()
                .map(isEmpty -> !isEmpty);
    }

    /**
     * Persists the specified trace data, so that they will be part of {@link
     * #getPreviouslyAccessedTraceData()}.
     */
    public Completable addToAccessedData(@NonNull List<AccessedTraceData> accessedTraceData) {
        return getOrRestoreAccessedData()
                .doOnSuccess(accessedData -> accessedData.addData(accessedTraceData))
                .flatMapCompletable(this::persistAccessedData)
                .doOnComplete(() -> Timber.d("Added trace data to accessed data: %s", accessedTraceData));
    }

    /**
     * Emits true if the specified trace ID is part of the accessed data and is marked as new.
     */
    public Single<Boolean> isNewNotification(@NonNull String traceId) {
        return getPreviouslyAccessedTraceData(traceId)
                .map(AccessedTraceData::getIsNew)
                .filter(isNew -> isNew)
                .firstElement()
                .defaultIfEmpty(false);
    }

    /**
     * Mark accessedTraceData with the given traceId as not new.
     */
    public Completable markAsNotNew(@NonNull String traceId, int warningLevel) {
        return getOrRestoreAccessedData()
                .map(accessedData -> accessedData.markAsNotNew(traceId, warningLevel))
                .flatMapCompletable(this::persistAccessedData);
    }

    /*
        Trace IDs
     */

    /**
     * Emits trace IDs related to recent check-ins.
     */
    public Observable<String> getRecentTraceIds() {
        return checkInManager.getArchivedTraceIds();
    }

    /**
     * Hashes the specified base64 encoded trace ID and encodes the result back to base64.
     */
    public Single<String> getHashedTraceId(@NonNull String healthDepartmentId, int warningLevel, @NonNull String traceId, int hashLength) {
        Single<byte[]> getMessage = Single.just(UUID.fromString(healthDepartmentId))
                .flatMap(CryptoManager::encode)
                .flatMap(bytes -> CryptoManager.concatenate(bytes, new byte[]{(byte) warningLevel}));

        Single<SecretKey> getKey = Single.just(traceId)
                .flatMap(CryptoManager::decodeFromString)
                .flatMap(CryptoManager::createKeyFromSecret);

        return Single.zip(getMessage, getKey, (message, key) -> cryptoManager.getMacProvider().sign(message, key))
                .flatMap(sign -> sign)
                .flatMap(signature -> CryptoManager.trim(signature, hashLength))
                .flatMap(CryptoManager::encodeToString);
    }

    /*
        Health Departments
     */

    protected Observable<NotifyingHealthDepartment> fetchHealthDepartments() {
        return getOrFetchNotificationConfig()
                .map(notificationConfig -> notificationConfig.getHealthDepartments())
                .doOnSuccess(healthDepartments -> Timber.d("Fetched %d health departments", healthDepartments.size()))
                .doOnError(throwable -> Timber.e("Unable to get health departments: %s", throwable.toString()))
                .flatMapObservable(Observable::fromIterable);
    }

    /*
        Notifications
     */

    public Single<NotificationTexts> getNotificationTexts(@NonNull AccessedTraceData accessedTraceData) {
        return getOrFetchNotificationConfig()
                .map(notificationConfig -> notificationConfig.getTexts(
                        accessedTraceData.getWarningLevel(),
                        accessedTraceData.getHealthDepartment().getId()
                ))
                .onErrorResumeWith(getFallbackNotificationTexts(accessedTraceData));
    }

    private Single<NotificationTexts> getFallbackNotificationTexts(@NonNull AccessedTraceData accessedTraceData) {
        return Single.fromCallable(() -> {
            switch (accessedTraceData.getWarningLevel()) {
                case 2:
                    return new NotificationTexts(
                            context.getString(R.string.accessed_data_level_2_title),
                            context.getString(R.string.accessed_data_level_2_banner_text),
                            context.getString(R.string.accessed_data_level_2_description),
                            context.getString(R.string.accessed_data_level_2_detailed_description)
                    );
                case 3:
                    return new NotificationTexts(
                            context.getString(R.string.accessed_data_level_3_title),
                            context.getString(R.string.accessed_data_level_3_banner_text),
                            context.getString(R.string.accessed_data_level_3_description),
                            context.getString(R.string.accessed_data_level_3_detailed_description)
                    );
                case 4:
                    return new NotificationTexts(
                            context.getString(R.string.accessed_data_level_4_title),
                            context.getString(R.string.accessed_data_level_4_banner_text),
                            context.getString(R.string.accessed_data_level_4_description),
                            context.getString(R.string.accessed_data_level_4_detailed_description)
                    );
                default:
                    return new NotificationTexts(
                            context.getString(R.string.accessed_data_level_1_title),
                            context.getString(R.string.accessed_data_level_1_banner_text),
                            context.getString(R.string.accessed_data_level_1_description),
                            context.getString(R.string.accessed_data_level_1_detailed_description)
                    );
            }
        });
    }

    private Single<NotificationConfig> getOrFetchNotificationConfig() {
        return Single.defer(() -> {
            if (cachedNotificationConfig == null) {
                cachedNotificationConfig = fetchNotificationConfig().cache();
            }
            return cachedNotificationConfig;
        });
    }

    protected Single<NotificationConfig> fetchNotificationConfig() {
        return networkManager.getLucaEndpointsV4()
                .flatMap(LucaEndpointsV4::getNotificationConfig)
                .map(NotificationConfig::new);
    }

    public Single<AccessedDataListItem> createAccessDataListItem(@NonNull AccessedTraceData accessedTraceData) {
        return getNotificationTexts(accessedTraceData)
                .map(notificationTexts -> AccessedDataListItem.from(context, accessedTraceData, notificationTexts));
    }

    /*
        History
     */

    /**
     * Creates history items for the specified data, allowing the user to see which data has been
     * accessed after dismissing the notification.
     */
    public Completable addHistoryItems(@NonNull List<AccessedTraceData> accessedTraceData) {
        return Observable.fromIterable(accessedTraceData)
                .flatMapCompletable(historyManager::addTraceDataAccessedItem);
    }

    private Maybe<Long> getHistoryItemTimestamp(@HistoryItem.Type int type, @NonNull AccessedTraceData accessedTraceData) {
        return getRelatedHistoryItems(accessedTraceData)
                .filter(historyItem -> historyItem.getType() == type)
                .map(HistoryItem::getTimestamp)
                .firstElement();
    }

    private Observable<HistoryItem> getRelatedHistoryItems(@NonNull AccessedTraceData accessedTraceData) {
        return historyManager.getItems()
                .filter(historyItem -> accessedTraceData.getTraceId().equals(historyItem.getRelatedId()));
    }

    private Maybe<CheckInData> getCheckInData(@NonNull AccessedTraceData accessedTraceData) {
        return checkInManager.getArchivedCheckInData(accessedTraceData.getTraceId());
    }

    private Single<String> getLocationName(@NonNull AccessedTraceData accessedTraceData) {
        return getCheckInData(accessedTraceData)
                .flatMap(checkInData -> Maybe.fromCallable(checkInData::getLocationDisplayName))
                .defaultIfEmpty(context.getString(R.string.unknown));
    }

    private Single<Pair<Long, Long>> getCheckInAndOutTimestamps(@NonNull AccessedTraceData accessedTraceData) {
        Single<Long> getCheckInTimestamp = getHistoryItemTimestamp(HistoryItem.TYPE_CHECK_IN, accessedTraceData)
                .defaultIfEmpty(accessedTraceData.getAccessTimestamp());

        Single<Long> getCheckOutTimestamp = getHistoryItemTimestamp(HistoryItem.TYPE_CHECK_OUT, accessedTraceData)
                .defaultIfEmpty(accessedTraceData.getAccessTimestamp());

        return Single.zip(getCheckInTimestamp, getCheckOutTimestamp, Pair::new);
    }

}
