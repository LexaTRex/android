package de.culture4life.luca.ui.children

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputLayout
import de.culture4life.luca.R
import de.culture4life.luca.children.Child
import de.culture4life.luca.children.ChildrenManager
import de.culture4life.luca.databinding.ChildNameDialogBinding
import android.widget.FrameLayout
import androidx.core.widget.doOnTextChanged


class AddChildDialogFragment : BottomSheetDialogFragment() {

    interface AddChildListener {
        fun addChild(child: Child)
    }

    private lateinit var listener: AddChildListener
    private lateinit var binding: ChildNameDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetStyle)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            with (BottomSheetBehavior.from(bottomSheet!!)) {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ChildNameDialogBinding.inflate(inflater)
        setTextChangedListener(binding.firstNameLayout)
        setTextChangedListener(binding.lastNameLayout)
        binding.primaryActionButton.setOnClickListener { validateChildName() }
        return binding.root
    }

    private fun setTextChangedListener(textInputLayout: TextInputLayout) {
        textInputLayout.editText?.doOnTextChanged { text, _, _, _ ->
            if (textInputLayout.error != null) {
                textInputLayout.setErrorStateFor(text.toString())
            }
        }
    }

    private fun validateChildName() {
        val firstName = binding.firstNameLayout.editText!!.text.toString()
        val lastName = binding.lastNameLayout.editText!!.text.toString()
        binding.firstNameLayout.setErrorStateFor(firstName)
        binding.lastNameLayout.setErrorStateFor(lastName)
        val child = Child(firstName, lastName)
        if (ChildrenManager.isValidChildName(child)) {
            listener.addChild(child)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(listener: AddChildListener): AddChildDialogFragment {
            return AddChildDialogFragment().apply {
                this.listener = listener
            }
        }
    }
}

fun TextInputLayout.setErrorStateFor(name: String) {
    error = if (!ChildrenManager.isValidChildName(name)) {
        context.getString(R.string.venue_children_add_validation_error)
    } else {
        null
    }
}