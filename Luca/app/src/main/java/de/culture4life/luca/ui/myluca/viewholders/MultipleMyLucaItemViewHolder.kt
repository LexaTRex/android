package de.culture4life.luca.ui.myluca.viewholders

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import de.culture4life.luca.R

class MultipleMyLucaItemViewHolder(itemView: ViewGroup) : RecyclerView.ViewHolder(itemView) {

    val viewPager: ViewPager2 = itemView.findViewById(R.id.myLucaItemsViewPager)
    val pageIndicator: SpringDotsIndicator = itemView.findViewById(R.id.myLucaItemsViewPagerIndicator)

}
