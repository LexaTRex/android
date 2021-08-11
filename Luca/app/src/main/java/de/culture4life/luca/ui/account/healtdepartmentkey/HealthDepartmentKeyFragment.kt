package de.culture4life.luca.ui.account.healtdepartmentkey

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import de.culture4life.luca.R
import de.culture4life.luca.databinding.FragmentHealthDepartmentKeyBinding
import de.culture4life.luca.ui.BaseFragment
import de.culture4life.luca.ui.addTo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.util.*

class HealthDepartmentKeyFragment : BaseFragment<HealthDepartmentKeyViewModel>() {

    private lateinit var binding: FragmentHealthDepartmentKeyBinding

    override fun getLayoutResource(): Int = R.layout.fragment_health_department_key
    override fun getViewModelClass(): Class<HealthDepartmentKeyViewModel> =
        HealthDepartmentKeyViewModel::class.java

    var getActivityResult: ActivityResultLauncher<Intent>? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        getActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                it.data?.data?.also { uri ->
                    viewModel.writeContentToUri(uri)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .onErrorComplete()
                        .subscribe()
                        .addTo(viewDisposable)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentHealthDepartmentKeyBinding.bind(view)
        return binding.root
    }

    override fun initializeViews(): Completable {
        return super.initializeViews().andThen {
            initializeObservers()
            viewModel.getDailyKeyPairAndVerify()
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
                .addTo(viewDisposable)
        }
    }

    private fun initializeObservers() {
        viewModel.dailyKeyPairLiveData.observe(viewLifecycleOwner, {
            updateCreatedAt(it.dailyKeyPair.createdAt)
            updateIssuerName(it.issuer.name)
        })

        viewModel.hasVerifiedDailyKeyPair.observe(viewLifecycleOwner, {
            updateSignedIndicator(it)
        })

        viewModel.isLoading.observe(viewLifecycleOwner, { isLoading ->
            updateLoadingView(isLoading)
        })

        binding.saveKeyMaterialButton.setOnClickListener {
            launchSaveAsIntent()
        }
    }

    private fun launchSaveAsIntent() {
        val intent = Intent().apply {
            action = Intent.ACTION_CREATE_DOCUMENT
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "transferKeyChain.txt")
        }

        getActivityResult?.launch(intent)
    }

    private fun updateLoadingView(isLoading: Boolean) {
        if (isLoading) {
            binding.contentGroup.visibility = View.INVISIBLE
            binding.loadingProgressBar.visibility = View.VISIBLE
        } else {
            binding.contentGroup.visibility = View.VISIBLE
            binding.loadingProgressBar.visibility = View.INVISIBLE
        }
    }

    private fun updateCreatedAt(createdAt: Long) {
        val readableDateFormat =
            SimpleDateFormat(application.getString(R.string.time_format), Locale.GERMANY)
        binding.dateValueTextView.text = readableDateFormat.format(Date(createdAt * 1000))
    }

    private fun updateIssuerName(name: String) {
        binding.issuerValueTextView.text = name
    }

    private fun updateSignedIndicator(isSigned: Boolean) {
        if (isSigned) {
            binding.signedImageView.setImageResource(R.drawable.ic_key_signed)
        } else {
            binding.signedImageView.setImageResource(R.drawable.ic_key_unsigned)
        }
    }
}