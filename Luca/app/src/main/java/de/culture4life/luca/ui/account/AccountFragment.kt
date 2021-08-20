package de.culture4life.luca.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.culture4life.luca.BuildConfig
import de.culture4life.luca.R
import de.culture4life.luca.R.layout
import de.culture4life.luca.databinding.FragmentAccountBinding
import de.culture4life.luca.ui.BaseFragment
import de.culture4life.luca.ui.dialog.BaseDialogFragment
import de.culture4life.luca.ui.registration.RegistrationActivity
import io.reactivex.rxjava3.core.Completable

class AccountFragment : BaseFragment<AccountViewModel>() {

    override fun getLayoutResource(): Int = layout.fragment_account
    override fun getViewModelClass(): Class<AccountViewModel> = AccountViewModel::class.java

    private lateinit var binding: FragmentAccountBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentAccountBinding.bind(view)
        return binding.root
    }

    override fun initializeViews(): Completable {
        return super.initializeViews()
            .andThen {
                initializeOnClickListeners()
            }
    }

    private fun initializeOnClickListeners() {
        view?.also {
            binding.editContactDataTextView.setOnClickListener {
                val intent = Intent(activity, RegistrationActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

            binding.deleteAccountTextView.setOnClickListener { showDeleteAccountDialog() }
            binding.faqTextView.setOnClickListener { application.openUrl(getString(R.string.url_faq)) }
            binding.supportTextView.setOnClickListener { viewModel.requestSupportMail() }
            binding.privacyTextView.setOnClickListener { application.openUrl(getString(R.string.url_privacy_policy)) }
            binding.termsTextView.setOnClickListener { application.openUrl(getString(R.string.url_terms_and_conditions)) }
            binding.imprintTextView.setOnClickListener { application.openUrl(getString(R.string.url_imprint)) }
            binding.showAppDataTextView.setOnClickListener { application.openAppSettings() }
            binding.versionTextView.setOnClickListener { showVersionDetailsDialog() }
            binding.gitlabTextView.setOnClickListener { showGitlabDialog() }
            binding.healthDepartmentKeyTextView.setOnClickListener { viewModel.openHealthDepartmentKeyView() }
        }
    }

    private fun showDeleteAccountDialog() {
        context?.also {
            BaseDialogFragment(MaterialAlertDialogBuilder(it)
                .setTitle(R.string.delete_account_dialog_title)
                .setMessage(R.string.delete_account_dialog_message)
                .setPositiveButton(R.string.delete_account_dialog_action) { _, _ -> viewModel.deleteAccount() }
                .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.dismiss() })
                .show()
        }
    }

    private fun showVersionDetailsDialog() {
        context?.also {
            var commitHash = BuildConfig.COMMIT_HASH
            if (commitHash.length > 8 && !commitHash.startsWith("<")) {
                commitHash = commitHash.substring(0, 8)
            }
            val message = getString(
                R.string.version_details_dialog_message,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                commitHash
            )
            BaseDialogFragment(MaterialAlertDialogBuilder(it)
                .setTitle(R.string.version_details_dialog_title)
                .setMessage(message)
                .setPositiveButton(R.string.version_details_dialog_action) { dialog, _ -> dialog.dismiss() })
                .show()
        }
    }

    private fun showGitlabDialog() {
        context?.let {
            BaseDialogFragment(MaterialAlertDialogBuilder(it)
                .setTitle(R.string.gitlab_dialog_title)
                .setMessage(R.string.gitlab_dialog_message)
                .setPositiveButton(R.string.gitlab_dialog_action) { _, _ -> application.openUrl(getString(R.string.url_gitlab)) }
                .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.dismiss() })
                .show()
        }
    }
}