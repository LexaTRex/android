package de.culture4life.luca.ui.myluca

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.culture4life.luca.ui.myluca.viewholders.SingleMyLucaItemViewHolder

class MyLucaItemForViewPagerFragment :
    Fragment() {

    private lateinit var expandClickLister: MyLucaListAdapter.MyLucaListItemExpandListener
    private lateinit var deleteClickListener: MyLucaListAdapter.MyLucaListClickListener
    private lateinit var item: MyLucaListItem
    private lateinit var itemViewHolder: SingleMyLucaItemViewHolder

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        itemViewHolder = SingleMyLucaItemViewHolder(SingleLucaItemView(requireContext()))
        itemViewHolder.show(item)
        itemViewHolder.setListeners(
            { expandClickLister?.onExpand() },
            { deleteClickListener?.onDelete(item) })
        return itemViewHolder.view
    }

    companion object {
        fun newInstance(
            item: MyLucaListItem,
            expandClickLister: MyLucaListAdapter.MyLucaListItemExpandListener,
            deleteClickListener: MyLucaListAdapter.MyLucaListClickListener,
        ): MyLucaItemForViewPagerFragment {
            return MyLucaItemForViewPagerFragment().apply {
                this.expandClickLister = expandClickLister
                this.deleteClickListener = deleteClickListener
                this.item = item
            }
        }
    }
}



