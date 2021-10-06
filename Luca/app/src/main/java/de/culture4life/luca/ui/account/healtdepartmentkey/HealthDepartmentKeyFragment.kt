package de.culture4life.luca.ui.account.healtdepartmentkey

import android.view.View
import androidx.viewbinding.ViewBinding
import de.culture4life.luca.R
import de.culture4life.luca.databinding.FragmentHealthDepartmentKeyBinding
import de.culture4life.luca.ui.BaseFragment
import de.culture4life.luca.util.addTo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.joda.time.format.DateTimeFormat

class HealthDepartmentKeyFragment : BaseFragment<HealthDepartmentKeyViewModel>() {

    private lateinit var binding: FragmentHealthDepartmentKeyBinding

    override fun getViewBinding(): ViewBinding? {
        binding = FragmentHealthDepartmentKeyBinding.inflate(layoutInflater)
        return binding;
    }

    override fun getViewModelClass(): Class<HealthDepartmentKeyViewModel> =
        HealthDepartmentKeyViewModel::class.java

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
            viewModel.exportDailyKey(getFileExportUri("luca-daily-key-chain.txt"))
        }
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
            DateTimeFormat.forPattern(application.getString(R.string.time_format))
        binding.dateValueTextView.text = readableDateFormat.print(createdAt * 1000)
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