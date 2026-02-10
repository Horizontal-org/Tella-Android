package org.horizontal.tella.mobile.views.activity.onboarding

import androidx.activity.OnBackPressedCallback
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.ui.password.SetPasswordActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternSetActivity
import com.hzontal.tella_locking_ui.ui.pin.SetPinActivity
import org.horizontal.tella.mobile.databinding.OnboardLockWarningFragmentBinding
import org.horizontal.tella.mobile.views.base_ui.BaseFragment

class OnBoardLockWarningFragment : BaseFragment() {

    private lateinit var binding: OnboardLockWarningFragmentBinding
    private var selectedLockType: LockType = LockType.PASSWORD
    private var isFromSettings: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val typeName = it.getString(ARG_LOCK_TYPE, LockType.PASSWORD.name)
            selectedLockType = runCatching { LockType.valueOf(typeName) }.getOrDefault(LockType.PASSWORD)
            isFromSettings = it.getBoolean(IS_FROM_SETTINGS, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = OnboardLockWarningFragmentBinding.inflate(inflater, container, false)
        return binding.root
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

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (baseActivity as OnBoardActivityInterface).showViewpager()
                    baseActivity.supportFragmentManager.popBackStack()
                }
            }
        )

        binding.backBtn.setOnClickListener {
            (baseActivity as OnBoardActivityInterface).showViewpager()
            baseActivity.supportFragmentManager.popBackStack()
        }
        binding.understandBtn.setOnClickListener {
            startSelectedLockSetup()
        }
    }

    private fun startSelectedLockSetup() {
        val destination = when (selectedLockType) {
            LockType.PASSWORD -> SetPasswordActivity::class.java
            LockType.PIN -> SetPinActivity::class.java
            LockType.PATTERN -> PatternSetActivity::class.java
        }
        val intent = Intent(baseActivity, destination)
        intent.putExtra(IS_FROM_SETTINGS, isFromSettings)
        startActivity(intent)
        if (isFromSettings) {
            baseActivity.finish()
        } else {
            parentFragmentManager.popBackStack()
        }
    }

    companion object {
        private const val ARG_LOCK_TYPE = "arg_lock_type"

        fun newInstance(lockType: LockType, isFromSettings: Boolean): OnBoardLockWarningFragment {
            return OnBoardLockWarningFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_LOCK_TYPE, lockType.name)
                    putBoolean(IS_FROM_SETTINGS, isFromSettings)
                }
            }
        }
    }

    enum class LockType {
        PASSWORD, PIN, PATTERN
    }
}
