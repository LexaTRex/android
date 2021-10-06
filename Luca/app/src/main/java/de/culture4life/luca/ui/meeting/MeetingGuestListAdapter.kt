package de.culture4life.luca.ui.meeting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.culture4life.luca.R

class GuestListAdapter(
    private var guestsList: List<Guest>
) : RecyclerView.Adapter<GuestListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val guestNumberTextView: TextView = view.findViewById(R.id.guestNumberTextView)
        val guestNameTextView: TextView = view.findViewById(R.id.guestNameTextView)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.guest_list_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.guestNumberTextView.text = (position + 1).toString()
        viewHolder.guestNameTextView.text = guestsList[position].name
    }

    override fun getItemCount(): Int = guestsList.size

    fun setGuests(list: List<Guest>) {
        if (shouldUpdateDataSet(list)) {
            guestsList = list
            notifyDataSetChanged()
        }
    }

    private fun shouldUpdateDataSet(list: List<Guest>): Boolean {
        if (list.size != itemCount) {
            return true
        }

        for (itemIndex in 0 until itemCount) {
            if (!list.contains(guestsList[itemIndex])) {
                return true
            }
        }
        return false
    }
}

data class Guest(val name: String, val isCheckedIn: Boolean)