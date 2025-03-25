package org.horizontal.tella.mobile.views.fragment.peertopeer.activity

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity
import org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow.ConnectHotspotFragment

@AndroidEntryPoint
class PeerToPeerActivity  : BaseLockActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.peer_to_peer_layout)
        addFragment(ConnectHotspotFragment.newInstance(), R.id.root)
    }
}