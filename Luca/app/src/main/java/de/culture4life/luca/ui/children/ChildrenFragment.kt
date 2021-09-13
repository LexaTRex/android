package de.culture4life.luca.ui.children

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.culture4life.luca.R
import de.culture4life.luca.children.Child
import de.culture4life.luca.databinding.FragmentAddingChildrenBinding
import de.culture4life.luca.ui.BaseFragment
import de.culture4life.luca.ui.UiUtil
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers

class ChildrenFragment : BaseFragment<ChildrenViewModel>(),
    AddChildDialogFragment.AddChildListener {

    private var bottomSheet: AddChildDialogFragment? = null
    private lateinit var binding: FragmentAddingChildrenBinding
    private lateinit var childListAdapter: ChildListAdapter
    private var darkStyle = true

    override fun getViewModelClass(): Class<ChildrenViewModel> = ChildrenViewModel::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            darkStyle = !it.getBoolean(CHECK_IN, false)
        }
    }

    override fun getViewBinding(): ViewBinding {
        binding = FragmentAddingChildrenBinding.inflate(layoutInflater)
        setupDesign()
        return binding
    }

    private fun setupDesign() {
        if (darkStyle) {
            with(binding) {
                layout.setBackgroundColor(Color.BLACK)
                backImageView.setColorFilter(Color.WHITE)
                headingTextView.setTextColor(Color.WHITE)
                childAddingDescriptionTextView.setTextColor(Color.WHITE)
                primaryActionButton.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.primaryColor)
                emptyImageView.setImageResource(R.drawable.g_child_light)
            }
        }
    }


    override fun initializeViews(): Completable {
        return super.initializeViews()
            .andThen(Completable.fromAction {
                initializeChildItemsViews()
                initializeAddChildViews()
            })
    }

    override fun onResume() {
        super.onResume()
        viewModel.restoreChildren().subscribe()
    }

    private fun initializeAddChildViews() {
        with(binding) {
            backImageView.setOnClickListener { viewModel.navigateBack() }
            primaryActionButton.setOnClickListener { showAddChildDialog() }
        }
    }

    private fun initializeChildItemsViews() {
        childListAdapter = ChildListAdapter(
            requireContext(),
            binding.childListView.id,
            viewModel,
            darkStyle,
            { showRemoveChildDialog(it) },
            { showAddChildDialog() }
        )
        binding.childListView.adapter = childListAdapter
        View(context).let { paddingView ->
            paddingView.minimumHeight = UiUtil.convertDpToPixel(16f, context).toInt()
            binding.childListView.addHeaderView(paddingView)
        }
        observe(viewModel.children, this@ChildrenFragment::updateChildItemsList)
    }

    private fun showAddChildDialog() {
        parentFragmentManager.let {
            bottomSheet = AddChildDialogFragment.newInstance(this).apply {
                show(it, tag)
            }
        }
    }

    private fun showRemoveChildDialog(child: Child) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.venue_children_remove_confirmation_title)
            .setMessage(R.string.venue_children_remove_confirmation_description)
            .setNegativeButton(R.string.action_no) { _, _ -> }
            .setPositiveButton(R.string.action_yes) { _, _ ->
                viewModel.removeChild(child)
                    .onErrorComplete()
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            }.create()
            .show()
    }

    private fun updateChildItemsList(children: ChildListItemContainer) {
        with(binding) {
            headingTextView.setText(R.string.add_children_title)
            childAddingDescriptionTextView.setText(if (children.isEmpty()) R.string.venue_children_empty_list_description else R.string.venue_children_list_description)
            childListView.isVisible = !children.isEmpty()
            emptyImageView.isVisible = children.isEmpty()
            primaryActionButton.isVisible = children.isEmpty()
        }
        childListAdapter.setChildItems(children)
    }

    companion object {
        const val CHECK_IN = "checkIn"
    }

    override fun addChild(child: Child) {
        viewDisposable.add(
            viewModel.addChild(child)
                .onErrorComplete()
                .doFinally { bottomSheet?.dismiss() }
                .subscribe())
    }

}