package de.culture4life.luca.ui.checkin


import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.CheckBox
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.culture4life.luca.BuildConfig
import de.culture4life.luca.R
import de.culture4life.luca.databinding.FragmentCheckInBinding
import de.culture4life.luca.ui.BaseQrCodeFragment
import de.culture4life.luca.ui.BaseQrCodeViewModel.Companion.BARCODE_DATA_KEY
import de.culture4life.luca.ui.dialog.BaseDialogFragment
import de.culture4life.luca.ui.registration.RegistrationActivity
import de.culture4life.luca.util.addTo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.util.concurrent.TimeUnit


class CheckInFragment : BaseQrCodeFragment<CheckInViewModel>(), NavController.OnDestinationChangedListener {

    private var qrCodeBottomSheet: QrCodeBottomSheetFragment? = null
    private lateinit var qrCodeBottomSheetViewModel: QrCodeBottomSheetViewModel
    private lateinit var binding: FragmentCheckInBinding

    override fun getViewBinding(): ViewBinding {
        binding = FragmentCheckInBinding.inflate(layoutInflater)
        return binding
    }

    override fun getViewModelClass(): Class<CheckInViewModel> {
        return CheckInViewModel::class.java
    }

    override fun initializeViewModel(): Completable {
        return super.initializeViewModel()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete {
                viewModel.setupViewModelReference(requireActivity())
                qrCodeBottomSheetViewModel = ViewModelProvider(requireActivity())
                    .get(QrCodeBottomSheetViewModel::class.java)
            }
    }

    override fun initializeViews(): Completable {
        return super.initializeViews()
            .andThen(initializeQrCodeBottomSheet())
            .andThen(initializeMiscellaneous())
    }

    override fun initializeCameraPreview(): Completable {
        return super.initializeCameraPreview()
            .andThen(Completable.fromAction {
                cameraPreviewView = binding.cameraPreviewView
                binding.cameraContainerConstraintLayout.setOnClickListener {
                    showCameraPreview(requestConsent = true, requestPermission = true)
                }
                binding.flashLightButtonImageView.setOnClickListener { toggleTorch() }
            })
    }

    private fun initializeQrCodeBottomSheet(): Completable {
        return Completable.fromAction {
            binding.showQrCodeButton.setOnClickListener { showQrCodeBottomSheet() }
            observe(viewModel.qrCode) {
                qrCodeBottomSheet?.setQrCodeBitmap(it)
            }
            observe(qrCodeBottomSheetViewModel.onBottomSheetClosed) {
                if (!it.hasBeenHandled()) {
                    it.setHandled(true)
                    if (isAdded) {
                        showCameraPreview(requestConsent = false, requestPermission = false)
                    }
                }
            }
            observe(qrCodeBottomSheetViewModel.onDebuggingCheckInRequested) {
                if (!it.hasBeenHandled()) {
                    it.setHandled(true)
                    if (BuildConfig.DEBUG) {
                        viewModel.onDebuggingCheckInRequested()
                    }
                }
            }
        }
    }

