package de.culture4life.luca.ui.history

import android.graphics.Color
import androidx.viewbinding.ViewBinding
import de.culture4life.luca.databinding.FragmentMeetingHistoryDetailBinding
import de.culture4life.luca.databinding.GuestListItemBinding
import de.culture4life.luca.ui.BaseFragment
import io.reactivex.rxjava3.core.Completable

class MeetingHistoryDetailFragment : BaseFragment<MeetingHistoryDetailViewModel>() {

    private lateinit var binding: FragmentMeetingHistoryDetailBinding

    override fun getViewBinding(): ViewBinding {
        binding = FragmentMeetingHistoryDetailBinding.inflate(layoutInflater)
        return binding
    }

    override fun getViewModelClass(): Class<MeetingHistoryDetailViewModel> {
        return MeetingHistoryDetailViewModel::class.java
    }

    override fun initializeViews(): Completable {
        return super.initializeViews()
            .andThen(Completable.fromAction {
                observe(viewModel.privateMeetingItem) {
                    binding.headingTextView.text = it.title
                    binding.subtitleTextView.text = it.subtitle
                    binding.descriptionTextView.text = it.description
                    binding.guestsTitleView.text = it.guestsTitle
                    binding.guestsListContainer.removeAllViews()
                    it.guests.forEachIndexed { index, name ->
                        with(GuestListItemBinding.inflate(layoutInflater)) {
                            guestNumberTextView.apply {
                                text = (index + 1).toString()
                                setTextColor(Color.WHITE)
                            }
                            guestNameTextView.apply {
                                text = name
                                setTextColor(Color.WHITE)
                            }
                            binding.guestsListContainer.addView(root)
                        }
                    }
                }
            })
    }

    companion object {
        const val KEY_PRIVATE_MEETING_ITEM = "PrivateMeetingItem"
    }

}
