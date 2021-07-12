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

class SingleLucaItemView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        item: MyLucaListItem? = null,
        position: Int = 0,
) : ConstraintLayout(context, attrs) {
    val layout: ConstraintLayout = LayoutInflater.from(context).inflate(R.layout.my_luca_list_item,
            this,
            true) as ConstraintLayout
    lateinit var cardView: CardView
    lateinit var topContent: ViewGroup
    lateinit var titleTextView: TextView
    lateinit var itemTitleImageView: ImageView
    lateinit var barcodeImageView: ImageView
    lateinit var providerTextView: TextView
    lateinit var deleteItemButton: Button
    lateinit var collapseLayout: ViewGroup
    lateinit var collapsedContent: ViewGroup

    init {
        val params = LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT)

        this.layoutParams = params

        val topPadding = if (position == 0) UiUtil.convertDpToPixel(8f,
                context).toInt() else 0
        layout.setPadding(layout.paddingLeft,
                topPadding,
                layout.paddingRight,
                layout.paddingBottom)

        topContent = layout.findViewById(R.id.topContent)
        cardView = layout.findViewById(R.id.cardView)
        titleTextView = layout.findViewById(R.id.itemTitleTextView)
        itemTitleImageView = layout.findViewById(R.id.itemTitleImageView)
        barcodeImageView = layout.findViewById(R.id.qrCodeImageView)
        providerTextView = layout.findViewById(R.id.providerTextView)
        deleteItemButton = layout.findViewById(R.id.deleteItemButton)
        collapseLayout = layout.findViewById(R.id.collapseLayout)
        collapsedContent = layout.findViewById(R.id.collapsedContent)


        item?.let {
            cardView.setCardBackgroundColor(it.color)
            titleTextView.text = it.title
            itemTitleImageView.setImageResource(it.imageResource)
            barcodeImageView.setImageBitmap(it.barcode)
            providerTextView.setText(it.provider)
            collapseLayout.visibility = if (it.isExpanded) View.VISIBLE else View.GONE
            deleteItemButton.text = it.deleteButtonText
            setupDynamicContent(it.getTopContent(), topContent)
            setupDynamicContent(it.getCollapsedContent(), collapsedContent)

        }
    }

    fun setListeners(
            expandClickListener: OnClickListener? = null,
            deleteClickListener: OnClickListener? = null,
    ) {
        deleteClickListener?.let {
            deleteItemButton.setOnClickListener(it)
        }
        expandClickListener?.let {
            layout.setOnClickListener(it)
        }
    }

    private fun addLabelAndText(
            container: ViewGroup,
            labelTextView: ConstraintLayout?,
            label: String?,
            text: String?,
    ) {
        var labelAndTextView: ConstraintLayout? = labelTextView
        if (labelAndTextView == null) {
            val layoutInflater = LayoutInflater.from(container.context)
            labelAndTextView = layoutInflater.inflate(R.layout.my_luca_vaccination_procedure,
                    container,
                    false) as ConstraintLayout
            container.addView(labelAndTextView)
        }
        val labelView = labelAndTextView.findViewById<TextView>(R.id.vaccination_name)
        val textView = labelAndTextView.findViewById<TextView>(R.id.vaccination_date)
        label?.let { labelView.text = it }
        text?.let { textView.text = text }
        setConstrainWidth(labelAndTextView, R.id.vaccination_name, !TextUtils.isEmpty(text))
    }

    private fun setConstrainWidth(
            constraintLayout: ConstraintLayout,
            viewId: Int,
            isConstrained: Boolean,
    ) {
        val set = ConstraintSet()
        set.clone(constraintLayout)
        set.constrainedWidth(viewId, isConstrained)
        set.applyTo(constraintLayout)
    }

    private fun setupDynamicContent(content: List<Pair<String, String>>, topContent: ViewGroup) {
        for (i in 0 until Math.max(topContent.childCount, content.size)) {
            val labelAndTextView = if (topContent.getChildAt(i) != null) topContent.getChildAt(i) as ConstraintLayout else null
            if (content.size > i) {
                val labelAndText = content[i]
                addLabelAndText(topContent,
                        labelAndTextView,
                        labelAndText.first,
                        labelAndText.second)
            } else {
                topContent.removeView(labelAndTextView)
            }
        }
    }
}

