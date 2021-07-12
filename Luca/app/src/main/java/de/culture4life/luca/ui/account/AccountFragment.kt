package de.culture4life.luca.ui.account

import android.content.Intent
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.culture4life.luca.BuildConfig
import de.culture4life.luca.R
import de.culture4life.luca.R.layout
import de.culture4life.luca.ui.BaseFragment
import de.culture4life.luca.ui.dialog.BaseDialogFragment
import de.culture4life.luca.ui.registration.RegistrationActivity
import io.reactivex.rxjava3.core.Completable

class AccountFragment : BaseFragment<AccountViewModel>() {

    private lateinit var editContactData: TextView
    private lateinit var deleteAccount: TextView
    private lateinit var faq: TextView
    private lateinit var support: TextView
    private lateinit var privacyPolicy: TextView
    private lateinit var termsOfUse: TextView
    private lateinit var imprint: TextView
    private lateinit var showAppData: TextView
    private lateinit var versionDetails: TextView
    private lateinit var gitlab: TextView

    override fun getLayoutResource(): Int = layout.fragment_account
    override fun getViewModelClass(): Class<AccountViewModel> = AccountViewModel::class.java

    override fun initializeViews(): Completable {
        return super.initializeViews()
                .andThen {
                    initOnClickListeners()
                }
    }

    private fun initOnClickListeners() {
        view?.also {
            editContactData = it.findViewById(R.id.editContactDataTextView)
            deleteAccount = it.findViewById(R.id.deleteAccountTextView)
            faq = it.findViewById(R.id.faqTextView)
            support = it.findViewById(R.id.supportTextView)
            privacyPolicy = it.findViewById(R.id.privacyTextView)
            termsOfUse = it.findViewById(R.id.termsTextView)
            imprint = it.findViewById(R.id.imprintTextView)
            showAppData = it.findViewById(R.id.showAppDataTextView)
            versionDetails = it.findViewById(R.id.versionTextView)
            gitlab = it.findViewById(R.id.gitlabTextView)

            editContactData.setOnClickListener {
                val intent = Intent(activity, RegistrationActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

            deleteAccount.setOnClickListener { showDeleteAccountDialog() }
            faq.setOnClickListener { application.openUrl(getString(R.string.url_faq)) }
            support.setOnClickListener { viewModel.requestSupportMail() }
            privacyPolicy.setOnClickListener { application.openUrl(getString(R.string.url_privacy_policy)) }
            termsOfUse.setOnClickListener { application.openUrl(getString(R.string.url_terms_and_conditions)) }
            imprint.setOnClickListener { application.openUrl(getString(R.string.url_imprint)) }
            showAppData.setOnClickListener { application.openAppSettings() }
            versionDetails.setOnClickListener { showVersionDetailsDialog() }
            gitlab.setOnClickListener { application.openUrl(getString(R.string.url_gitlab)) }
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
}