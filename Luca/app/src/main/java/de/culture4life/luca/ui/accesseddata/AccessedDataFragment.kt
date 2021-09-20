package de.culture4life.luca.ui.accesseddata

import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import de.culture4life.luca.R
import de.culture4life.luca.databinding.FragmentAccessedDataBinding
import de.culture4life.luca.ui.BaseFragment
import de.culture4life.luca.ui.history.HistoryFragment
import de.culture4life.luca.ui.history.HistoryFragment.NO_WARNING_LEVEL_FILTER
import io.reactivex.rxjava3.core.Completable

class AccessedDataFragment : BaseFragment<AccessedDataViewModel>() {

    companion object {
        const val KEY_TRACE_ID = "TraceId"
    }

    private lateinit var binding: FragmentAccessedDataBinding
    private lateinit var accessedDataListAdapter: AccessedDataListAdapter

    override fun getViewBinding(): ViewBinding {
        binding = FragmentAccessedDataBinding.inflate(layoutInflater)
        return binding
    }

    override fun getViewModelClass(): Class<AccessedDataViewModel> {
        return AccessedDataViewModel::class.java
    }

    override fun initializeViews(): Completable {
        return super.initializeViews()
            .andThen(Completable.fromAction {
                initializeAccessedDataItemsViews()
                initializeEmptyStateViews()
            })
    }

    private fun initializeAccessedDataItemsViews() {
        val traceId = arguments?.getString(KEY_TRACE_ID) ?: ""
        val warningLevel = arguments?.getInt(HistoryFragment.KEY_WARNING_LEVEL_FILTER, NO_WARNING_LEVEL_FILTER) ?: NO_WARNING_LEVEL_FILTER
        with(binding) {
            accessedDataListAdapter = AccessedDataListAdapter(requireContext(), accessedDataListView.id)
            accessedDataListView.adapter = accessedDataListAdapter
            accessedDataListView.setOnItemClickListener { _, _, position, _ ->
                val item = accessedDataListAdapter.getItem(position - accessedDataListView.headerViewsCount)
                val bundle = Bundle().apply {
                    putSerializable(AccessedDataDetailFragment.KEY_ACCESSED_DATA_LIST_ITEM, item)
                }
                safeNavigateFromNavController(R.id.action_accessedDataFragment_to_accessedDataDetailFragment, bundle)
            }
            observe(viewModel.accessedDataItems) { allItems ->
                val items = allItems
                    .filter { it.traceId.contains(traceId) }
                    .filter { warningLevel == NO_WARNING_LEVEL_FILTER || it.warningLevel == warningLevel }
                accessedDataListAdapter.setHistoryItems(items)
            }
        }
    }

    private fun initializeEmptyStateViews() {
        observe(viewModel.accessedDataItems) { items ->
            val emptyStateVisibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            val contentVisibility = if (items.isNotEmpty()) View.VISIBLE else View.GONE
            with(binding) {
                emptyTitleTextView.visibility = emptyStateVisibility
                emptyDescriptionTextView.visibility = emptyStateVisibility
                emptyImageView.visibility = emptyStateVisibility
                accessedDataListView.visibility = contentVisibility
            }
        }
    }

}