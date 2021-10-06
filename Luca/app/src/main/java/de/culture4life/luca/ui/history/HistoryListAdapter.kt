package de.culture4life.luca.ui.history

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import de.culture4life.luca.R
import de.culture4life.luca.databinding.HistoryListItemBinding

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
        // for smooth scrolling
        val binding = if (convertView == null) {
            HistoryListItemBinding.inflate(layoutInflater)
        } else {
            HistoryListItemBinding.bind(convertView)
        }

        binding.root.setOnClickListener(null)

        val item = getItem(position)!!

        binding.topLineView.visibility = if (showTimeLine && position > 0) View.VISIBLE else View.GONE
        binding.bottomLineView.visibility = if (showTimeLine && position < count - 1) View.VISIBLE else View.GONE
        binding.itemTitleTextView.text = item.title
        binding.itemDescriptionTextView.text = item.description
        binding.itemDescriptionTextView.visibility = if (item.description != null) View.VISIBLE else View.GONE

        with(binding.itemTitleImageView) {
            if (item.additionalTitleDetails != null) {
                setImageResource(item.titleIconResourceId)
                if (item.accessedTraceData.isNotEmpty()) {
                    binding.root.setOnClickListener { itemClickHandler?.showAccessedDataDetails(item) }
                    visibility = View.VISIBLE
                } else if (item.isPrivateMeeting) {
                    binding.root.setOnClickListener { itemClickHandler?.showPrivateMeetingDetails(item) }
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
                binding.root.setOnLongClickListener {
                    itemClickHandler?.showTraceInformation(item)
                    return@setOnLongClickListener true
                }
            } else {
                visibility = View.GONE
                binding.root.setOnLongClickListener(null)
            }
        }

        binding.itemTimeTextView.text = item.time

        val isNew = item.accessedTraceData.any { it.isNew }
        val color = ContextCompat.getColor(
            context,
            if (isNew) R.color.highlightColor else android.R.color.white
        )
        binding.itemTitleTextView.setTextColor(color)
        binding.dotView.background.setTint(color)
        binding.itemTitleImageView.setColorFilter(color)

        return binding.root
    }

    interface ItemClickHandler {
        fun showAccessedDataDetails(item: HistoryListItem)
        fun showTraceInformation(item: HistoryListItem)
        fun showPrivateMeetingDetails(item: HistoryListItem)
    }

}