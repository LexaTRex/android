package de.culture4life.luca.ui.myluca

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import de.culture4life.luca.ui.myluca.viewholders.SingleMyLucaItemViewHolder

class MyLucaItemForViewPagerFragment : Fragment() {

    private val viewModel by activityViewModels<MyLucaViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val itemViewHolder = SingleMyLucaItemViewHolder(SingleLucaItemView(requireContext()).binding)
        viewModel.myLucaItems.value?.let { myLucaItems ->
            arguments?.getString(KEY_DOCUMENT_ID)?.let { itemId ->
                myLucaItems.first { it.document.id == itemId }?.let { item ->
                    itemViewHolder.show(item)
                    itemViewHolder.setListeners(
                        { viewModel.onItemExpandToggleRequested(item) },
                        { viewModel.onItemDeletionRequested(item) })
                }
            }
        }
        return itemViewHolder.binding.root
    }

    companion object {
        const val KEY_DOCUMENT_ID = "DocumentId"

        fun newInstance(itemDocumentId: String): MyLucaItemForViewPagerFragment {
            return MyLucaItemForViewPagerFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(KEY_DOCUMENT_ID, itemDocumentId)
                }
            }
        }
    }

}



