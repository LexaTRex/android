package de.culture4life.luca.children

import android.content.Context
import de.culture4life.luca.Manager
import de.culture4life.luca.preference.PreferencesManager
import de.culture4life.luca.registration.RegistrationManager
import de.culture4life.luca.ui.children.LegacyChildListItemContainer
import de.culture4life.luca.util.StringSanitizeUtil
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class ChildrenManager(
    private val preferencesManager: PreferencesManager,
    private val registrationManager: RegistrationManager
) : Manager() {
    companion object {
        const val LEGACY_KEY_CHILDREN = "children"
        private const val KEY_CHILDREN = "children2"
        private const val KEY_CHECKED_IN_CHILDREN = "checked_in_children"

        fun isValidChildName(childName: String): Boolean {
            return StringSanitizeUtil.sanitize(childName).trim().isNotEmpty()
        }

        fun isValidChildName(child: Child): Boolean {
            return isValidChildName(child.firstName) && isValidChildName(child.lastName)
        }
    }

    override fun doInitialize(context: Context): Completable {
        return preferencesManager.initialize(context)
            .andThen(registrationManager.initialize(context))
            .andThen(migrate())
    }

    /**
     * Add a child
     */
    fun addChild(child: Child): Completable {
        return getChildren()
            .map { children -> children.apply { add(child) } }
            .flatMapCompletable(this::persistChildren)
    }

    /**
     * Remove a child
     */
    fun removeChild(child: Child): Completable {
        return checkOut(child)
            .andThen(getChildren())
            .map { children -> children.apply { remove(child) } }
            .flatMapCompletable(this::persistChildren)
    }

    /**
     * Get children from preferences
     */
    fun getChildren(): Single<Children> {
        return preferencesManager.restoreOrDefault(KEY_CHILDREN, Children())
    }

    /**
     * Check if user has one or more children
     */
    fun hasChildren(): Single<Boolean> {
        return getChildren().map { !it.isEmpty() }
    }

    /**
     * Check if the given child is in the storage
     */
    fun containsChild(child: Child): Single<Boolean> {
        return getChildren().map { it.contains(child) }
    }

    /**
     * Mark child as checked in
     */
    fun checkIn(child: Child): Completable {
        return loadCheckins()
            .map { it.apply { add(child.getFullName()) } }
            .flatMapCompletable {
                preferencesManager.persist(KEY_CHECKED_IN_CHILDREN, it)
            }
    }

    /**
     * Mark child as not checked in
     */
    fun checkOut(child: Child): Completable {
        return loadCheckins()
            .map { it.apply { remove(child.getFullName()) } }
            .flatMapCompletable { preferencesManager.persist(KEY_CHECKED_IN_CHILDREN, it) }
    }

    /**
     * Remove all children from the checked in list
     */
    fun clearCheckIns(): Completable {
        return preferencesManager.delete(KEY_CHECKED_IN_CHILDREN)
    }

    /**
     * Return true if the given child is checked in
     */
    fun isCheckedIn(child: Child): Single<Boolean> {
        return loadCheckins().map { it.contains(child.getFullName()) }
    }

    private fun loadCheckins(): Single<HashSet<String>> {
        return preferencesManager.restoreOrDefault(KEY_CHECKED_IN_CHILDREN, HashSet())
    }

    private fun cleanCheckins(children: Children): Completable {
        return loadCheckins()
            .map { checkedIn ->
                val childNames = children.map { it.getFullName() }.toSet()
                checkedIn.filter { childNames.contains(it) }
            }.flatMapCompletable { preferencesManager.persist(KEY_CHECKED_IN_CHILDREN, it) }
    }

    /**
     * Get children that are currently marked as checked-in
     */
    fun getCheckedInChildren(): Single<List<Child>> {
        return getChildren().map {
            it.filter { child -> isCheckedIn(child).blockingGet() }
        }
    }

    /**
     * Persist children in preferences
     */
    fun persistChildren(children: Children): Completable {
        return preferencesManager.persist(KEY_CHILDREN, children)
            .andThen(cleanCheckins(children))
    }

    /**
     * Migrate from legacy to the new storage key and data model
     */
    fun migrate(): Completable {
        return preferencesManager.restoreIfAvailable(LEGACY_KEY_CHILDREN, LegacyChildListItemContainer::class.java)
            .map {
                val lastName = registrationManager.getOrCreateRegistrationData().blockingGet().lastName ?: ""
                it.map { childListItem -> Child.from(childListItem.name, lastName) }
            }
            .flatMapCompletable {
                if (it.isNotEmpty()) {
                    persistChildren(Children().apply { addAll(it) })
                        .andThen(preferencesManager.delete(LEGACY_KEY_CHILDREN))
                } else {
                    Completable.complete()
                }
            }
    }
}