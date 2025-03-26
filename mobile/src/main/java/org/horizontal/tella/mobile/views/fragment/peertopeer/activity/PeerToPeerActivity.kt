package org.horizontal.tella.mobile.views.fragment.peertopeer.activity

import android.os.Bundle
import org.horizontal.tella.mobile.databinding.ActivityPeerToPeerBinding
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity

class PeerToPeerActivity  : BaseLockActivity() {
    private lateinit var binding: ActivityPeerToPeerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPeerToPeerBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
        binding.toolbar.backClickListener = { this.onBackPressed() }

    }
}