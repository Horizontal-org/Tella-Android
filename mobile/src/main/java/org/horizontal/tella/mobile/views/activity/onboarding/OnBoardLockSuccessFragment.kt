package org.horizontal.tella.mobile.views.activity.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.OnboardLockSuccessFragmentBinding
import org.horizontal.tella.mobile.views.base_ui.BaseFragment
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils

private const val TELLA_LOCK_DOCS_URL = "https://tella-app.org/features?_highlight=lock#app-lock"

class OnBoardLockSuccessFragment : BaseFragment() {

    private lateinit var binding: OnboardLockSuccessFragmentBinding
    private var protectSheetShown = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = OnboardLockSuccessFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Keep success content hidden until user taps "Continue" on the bottom sheet
        binding.successContent.visibility = View.GONE
        initView(view)
    }

    override fun onResume() {
        super.onResume()
        when {
            binding.successContent.visibility == View.VISIBLE -> { /* already showing success */ }
            !protectSheetShown -> {
                protectSheetShown = true
                showProtectYourLockSheet()
            }
            else -> binding.successContent.visibility = View.VISIBLE
        }
    }

    override fun initView(view: View) {
        (baseActivity as OnBoardActivityInterface).apply {
            enableSwipe(isSwipeable = false, isTabLayoutVisible = false)
            showButtons(isNextButtonVisible = false, isBackButtonVisible = false)
        }

        binding.learnMoreLink.setOnClickListener {
            openLockDocs()
        }
        binding.nextBtn.setOnClickListener {
            goToLockSetFragment()
        }
    }

    private fun showProtectYourLockSheet() {
        BottomSheetUtils.showStandardSheet(
            fragmentManager = baseActivity.supportFragmentManager,
            titleText = getString(R.string.onboard_lock_protect_sheet_title),
            descriptionText = getString(R.string.onboard_lock_protect_sheet_message),
            actionButtonLabel = getString(R.string.onboard_lock_protect_sheet_continue),
            cancelButtonLabel = null,
            onConfirmClick = {
                binding.successContent.visibility = View.VISIBLE
            },
            onCancelClick = null
        )
    }

    private fun openLockDocs() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, TELLA_LOCK_DOCS_URL.toUri()))
        } catch (_: Exception) { }
    }

    private fun goToLockSetFragment() {
        baseActivity.addFragment(
            this,
            OnBoardAllDoneFragment(),
            R.id.rootOnboard
        )
    }
}
