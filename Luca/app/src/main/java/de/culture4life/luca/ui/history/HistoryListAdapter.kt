package de.culture4life.luca.ui.history

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.culture4life.luca.R

class HistoryListAdapter(context: Context, resource: Int, private val showTimeLine: Boolean) :
    ArrayAdapter<HistoryListItem>(context, resource) {

    var itemClickHandler: ItemClickHandler? = null
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
            as LayoutInflater

    fun setHistoryItems(items: List<HistoryListItem>) {
        if (shouldUpdateDataSet(items)) {
            clear()
            addAll(items)
            notifyDataSetChanged()
        }
    }

    private fun shouldUpdateDataSet(items: List<HistoryListItem>): Boolean {
        if (items.size != count) {
            return true
        }
        for (itemIndex in 0 until count) {
            if (!items.contains(getItem(itemIndex))) {
                return true
            }
        }
        return false
    }

    override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
        val view = convertView ?: layoutInflater.inflate(
            R.layout.history_list_item, container, false
        )

        val item = getItem(position)!!
        val dotView = view.findViewById<View>(R.id.dotView)
        val topLineView = view.findViewById<View>(R.id.topLineView)
        val bottomLineView = view.findViewById<View>(R.id.bottomLineView)
        val titleTextView = view.findViewById<TextView>(R.id.itemTitleTextView)
        val descriptionTextView = view.findViewById<TextView>(R.id.itemDescriptionTextView)
        val titleImageView = view.findViewById<ImageView>(R.id.itemTitleImageView)
        val descriptionImageView = view.findViewById<ImageView>(R.id.itemDescriptionImageView)
        val timeTextView = view.findViewById<TextView>(R.id.itemTimeTextView)

        topLineView.visibility = if (showTimeLine && position > 0) View.VISIBLE else View.GONE
        bottomLineView.visibility = if (showTimeLine && position < count - 1) View.VISIBLE else View.GONE
        titleTextView.text = item.title
        descriptionTextView.text = item.description
        descriptionTextView.visibility = if (item.description != null) View.VISIBLE else View.GONE
        with(titleImageView) {
            if (item.additionalTitleDetails != null) {
                setImageResource(item.titleIconResourceId)
                if (item.accessedTraceData.isNotEmpty()) {
                    view.setOnClickListener { itemClickHandler?.showAccessedDataDetails(item) }
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
                view.setOnLongClickListener {
                    itemClickHandler?.showTraceInformation(item)
                    return@setOnLongClickListener true
                }
            } else {
                visibility = View.GONE
                view.setOnClickListener(null)
            }
        }
        with(descriptionImageView) {
            if (item.additionalDescriptionDetails != null) {
                setImageResource(item.descriptionIconResourceId)
                visibility = View.VISIBLE
                setOnClickListener { itemClickHandler?.showAdditionalDescriptionDetails(item) }
                descriptionTextView.setOnClickListener {
                    itemClickHandler?.showAdditionalDescriptionDetails(item)
                }
            } else {
                visibility = View.GONE
                setOnClickListener(null)
                descriptionTextView.setOnClickListener(null)
            }
        }
        timeTextView.text = item.time

        val isNew = item.accessedTraceData.any { it.isNew }
        val color = ContextCompat.getColor(
            context,
            if (isNew) R.color.highlightColor else android.R.color.white
        )
        titleTextView.setTextColor(color)
        dotView.background.setTint(color)
        titleImageView.setColorFilter(color)

        return view
    }

    interface ItemClickHandler {
        fun showAccessedDataDetails(item: HistoryListItem)
        fun showAdditionalDescriptionDetails(item: HistoryListItem)
        fun showTraceInformation(item: HistoryListItem)
    }

}