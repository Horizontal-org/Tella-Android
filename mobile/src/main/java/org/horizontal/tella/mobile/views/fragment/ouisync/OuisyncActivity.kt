package org.horizontal.tella.mobile.views.fragment.ouisync

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.ouisync.OuisyncClient
import org.horizontal.tella.mobile.databinding.ActivityOuisyncBinding
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity
import timber.log.Timber

class OuisyncActivity : BaseLockActivity() {
    private var retained = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!OuisyncClient.isSupported) {
            Toast.makeText(this, R.string.ouisync_unsupported_device, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        OuisyncClient.retain()
        retained = true
        val binding = ActivityOuisyncBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (retained) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { OuisyncClient.release() }
                    .onFailure { Timber.e("ouisync shutdown failed: ${it.javaClass.simpleName}") }
            }
        }
    }
}
