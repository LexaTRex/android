package de.culture4life.luca.ui.myluca.viewholders

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import de.culture4life.luca.R

class SingleMyLucaItemViewHolder(
        view: View,
) : RecyclerView.ViewHolder(view) {

    val constraintLayoutContainer: ConstraintLayout = itemView.findViewById(R.id.constraintLayoutMyLucaItemContainer)

}