    private fun initializeMiscellaneous(): Completable {
        return Completable.fromAction {
            observe(viewModel.bundle) { processBundle(it) }
            observe(viewModel.possibleDocumentData) {
                if (!it.hasBeenHandled()) {
                    showImportDocumentDialog(it.valueAndMarkAsHandled)
                }
            }
            observe(viewModel.isLoading) {
                qrCodeBottomSheet?.setIsLoading(it)
                binding.checkingInLoadingLayout.isVisible = it
            }
            observe(viewModel.isNetworkAvailable) {
                qrCodeBottomSheet?.setNoNetworkWarningVisible(!it)
            }
            observe(viewModel.isUpdateRequired) {
                if (it) {
                    showUpdateRequiredDialog()
                }
            }
            observe(viewModel.isContactDataMissing) {
                if (it) {
                    showContactDataMissingDialog()
                }
            }
            binding.createMeetingButton.setOnClickListener { showCreatePrivateMeetingDialog() }
            observe(viewModel.confirmPrivateMeeting) {
                if (!it.hasBeenHandled()) {
                    showJoinPrivateMeetingDialog(it.valueAndMarkAsHandled)
                }
            }
            binding.historyTextView.setOnClickListener {
                safeNavigateFromNavController(R.id.action_checkInFragment_to_history)
            }
            observe(viewModel.confirmCheckIn) {
                if (!it.hasBeenHandled()) {
                    val urlAndName = it.valueAndMarkAsHandled
                    showConfirmCheckInDialog(urlAndName.first, urlAndName.second)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.checkIfUpdateIsRequired()
    }

    override fun onResume() {
        super.onResume()
        hideKeyboard()
        viewModel.checkIfContactDataMissing()
        viewModel.checkIfHostingMeeting()
        arguments?.let { bundle -> viewModel.setBundle(bundle) }
        navigationController.addOnDestinationChangedListener(this)
    }

    override fun onPause() {
        navigationController.removeOnDestinationChangedListener(this)
        super.onPause()
    }

    override fun onStop() {
        viewModel.setBundle(null)
        super.onStop()
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        qrCodeBottomSheet?.dismiss()
    }

    private fun processBundle(bundle: Bundle?) {
        if (bundle == null) {
            return
        }
        bundle.getString(BARCODE_DATA_KEY)?.let { barcode ->
            viewModel.processBarcode(barcode)
                .delaySubscription(500, TimeUnit.MILLISECONDS) // avoid processing if checked in
                .doOnComplete { viewModel.setBundle(null) }
                .onErrorComplete()
                .subscribe()
                .addTo(viewDisposable)
        }
    }

    private fun showQrCodeBottomSheet() {
        hideCameraPreview()
        parentFragmentManager.let {
            qrCodeBottomSheet = QrCodeBottomSheetFragment.newInstance(
                qrCodeBitmap = viewModel.qrCode.value,
                isLoading = viewModel.isLoading.value,
                isNetworkAvailable = viewModel.isNetworkAvailable.value
            ).apply {
                show(it, tag)
            }
        }
    }

    private fun showConfirmCheckInDialog(url: String, locationName: String) {
        hideCameraPreview()
        val skipCheckInConfirmationView = View.inflate(
            ContextThemeWrapper(context, R.style.ThemeOverlay_Luca_AlertDialog),
            R.layout.layout_dont_ask_again,
            null
        )
        val skipCheckInConfirmationCheckBox = skipCheckInConfirmationView.findViewById<CheckBox>(R.id.dontAskAgainCheckbox)
        BaseDialogFragment(MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.venue_check_in_confirmation_title)
            .setView(skipCheckInConfirmationView)
            .setMessage(getString(R.string.venue_check_in_confirmation_description, locationName))
            .setPositiveButton(R.string.action_confirm) { _, _ ->
                viewModel.onCheckInConfirmationApproved(url, skipCheckInConfirmationCheckBox.isChecked)
            }
            .setNegativeButton(R.string.action_cancel) { _, _ -> })
            .apply {
                setOnDismissListener {
                    viewModel.onCheckInConfirmationDismissed(url, skipCheckInConfirmationCheckBox.isChecked)
                }
                show()
            }
    }

    private fun showJoinPrivateMeetingDialog(privateMeetingUrl: String) {
        hideCameraPreview()
        BaseDialogFragment(MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.meeting_join_heading)
            .setMessage(R.string.meeting_join_description)
            .setPositiveButton(R.string.action_ok) { _, _ ->
                viewModel.onPrivateMeetingJoinApproved(privateMeetingUrl)
            }
            .setNegativeButton(R.string.action_cancel) { _, _ -> })
            .apply {
                setOnDismissListener { viewModel.onPrivateMeetingJoinDismissed(privateMeetingUrl) }
                show()
            }
    }

    private fun showCreatePrivateMeetingDialog() {
        hideCameraPreview()
        BaseDialogFragment(MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.meeting_create_modal_heading)
            .setMessage(R.string.meeting_create_modal_description)
            .setPositiveButton(R.string.meeting_create_modal_action) { _, _ ->
                viewModel.onPrivateMeetingCreationRequested()
            }
            .setNegativeButton(R.string.action_cancel) { _, _ -> })
            .apply {
                setOnDismissListener { viewModel.onPrivateMeetingCreationDismissed() }
                show()
            }
    }

    private fun showImportDocumentDialog(documentData: String) {
        hideCameraPreview()
        BaseDialogFragment(MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.venue_check_in_document_redirect_title)
            .setMessage(R.string.venue_check_in_document_redirect_description)
            .setPositiveButton(R.string.action_continue) { _, _ ->
                val bundle = Bundle()
                bundle.putString(BARCODE_DATA_KEY, documentData)
                safeNavigateFromNavController(R.id.myLucaFragment, bundle)
            }
            .setNegativeButton(R.string.action_cancel) { _, _ -> })
            .apply {
                setOnDismissListener {
                    viewModel.onImportDocumentConfirmationDismissed()
                }
                show()
            }
    }

    private fun showContactDataMissingDialog() {
        hideCameraPreview()
        BaseDialogFragment(MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.registration_missing_info)
            .setMessage(R.string.registration_address_mandatory)
            .setPositiveButton(R.string.action_ok) { _, _ ->
                val intent = Intent(application, RegistrationActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                application.startActivity(intent)
            })
            .apply {
                isCancelable = false
                setOnDismissListener {
                    viewModel.onContactDataMissingDialogDismissed()
                }
                show()
            }
    }

    private fun showUpdateRequiredDialog() {
        hideCameraPreview()
        BaseDialogFragment(MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.update_required_title)
            .setMessage(R.string.update_required_description)
            .setPositiveButton(R.string.action_update) { _, _ ->
                try {
                    application.openUrl("market://details?id=de.culture4life.luca")
                } catch (e: ActivityNotFoundException) {
                    application.openUrl("https://luca-app.de")
                }
            })
            .apply {
                isCancelable = false
                setOnDismissListener {
                    viewModel.onUpdateRequiredDialogDismissed()
                }
                show()
            }
    }

    /*
        Camera
     */

    override fun bindCameraPreview(cameraProvider: ProcessCameraProvider) {
        super.bindCameraPreview(cameraProvider)
        binding.flashLightButtonImageView.isVisible = camera?.cameraInfo?.hasFlashUnit() == true
    }

    override fun setCameraPreviewVisible(isVisible: Boolean) {
        super.setCameraPreviewVisible(isVisible)
        binding.cameraContainerConstraintLayout.background = ContextCompat.getDrawable(
            requireContext(),
            if (isVisible) {
                R.drawable.bg_camera_box_active_preview
            } else {
                R.drawable.bg_camera_box
            }
        )
        binding.startCameraLinearLayout.isVisible = !isVisible
    }

    override fun setTorchEnabled(isEnabled: Boolean) {
        super.setTorchEnabled(isEnabled)
        binding.flashLightButtonImageView.setImageResource(
            if (isEnabled) {
                R.drawable.ic_flashlight_off
            } else {
                R.drawable.ic_flashlight_on
            }
        )
    }

}