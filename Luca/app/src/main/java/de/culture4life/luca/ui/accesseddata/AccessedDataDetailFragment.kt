package de.culture4life.luca.ui.accesseddata

import androidx.viewbinding.ViewBinding
import de.culture4life.luca.databinding.FragmentAccessedDataDetailBinding
import de.culture4life.luca.ui.BaseFragment
import de.culture4life.luca.ui.MainActivity
import io.reactivex.rxjava3.core.Completable

class AccessedDataDetailFragment : BaseFragment<AccessedDataDetailViewModel>() {

    private lateinit var binding: FragmentAccessedDataDetailBinding

    override fun getViewBinding(): ViewBinding {
        binding = FragmentAccessedDataDetailBinding.inflate(layoutInflater)
        return binding
    }

    override fun getViewModelClass(): Class<AccessedDataDetailViewModel> {
        return AccessedDataDetailViewModel::class.java
    }

    override fun initializeViews(): Completable {
        return super.initializeViews()
            .andThen(Completable.fromAction {
                observe(viewModel.accessedDataItem) {
                    binding.headingTextView.text = it.title
                    binding.healthDepartmentTextView.text = it.accessorName
                    binding.locationTextView.text = it.locationName
                    binding.timeTextView.text = it.checkInTimeRange
                    binding.descriptionTextView.text = it.detailedMessage
                    viewModel.onItemSeen(it)
                    (activity as MainActivity).updateHistoryBadge() // TODO: 04.10.21 activity should observe changes
                }
            })
    }

    companion object {
        const val KEY_ACCESSED_DATA_LIST_ITEM = "AccessedDataListItem"
    }

}
