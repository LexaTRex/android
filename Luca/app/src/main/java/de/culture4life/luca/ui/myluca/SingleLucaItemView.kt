package de.culture4life.luca.ui.myluca

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import de.culture4life.luca.R
import de.culture4life.luca.ui.UiUtil

class SingleLucaItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {
    val layout: ConstraintLayout = LayoutInflater.from(context).inflate(
        R.layout.my_luca_list_item,
        this,
        true
    ) as ConstraintLayout

    init {
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        this.layoutParams = params
    }
}

