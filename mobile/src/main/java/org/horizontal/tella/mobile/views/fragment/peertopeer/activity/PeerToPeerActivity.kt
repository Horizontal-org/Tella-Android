package org.horizontal.tella.mobile.views.fragment.peertopeer.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.hzontal.tella_vault.VaultFile
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.ActivityPeerToPeerBinding
import org.horizontal.tella.mobile.mvvm.media.MediaImportViewModel
import org.horizontal.tella.mobile.util.C
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity
import org.horizontal.tella.mobile.views.fragment.uwazi.attachments.VAULT_FILE_KEY
import timber.log.Timber

class PeerToPeerActivity : BaseLockActivity() {

    private lateinit var binding: ActivityPeerToPeerBinding
    private val mediaImportViewModel: MediaImportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPeerToPeerBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
        initObservers()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle import results
        if (requestCode == C.IMPORT_VIDEO || requestCode == C.IMPORT_IMAGE || requestCode == C.IMPORT_FILE) {
            handleImportResult(requestCode, data)
            return
        }

        // Delegate onActivityResult to child fragments
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.forEach {
            it.onActivityResult(requestCode, resultCode, data)
        }

    }

    private fun handleImportResult(requestCode: Int, data: Intent?) {
        try {
            if (data != null) {
                val uri = data.data
                if (uri != null) {
                    divviupUtils.runFileImportEvent()
                    when (requestCode) {
                        C.IMPORT_FILE -> mediaImportViewModel.importFile(uri)
                    }
                }
            }
        } catch (e: NullPointerException) {
            // Handle null pointer exception
            showToast(R.string.gallery_toast_fail_importing_file)
            FirebaseCrashlytics.getInstance().recordException(e)
            Timber.e(e, "NullPointerException occurred: ${e.message}")
        } catch (e: Exception) {
            // Handle other exceptions
            FirebaseCrashlytics.getInstance().recordException(e)
            Timber.e(e, "NullPointerException occurred: ${e.message}")
        }
    }

    private fun onMediaFileImported(vaultFile: VaultFile) {
        val list: MutableList<String> = ArrayList()
        list.add(vaultFile.id)
        onActivityResult(
            C.MEDIA_FILE_ID, RESULT_OK, Intent().putExtra(VAULT_FILE_KEY, Gson().toJson(list))
        )
    }

    private fun onImportError(throwable: Throwable?) {
        Timber.d(throwable)
    }

    private fun initObservers() {
        mediaImportViewModel.mediaFileLiveData.observe(this, ::onMediaFileImported)
        mediaImportViewModel.importError.observe(this, ::onImportError)
    }

}