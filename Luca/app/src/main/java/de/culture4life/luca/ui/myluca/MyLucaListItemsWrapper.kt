package de.culture4life.luca.ui.myluca

data class MyLucaListItemsWrapper(
        val items: List<MyLucaListItem>,
        val timeStamp: Long,
) {
    constructor(item: MyLucaListItem) : this(listOf(item), item.timestamp) {
    }

    constructor(items: List<MyLucaListItem>) : this(items.sortedBy { it.resultTimestamp },
            if (items.isEmpty()) 0 else items.sortedByDescending { it.timestamp }[0].timestamp) {
    }

    fun hasMultipleItems(): Boolean = items.size > 1
}
