package de.culture4life.luca.ui.children

import java.util.*

class ChildListItemContainer : ArrayList<ChildListItem> {
    constructor()
    constructor(children: Collection<ChildListItem>) : super(children)
}