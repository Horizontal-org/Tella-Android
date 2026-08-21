package org.horizontal.tella.mobile.views.activity

import android.os.Bundle
import android.view.View
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.ActivityLockUpdateSuccessBinding
import org.horizontal.tella.mobile.views.base_ui.BaseActivity
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils

class LockUpdateSuccessActivity : BaseActivity() {

    private lateinit var binding: ActivityLockUpdateSuccessBinding
    private var hasShownSheet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockUpdateSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdge(binding.root)
        binding.doneBtn.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        when {
            binding.successContent.visibility == View.VISIBLE -> { /* already showing success */ }
            !hasShownSheet -> {
                hasShownSheet = true
                showProtectYourLockSheet()
            }
            else -> binding.successContent.visibility = View.VISIBLE
        }
    }

    private fun showProtectYourLockSheet() {
        BottomSheetUtils.showStandardSheet(
            fragmentManager = supportFragmentManager,
            titleText = getString(R.string.onboard_lock_protect_sheet_title),
            descriptionText = getString(R.string.onboard_lock_protect_sheet_message),
            actionButtonLabel = getString(R.string.onboard_lock_protect_sheet_continue),
            cancelButtonLabel = null,
            onConfirmClick = { binding.successContent.visibility = View.VISIBLE },
            onCancelClick = null
        )
    }
}
