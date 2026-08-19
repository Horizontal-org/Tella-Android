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

private const val TELLA_LOCK_DOCS_URL = "https://tella-app.org/features?_highlight=lock#app-lock"

class OnBoardLockSuccessFragment : BaseFragment() {

    private lateinit var binding: OnboardLockSuccessFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = OnboardLockSuccessFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun applyEdgeToEdgeIfNeeded(view: View) {
        OnboardingInsets.applyFullscreenOverlay(view, baseActivity)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
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
