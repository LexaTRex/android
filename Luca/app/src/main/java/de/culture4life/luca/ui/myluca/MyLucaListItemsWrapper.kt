package de.culture4life.luca.ui.myluca

import de.culture4life.luca.R

data class MyLucaListItemsWrapper(
    val items: List<MyLucaListItem>,
    val timeStamp: Long,
    val sectionHeader: String? = null,
    var isChildSection: Boolean = false
) {

    constructor(sectionHeader: String, isChildSection: Boolean) : this(
        listOf(),
        0,
        sectionHeader,
        isChildSection
    )

    constructor(item: MyLucaListItem, isChildSection: Boolean) : this(
        listOf(item),
        item.timestamp,
        isChildSection = isChildSection
    )

    constructor(items: List<MyLucaListItem>, isChildSection: Boolean) : this(
        items.sortedBy { it.resultTimestamp },
        if (items.isEmpty()) 0 else items.sortedByDescending { it.timestamp }[0].timestamp,
        isChildSection = isChildSection
    )

    fun hasMultipleItems(): Boolean = items.size > 1

    fun isSectionHeader() = sectionHeader != null

    fun sectionDrawable(): Int {
        return if (isChildSection) R.drawable.ic_child else 0
    }

}
