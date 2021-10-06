package de.culture4life.luca.ui.checkin

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.culture4life.luca.BuildConfig
import de.culture4life.luca.R
import de.culture4life.luca.databinding.BottomSheetQrCodeBinding


class QrCodeBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var sharedViewModel: QrCodeBottomSheetViewModel
    private lateinit var binding: BottomSheetQrCodeBinding
    private var qrCodeBitmap: Bitmap? = null
    private var isLoading = false
    private var isNetworkAvailable = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetStyle)
        sharedViewModel = ViewModelProvider(requireActivity()).get(QrCodeBottomSheetViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetQrCodeBinding.inflate(inflater)
        initializeViews()
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window?.attributes?.apply { screenBrightness = 1f }
        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            with(BottomSheetBehavior.from(bottomSheet!!)) {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        sharedViewModel.onQrCodeBottomSheetClosed()
        super.onDismiss(dialog)
    }

    private fun initializeViews() {
        qrCodeBitmap?.let {
            setQrCodeBitmap(it)
        }

        setIsLoading(isLoading)

        if (BuildConfig.DEBUG) {
            // simulate check in when clicking on the QR code in debug builds
            binding.qrCodeImageView.setOnClickListener { sharedViewModel.onDebuggingCheckInRequested() }
        }
        setNoNetworkWarningVisible(!isNetworkAvailable)
    }

    fun setQrCodeBitmap(bitmap: Bitmap) {
        qrCodeBitmap = bitmap
        if (!::binding.isInitialized) return
        binding.qrCodeImageView.setImageBitmap(bitmap)
        setIsLoading(false)
    }

    fun setIsLoading(isLoading: Boolean) {
        val showLoadingIndicator = isLoading || qrCodeBitmap == null
        binding.loadingLayout.isVisible = showLoadingIndicator
        binding.qrCodeImageView.isVisible = !showLoadingIndicator
    }

    fun setNoNetworkWarningVisible(isWarningVisible: Boolean) {
        if (!::binding.isInitialized) return
        binding.myQrCodeDescriptionTextView.isVisible = !isWarningVisible
        binding.noNetworkWarningTextView.isVisible = isWarningVisible
    }

    companion object {
        fun newInstance(
            qrCodeBitmap: Bitmap?,
            isLoading: Boolean?,
            isNetworkAvailable: Boolean?
        ): QrCodeBottomSheetFragment {
            return QrCodeBottomSheetFragment().apply {
                qrCodeBitmap?.let { this.qrCodeBitmap = it }
                isLoading?.let { this.isLoading = it }
                isNetworkAvailable?.let { this.isNetworkAvailable = it }
            }
        }
    }
}

