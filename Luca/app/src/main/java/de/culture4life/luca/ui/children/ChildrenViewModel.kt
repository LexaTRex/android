package de.culture4life.luca.ui.children

import android.app.Application
import androidx.lifecycle.MutableLiveData
import de.culture4life.luca.R
import de.culture4life.luca.children.Child
import de.culture4life.luca.ui.BaseViewModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable


class ChildrenViewModel(application: Application) : BaseViewModel(application) {

    val children: MutableLiveData<ChildListItemContainer> = MutableLiveData()
    private val childrenManager = this.application.childrenManager
    private val registrationManager = this.application.registrationManager
    private val documentManager = this.application.documentManager

    override fun initialize(): Completable {
        return super.initialize()
            .andThen(childrenManager.initialize(application))
            .andThen(registrationManager.initialize(application))
            .andThen(restoreChildren())
    }

    fun navigateBack() {
        navigationController.popBackStack()
    }

    fun addChild(child: Child): Completable {
        return childrenManager.containsChild(child)
            .flatMapCompletable { childExists ->
                if (childExists) {
                    Completable.complete()
                } else {
                    childrenManager.addChild(child)
                        .andThen(updateList())
                }
            }
            .doOnError { throwable ->
                val viewError = createErrorBuilder(throwable)
                    .withTitle(R.string.venue_children_add_error_title)
                    .removeWhenShown()
                    .build()
                addError(viewError)
            }
    }

    fun removeChild(child: Child): Completable {
        return childrenManager.removeChild(child)
            .andThen(documentManager.reImportDocuments())
            .andThen(updateList())
            .doOnError { throwable ->
                addChild(child)
                    .onErrorComplete()
                    .subscribe()
                val viewError = createErrorBuilder(throwable)
                    .withTitle(R.string.venue_children_remove_error_title)
                    .removeWhenShown()
                    .build()
                addError(viewError)
            }
    }

    private fun updateList(): Completable {
        return childrenManager.getChildren()
            .map { children ->
                ChildListItemContainer().apply {
                    for (child in children) {
                        val isCheckedIn = childrenManager.isCheckedIn(child).blockingGet()
                        add(ChildListItem(child, isCheckedIn))
                    }
                }
            }
            .flatMapCompletable { childListItems -> update(children, childListItems) }
    }

    fun restoreChildren(): Completable {
        return childrenManager.getChildren()
            .map { children ->
                Observable.fromIterable(children)
                    .map { child ->
                        ChildListItem(
                            child,
                            childrenManager.isCheckedIn(child).blockingGet()
                        )
                    }
                    .toList()
            }
            .map { listSingle -> ChildListItemContainer(listSingle.blockingGet()) }
            .doOnSuccess { value -> children.setValue(value) }
            .ignoreElement()
    }

    fun toggleCheckIn(childItem: ChildListItem): Completable {
        return Completable.defer {
            childItem.toggleIsChecked()
            if (childItem.isCheckedIn) {
                childrenManager.checkIn(childItem.child)
            } else {
                childrenManager.checkOut(childItem.child)
            }
        }
    }

}