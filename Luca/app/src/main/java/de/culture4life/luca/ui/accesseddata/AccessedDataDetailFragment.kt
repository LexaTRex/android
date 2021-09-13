package de.culture4life.luca.ui.accesseddata

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.culture4life.luca.LucaApplication
import de.culture4life.luca.databinding.FragmentAccessedDataDetailBinding
import de.culture4life.luca.ui.MainActivity
import io.reactivex.rxjava3.schedulers.Schedulers

class AccessedDataDetailFragment : Fragment() {

    private lateinit var item: AccessedDataListItem
    private lateinit var binding: FragmentAccessedDataDetailBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccessedDataDetailBinding.inflate(layoutInflater)
        item = arguments?.getSerializable(KEY_ACCESSED_DATA_LIST_ITEM) as AccessedDataListItem
        markItemAsNotNew()
        with(binding) {
            headingTextView.text = item.title
            healthDepartmentTextView.text = item.accessorName
            locationTextView.text = item.locationName
            timeTextView.text = item.checkInTimeRange
            descriptionTextView.text = item.detailedMessage
        }
        return binding.root
    }

    private fun markItemAsNotNew() {
        (activity as MainActivity?)?.let { activity ->
            (activity.application as LucaApplication?)?.let { application ->
                val dataAccessManager = application.dataAccessManager
                dataAccessManager.initialize(application)
                    .andThen(dataAccessManager.markAsNotNew(item.traceId, item.warningLevel))
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            }
            activity.updateHistoryBadge()
        }
    }

    companion object {
        const val KEY_ACCESSED_DATA_LIST_ITEM = "AccessedDataListItem"
    }

}
