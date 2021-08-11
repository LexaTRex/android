package de.culture4life.luca.preference;

import androidx.annotation.NonNull;

import com.nexenio.rxpreferences.provider.PreferencesProvider;

import java.util.HashMap;
import java.util.UUID;

import de.culture4life.luca.checkin.ArchivedCheckInData;
import de.culture4life.luca.checkin.CheckInData;
import de.culture4life.luca.checkin.CheckInManager;
import de.culture4life.luca.crypto.CryptoManager;
import de.culture4life.luca.crypto.TraceIdWrapperList;
import de.culture4life.luca.crypto.WrappedSecret;
import de.culture4life.luca.dataaccess.AccessedData;
import de.culture4life.luca.dataaccess.DataAccessManager;
import de.culture4life.luca.document.DocumentManager;
import de.culture4life.luca.document.Documents;
import de.culture4life.luca.history.HistoryItemContainer;
import de.culture4life.luca.history.HistoryManager;
import de.culture4life.luca.meeting.ArchivedMeetingData;
import de.culture4life.luca.meeting.MeetingData;
import de.culture4life.luca.meeting.MeetingManager;
import de.culture4life.luca.registration.RegistrationData;
import de.culture4life.luca.registration.RegistrationManager;
import de.culture4life.luca.ui.onboarding.OnboardingActivity;
import de.culture4life.luca.ui.registration.RegistrationActivity;
import de.culture4life.luca.ui.registration.RegistrationViewModel;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import timber.log.Timber;

class PreferencesMigrationUtil {

    private static final HashMap<String, Class<?>> PREFERENCE_TYPES = createPreferenceTypes();

    public static Completable migrate(@NonNull PreferencesProvider oldProvider, @NonNull PreferencesProvider newProvider) {
        return oldProvider.getKeys()
                .flatMapCompletable(key -> getPreferenceType(key)
                        .doOnSuccess(type -> Timber.v("Migrating %s to %s", key, newProvider.getClass().getSimpleName()))
                        .flatMapCompletable(type -> oldProvider.restoreIfAvailable(key, type)
                                .flatMapCompletable(value -> newProvider.persistIfNotYetAvailable(key, value)
                                        .andThen(oldProvider.delete(key)))))
                .doOnComplete(() -> Timber.i("Completed migration from %s to %s", oldProvider.getClass().getSimpleName(), newProvider.getClass().getSimpleName()));
    }

    public static Maybe<Class<?>> getPreferenceType(@NonNull String key) {
        return Maybe.fromCallable(() -> {
            if (key.startsWith(CryptoManager.TRACING_SECRET_KEY_PREFIX)) {
                return WrappedSecret.class;
            } else {
                return PREFERENCE_TYPES.get(key);
            }
        });
    }

    private static HashMap<String, Class<?>> createPreferenceTypes() {
        HashMap<String, Class<?>> types = new HashMap<>();
        types.put(CheckInManager.KEY_CHECK_IN_DATA, CheckInData.class);
        types.put(CheckInManager.KEY_ARCHIVED_CHECK_IN_DATA, ArchivedCheckInData.class);
        types.put(CryptoManager.ALIAS_KEYSTORE_PASSWORD, WrappedSecret.class);
        types.put(CryptoManager.TRACE_ID_WRAPPERS_KEY, TraceIdWrapperList.class);
        types.put(CryptoManager.DAILY_KEY_PAIR_PUBLIC_KEY_ID_KEY, Integer.class);
        types.put(CryptoManager.DAILY_KEY_PAIR_PUBLIC_KEY_POINT_KEY, String.class);
        types.put(CryptoManager.DATA_SECRET_KEY, WrappedSecret.class);
        types.put(MeetingManager.KEY_CURRENT_MEETING_DATA, MeetingData.class);
        types.put(MeetingManager.KEY_ARCHIVED_MEETING_DATA, ArchivedMeetingData.class);
        types.put(RegistrationManager.REGISTRATION_COMPLETED_KEY, Boolean.class);
        types.put(RegistrationManager.REGISTRATION_DATA_KEY, RegistrationData.class);
        types.put(RegistrationManager.USER_ID_KEY, UUID.class);
        types.put(RegistrationViewModel.LAST_TAN_REQUEST_TIMESTAMP_KEY, Long.class);
        types.put(RegistrationViewModel.PHONE_VERIFICATION_COMPLETED_KEY, Boolean.class);
        types.put(RegistrationActivity.REGISTRATION_COMPLETED_SCREEN_SEEN_KEY, Boolean.class);
        types.put(HistoryManager.KEY_HISTORY_ITEMS, HistoryItemContainer.class);
        types.put(DocumentManager.KEY_DOCUMENTS, Documents.class);
        types.put(OnboardingActivity.WELCOME_SCREEN_SEEN_KEY, Boolean.class);
        types.put(DataAccessManager.LAST_UPDATE_TIMESTAMP_KEY, Long.class);
        types.put(DataAccessManager.LAST_INFO_SHOWN_TIMESTAMP_KEY, Long.class);
        types.put(DataAccessManager.ACCESSED_DATA_KEY, AccessedData.class);
        return types;
    }

}
