package de.culture4life.luca.ui.myluca

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MyLucaItemViewPager(
        fragment: Fragment,
        private val items: List<MyLucaListItem>,
        private val expandClickListener: MyLucaListAdapter.MyLucaListItemExpandListener,
        private val deleteClickListener: MyLucaListAdapter.MyLucaListClickListener,
        private val positionInRecyclerView: Int,
) :
        FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = items.size

    override fun createFragment(position: Int): Fragment {
        val itemFragment = MyLucaItemForViewPagerFragment.newInstance(items[position],
                expandClickListener,
                deleteClickListener,
                positionInRecyclerView);
        return itemFragment
    }
}
