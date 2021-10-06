package de.culture4life.luca.ui.meeting

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import de.culture4life.luca.R
import de.culture4life.luca.databinding.FragmentMeetingDetailBinding
import de.culture4life.luca.ui.BaseFragment
import io.reactivex.rxjava3.core.Completable

class MeetingDetailFragment : BaseFragment<MeetingViewModel>() {

    private lateinit var binding: FragmentMeetingDetailBinding
    private var guestListAdapter: GuestListAdapter? = null

    override fun getViewModelClass(): Class<MeetingViewModel> {
        return MeetingViewModel::class.java
    }

    override fun getViewBinding(): ViewBinding {
        binding = FragmentMeetingDetailBinding.inflate(layoutInflater)
        return binding
    }

    override fun initializeViews(): Completable {
        return super.initializeViews()
            .andThen(Completable.fromAction {
                guestListAdapter = GuestListAdapter(viewModel.allGuests.value ?: emptyList())
                binding.guestsNamesRecyclerView.adapter = guestListAdapter
                binding.guestsNamesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.startTimeTextView.text = viewModel.startTime.value
                binding.guestsNumberTextView.text = getString(R.string.meeting_details_guests, getCheckedInGuestsCount(viewModel.allGuests.value))
                initializeObservers()
            })
    }

    private fun initializeObservers() {
        observe(viewModel.duration) { duration ->
            binding.durationTimeTextView.text = duration
        }
        observe(viewModel.startTime) { startTime ->
            binding.startTimeTextView.text = startTime
        }
        observe(viewModel.allGuests) { guests ->
            guestListAdapter?.setGuests(guests.filter { it.isCheckedIn })
            binding.guestsNumberTextView.text = getString(R.string.meeting_details_guests, getCheckedInGuestsCount(guests))
        }
    }

    private fun getCheckedInGuestsCount(guests: List<Guest>?): Int {
        return guests?.filter { it.isCheckedIn }?.size ?: 0
    }

}