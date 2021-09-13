package de.culture4life.luca.ui.myluca.viewholders

import android.text.TextUtils
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
import androidx.recyclerview.widget.RecyclerView
import de.culture4life.luca.R
import de.culture4life.luca.ui.myluca.MyLucaListItem
import de.culture4life.luca.ui.myluca.SingleLucaItemView
import kotlin.math.max

class SingleMyLucaItemViewHolder(val view: SingleLucaItemView) : RecyclerView.ViewHolder(view) {

    private val cardView: CardView = view.findViewById(R.id.cardView)
    private val topContent: ViewGroup = view.findViewById(R.id.topContent)
    private val collapseLayout: ViewGroup = view.findViewById(R.id.collapseLayout)
    private val collapsedContent: ViewGroup = view.findViewById(R.id.collapsedContent)
    private val titleTextView: TextView = view.findViewById(R.id.itemTitleTextView)
    private val itemTitleImageView: ImageView = view.findViewById(R.id.itemTitleImageView)
    private val barcodeImageView: ImageView = view.findViewById(R.id.qrCodeImageView)
    private val providerTextView: TextView = view.findViewById(R.id.providerTextView)
    private val deleteItemButton: Button = view.findViewById(R.id.deleteItemButton)
    private val collapseIndicator: ImageView = view.findViewById(R.id.collapseIndicator)

    fun show(item: MyLucaListItem) {
        cardView.setCardBackgroundColor(item.color)
        titleTextView.text = item.title
        itemTitleImageView.setImageResource(item.imageResource)
        barcodeImageView.setImageBitmap(item.barcode)
        providerTextView.text = item.provider
        deleteItemButton.text = item.deleteButtonText
        collapseLayout.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
        collapseIndicator.rotationX = if (item.isExpanded) 180F else 0F
        setupDynamicContent(item.topContent, topContent)
        setupDynamicContent(item.collapsedContent, collapsedContent)
    }

    fun setListeners(
        expandClickListener: View.OnClickListener? = null,
        deleteClickListener: View.OnClickListener? = null,
    ) {
        deleteClickListener?.let {
            deleteItemButton.setOnClickListener(it)
        }
        expandClickListener?.let {
            view.setOnClickListener(it)
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
            labelAndTextView = layoutInflater.inflate(
                R.layout.my_luca_label_and_text,
                container,
                false
            ) as ConstraintLayout
            container.addView(labelAndTextView)
        }
        val labelView = labelAndTextView.findViewById<TextView>(R.id.labelTextView)
        val textView = labelAndTextView.findViewById<TextView>(R.id.valueTextView)
        labelView.text = label
        textView.text = text
        setConstrainWidth(labelAndTextView, R.id.labelTextView, !TextUtils.isEmpty(text))
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
        for (i in 0 until max(topContent.childCount, content.size)) {
            val labelAndTextView = if (topContent.getChildAt(i) != null) {
                topContent.getChildAt(i) as ConstraintLayout
            } else {
                null
            }
            if (content.size > i) {
                val labelAndText = content[i]
                addLabelAndText(
                    topContent,
                    labelAndTextView,
                    labelAndText.first,
                    labelAndText.second
                )
            } else {
                topContent.removeView(labelAndTextView)
            }
        }
    }

}

