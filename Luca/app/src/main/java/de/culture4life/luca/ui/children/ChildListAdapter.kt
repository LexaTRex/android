package de.culture4life.luca.ui.children

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.culture4life.luca.R
import de.culture4life.luca.children.Child
import io.reactivex.rxjava3.schedulers.Schedulers

class ChildListAdapter(
    context: Context,
    resource: Int,
    private val viewModel: ChildrenViewModel,
    private val darkStyle: Boolean,
    private val removeChildListener: (Child) -> Unit
) : ArrayAdapter<ChildListItem>(context, resource) {
    val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    fun setChildItems(items: List<ChildListItem>) {
        clear()
        addAll(items)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
        val view = convertView ?: layoutInflater.inflate(R.layout.child_list_item, container, false)
        getItem(position)?.let { childItem ->
            val checkBox = view.findViewById<CheckBox>(R.id.includeChildCheckBox)
            val nameTextView = view.findViewById<TextView>(R.id.childNameTextView)
            val removeChildImageView = view.findViewById<ImageView>(R.id.removeChildImageView)

            checkBox.apply {
                isChecked = childItem.isCheckedIn
                visibility = if (darkStyle) View.GONE else View.VISIBLE
                setOnClickListener {
                    viewModel.toggleCheckIn(childItem)
                        .onErrorComplete()
                        .subscribeOn(Schedulers.io())
                        .subscribe()
                }
            }
            nameTextView.apply {
                text = childItem.child.getFullName()
                setTextColor(if (darkStyle) Color.WHITE else Color.BLACK)
            }
            removeChildImageView.apply {
                if (darkStyle) {
                    val primaryColor = ContextCompat.getColor(context, R.color.primaryColor)
                    setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
                }
                setOnClickListener {
                    removeChildListener(childItem.child)
                }
            }
        }
        return view
    }
}