package de.culture4life.luca.ui.children

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import de.culture4life.luca.R
import de.culture4life.luca.children.Child
import de.culture4life.luca.databinding.ChildListItemBinding
import io.reactivex.rxjava3.schedulers.Schedulers

const val CHILD_NAME_VIEW_TYPE = 0
const val ADD_CHILD_BUTTON_VIEW_TYPE = 1

class ChildListAdapter(
    context: Context,
    resource: Int,
    private val viewModel: ChildrenViewModel,
    private val darkStyle: Boolean,
    private val removeChildListener: (Child) -> Unit,
    private val addChildListener: View.OnClickListener
) : ArrayAdapter<ChildListItem>(context, resource) {

    val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    override fun getCount(): Int {
        return super.getCount() + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == count - 1) ADD_CHILD_BUTTON_VIEW_TYPE else CHILD_NAME_VIEW_TYPE
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    fun setChildItems(items: List<ChildListItem>) {
        clear()
        addAll(items)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
        return if (getItemViewType(position) == ADD_CHILD_BUTTON_VIEW_TYPE) getAddChildButtonView(
            convertView,
            container
        ) else getChildView(position, convertView, container)
    }

    private fun getChildView(position: Int, convertView: View?, container: ViewGroup): View {
        // for smooth scrolling
        val binding = if (convertView == null) {
            ChildListItemBinding.inflate(layoutInflater)
        } else {
            ChildListItemBinding.bind(convertView)
        }

        getItem(position)?.let { childItem ->
            binding.includeChildCheckBox.apply {
                isChecked = childItem.isCheckedIn
                isVisible = !darkStyle
                setOnClickListener {
                    toggleCheckIn(childItem)
                }
            }

            binding.childNameTextView.apply {
                text = childItem.child.getFullName()
                setTextColor(if (darkStyle) Color.WHITE else Color.BLACK)
                if (!darkStyle) {
                    setOnClickListener {
                        toggleCheckIn(childItem)
                        binding.includeChildCheckBox.toggle()

                    }
                }
            }

            binding.removeChildImageView.apply {
                setOnClickListener {
                    removeChildListener(childItem.child)
                }
            }
        }
        return binding.root
    }

    private fun getAddChildButtonView(
        convertView: View?,
        container: ViewGroup
    ): View {
        val view =
            convertView ?: layoutInflater.inflate(R.layout.add_child_button, container, false)

        val addChildTextView = view.findViewById<TextView>(R.id.addChildTextView)
        val addChildLinearLayout = view.findViewById<LinearLayout>(R.id.addChildLayout)

        if (darkStyle) {
            addChildTextView.setTextColor(Color.WHITE)
        }

        addChildLinearLayout.setOnClickListener(addChildListener)
        return view
    }

    private fun toggleCheckIn(childItem: ChildListItem) {
        viewModel.toggleCheckIn(childItem)
            .onErrorComplete()
            .subscribeOn(Schedulers.io())
            .subscribe()
    }
}