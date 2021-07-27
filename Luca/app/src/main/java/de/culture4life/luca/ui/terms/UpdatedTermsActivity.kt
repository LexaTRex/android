package de.culture4life.luca.ui.terms

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.culture4life.luca.R
import de.culture4life.luca.databinding.ActivityUpdatedTermsBinding
import de.culture4life.luca.ui.BaseActivity
import de.culture4life.luca.ui.MainActivity
import de.culture4life.luca.ui.ViewError
import de.culture4life.luca.ui.dialog.BaseDialogFragment

/**
 * Shown to users that have agreed to the old terms and conditions and need to accept the new version.
 */
class UpdatedTermsActivity : BaseActivity() {

    private lateinit var viewModel: UpdatedTermsViewModel
    private lateinit var binding: ActivityUpdatedTermsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideActionBar()
        binding = ActivityUpdatedTermsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this).get(UpdatedTermsViewModel::class.java)

        viewModel.errors.observe(this, { errors ->
            errors.forEach { showErrorAsDialog(it) }
        })

        binding.primaryActionButton.setOnClickListener {
            activityDisposable.add(UpdatedTermsUtil.markTermsAsAccepted(application).subscribe {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            })
        }
        binding.updatedTermsDescription.movementMethod = LinkMovementMethod.getInstance()
        binding.menuImageView.setOnClickListener {
            PopupMenu(this, binding.menuImageView).apply {
                menuInflater.inflate(R.menu.updated_terms_menu, this.menu)
                setOnMenuItemClickListener { onMenuItemClick(it) }
                show()
            }
        }
    }

    private fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.showChangesMenuItem -> application.openUrl(getString(R.string.updated_terms_changes_url))
            R.id.deleteAccountMenuItem -> showDeleteAccountDialog()
            else -> return false
        }
        return true
    }

    private fun showDeleteAccountDialog() {
        BaseDialogFragment(MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_account_dialog_title)
            .setMessage(R.string.delete_account_dialog_message)
            .setPositiveButton(R.string.delete_account_dialog_action) { _, _ -> viewModel.deleteAccount() }
            .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.dismiss() })
            .show()
    }

    private fun showErrorAsDialog(error: ViewError) {
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(error.title)
            .setMessage(error.description)
            .setPositiveButton(R.string.action_ok) { _, _ -> }
        BaseDialogFragment(builder).apply {
            onDismissListener =
                DialogInterface.OnDismissListener { viewModel.onErrorDismissed(error) }
            show()
        }
        viewModel.onErrorShown(error)
    }
}