package de.culture4life.luca.ui.accesseddata

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.culture4life.luca.R
import de.culture4life.luca.databinding.AccessedDataListItemBinding

class AccessedDataListAdapter(context: Context, resource: Int) :
    ArrayAdapter<AccessedDataListItem>(context, resource) {

    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
            as LayoutInflater

    fun setHistoryItems(items: List<AccessedDataListItem>) {
        if (shouldUpdateDataSet(items)) {
            clear()
            addAll(items)
            notifyDataSetChanged()
        }
    }

    private fun shouldUpdateDataSet(items: List<AccessedDataListItem>): Boolean {
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
        val view = convertView ?: layoutInflater.inflate(R.layout.accessed_data_list_item, container, false)
        val binding = AccessedDataListItemBinding.bind(view)

        getItem(position)?.let { item ->
            binding.itemTitleTextView.text = item.title
            binding.itemDescriptionTextView.text = item.message
            binding.itemTimeTextView.text = item.accessTime
            val color = ContextCompat.getColor(
                context,
                if (item.isNew) R.color.highlightColor else android.R.color.white
            )
            binding.dotView.background.setTint(color)
        }
        return view
    }

}