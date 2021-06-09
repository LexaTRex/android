package de.culture4life.luca.ui.venue.children;

import android.app.Application;

import de.culture4life.luca.R;
import de.culture4life.luca.checkin.CheckInManager;
import de.culture4life.luca.preference.PreferencesManager;
import de.culture4life.luca.ui.BaseViewModel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public class VenueChildrenViewModel extends BaseViewModel {

    private final MutableLiveData<ChildListItemContainer> children;

    private final PreferencesManager preferencesManager;

    public VenueChildrenViewModel(@NonNull Application application) {
        super(application);
        preferencesManager = this.application.getPreferencesManager();
        children = new MutableLiveData<>();
    }

    @Override
    public Completable initialize() {
        return super.initialize()
                .andThen(restoreChildren());
    }

    void openVenueDetailsView() {
        if (isCurrentDestinationId(R.id.venueChildrenFragment)) {
            navigationController.navigate(R.id.action_venueChildFragment_to_venueDetailsFragment);
        }
    }

    Completable addChild(@NonNull String childName) {
        return addChild(new ChildListItem(childName));
    }

    private Completable addChild(@NonNull ChildListItem child) {
        return Single.just(child)
                .doOnSuccess(childListItem -> childListItem.setChecked(true))
                .flatMapCompletable(childListItem -> updateChildren(childListItem, true))
                .andThen(persistChildren());
    }

    Completable removeChild(@NonNull ChildListItem child) {
        return updateChildren(child, false)
                .andThen(persistChildren());
    }

    private Completable updateChildren(ChildListItem child, boolean shouldAdd) {
        return Completable.defer(() -> {
            ChildListItemContainer childrenContainer = children.getValue();
            if (shouldAdd) {
                childrenContainer.add(child);
            } else {
                childrenContainer.remove(child);
            }
            return update(children, childrenContainer);
        });
    }

    public void persistChildrenAsSideEffect() {
        persistChildren().subscribe();
    }

    private Completable persistChildren() {
        return preferencesManager
                .persist(CheckInManager.KEY_CHILDREN, children.getValue());
    }

    Completable restoreChildren() {
        return preferencesManager.restoreOrDefault(CheckInManager.KEY_CHILDREN, new ChildListItemContainer())
                .doOnSuccess(children::setValue)
                .ignoreElement();
    }

    public LiveData<ChildListItemContainer> getChildren() {
        return children;
    }

}
