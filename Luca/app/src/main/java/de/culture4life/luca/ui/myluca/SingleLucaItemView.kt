package de.culture4life.luca.ui.myluca

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import de.culture4life.luca.R
import de.culture4life.luca.ui.UiUtil
import kotlin.math.max

class SingleLucaItemView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {
    val layout: ConstraintLayout = LayoutInflater.from(context).inflate(R.layout.my_luca_list_item,
            this,
            true) as ConstraintLayout

    var withTopPadding = false

    init {
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        this.layoutParams = params
        val topPadding = if (withTopPadding) UiUtil.convertDpToPixel(8f, context).toInt() else 0
        layout.setPadding(layout.paddingLeft,
                topPadding,
                layout.paddingRight,
                layout.paddingBottom)
    }
}

