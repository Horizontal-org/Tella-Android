package org.horizontal.tella.mobile.views.fragment.peertopeer.common

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import org.horizontal.tella.mobile.databinding.FragmentTipsConnectBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

class TipsConnectFragment :
    BaseBindingFragment<FragmentTipsConnectBinding>(FragmentTipsConnectBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.toolbar.backClickListener =
            { requireActivity().onBackPressedDispatcher.onBackPressed() }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            isEnabled = false
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}