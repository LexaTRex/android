package de.culture4life.luca.ui.myluca.viewholders

import android.text.TextUtils
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import de.culture4life.luca.R
import de.culture4life.luca.databinding.MyLucaListItemBinding
import de.culture4life.luca.ui.myluca.MyLucaListItem
import kotlin.math.max

class SingleMyLucaItemViewHolder(val binding: MyLucaListItemBinding) : RecyclerView.ViewHolder(binding.root) {

    fun show(item: MyLucaListItem) {
        binding.cardView.setCardBackgroundColor(item.color)
        binding.itemTitleTextView.text = item.title
        binding.itemTitleImageView.setImageResource(item.imageResource)
        binding.qrCodeImageView.setImageBitmap(item.barcode)
        binding.providerTextView.text = item.provider
        binding.deleteItemButton.text = item.deleteButtonText
        binding.collapseLayout.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
        binding.collapseIndicator.rotationX = if (item.isExpanded) 180F else 0F
        setupDynamicContent(item.topContent, binding.topContent)
        setupDynamicContent(item.collapsedContent, binding.collapsedContent)
    }

    fun setListeners(
        expandClickListener: View.OnClickListener? = null,
        deleteClickListener: View.OnClickListener? = null,
    ) {
        deleteClickListener?.let {
            binding.deleteItemButton.setOnClickListener(it)
        }
        expandClickListener?.let {
            binding.root.setOnClickListener(it)
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

