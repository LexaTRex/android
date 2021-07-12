package de.culture4life.luca.ui.myluca

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import de.culture4life.luca.R

class MyLucaItemForViewPagerFragment :
        Fragment() {

    private var lucaItemView: SingleLucaItemView? = null
    private var expandClickLister: MyLucaListAdapter.MyLucaListItemExpandListener? = null
    private var deleteClickListener: MyLucaListAdapter.MyLucaListClickListener? = null
    private var positionInRecyclerView: Int? = null
    private var item: MyLucaListItem? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.my_luca_list_item_container,
                container,
                false) as ViewGroup
        val lucaItemViewContainer = view.findViewById<ConstraintLayout>(R.id.constraintLayoutMyLucaItemContainer) as ViewGroup
        lucaItemViewContainer.removeAllViews()

        val singleLucaItemView = SingleLucaItemView(requireContext(),
                null,
                0,
                item,
                positionInRecyclerView ?: 0
        )
        lucaItemViewContainer.addView(singleLucaItemView)
        lucaItemView = singleLucaItemView
        setListeners()
        return view
    }

    fun setListeners() {
        val deleteListener = View.OnClickListener { v: View? ->
            item?.let {
                    deleteClickListener?.onDelete(it)
            }
        }
        val expandClickListener = View.OnClickListener { v: View? ->
            item?.let {
                expandClickLister?.onExpand()
            }
        }
        lucaItemView?.setListeners(expandClickListener, deleteListener)
    }

    companion object {
        fun newInstance(
                item: MyLucaListItem?,
                expandClickLister: MyLucaListAdapter.MyLucaListItemExpandListener,
                deleteClickListener: MyLucaListAdapter.MyLucaListClickListener,
                positionInRecyclerView: Int,
        ): MyLucaItemForViewPagerFragment {
            return MyLucaItemForViewPagerFragment().apply {
                this.expandClickLister = expandClickLister
                this.deleteClickListener = deleteClickListener
                item?.let {
                    this.item = it
                }
                this.positionInRecyclerView = positionInRecyclerView
            }
        }
    }
}



