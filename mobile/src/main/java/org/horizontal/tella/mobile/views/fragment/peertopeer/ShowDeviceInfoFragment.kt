package org.horizontal.tella.mobile.views.fragment.peertopeer

import android.os.Bundle
import android.view.View
import org.horizontal.tella.mobile.databinding.ShowDeviceInfoLayoutBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

class ShowDeviceInfoFragment : BaseBindingFragment<ShowDeviceInfoLayoutBinding>(ShowDeviceInfoLayoutBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    private fun initListeners() {

    }
}