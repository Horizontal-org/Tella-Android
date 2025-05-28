package org.horizontal.tella.mobile.views.fragment.peertopeer.activity

import android.content.Intent
import android.os.Bundle
import org.horizontal.tella.mobile.databinding.ActivityPeerToPeerBinding
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity

class PeerToPeerActivity : BaseLockActivity() {
    private lateinit var binding: ActivityPeerToPeerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPeerToPeerBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Delegate onActivityResult to child fragments
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.forEach {
            it.onActivityResult(requestCode, resultCode, data)
        }
    }
}