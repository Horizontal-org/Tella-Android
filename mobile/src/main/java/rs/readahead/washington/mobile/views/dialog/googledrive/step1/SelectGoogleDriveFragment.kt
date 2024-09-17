package rs.readahead.washington.mobile.views.dialog.googledrive.step1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSelectGoogleDriveBinding
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.googledrive.SharedGoogleDriveViewModel
import rs.readahead.washington.mobile.views.dialog.googledrive.setp0.OBJECT_KEY
import timber.log.Timber

class SelectGoogleDriveFragment :
    BaseBindingFragment<FragmentSelectGoogleDriveBinding>(FragmentSelectGoogleDriveBinding::inflate),
    View.OnClickListener {

    private val sharedViewModel: SharedGoogleDriveViewModel by activityViewModels()
    private lateinit var requestAuthorizationLauncher: ActivityResultLauncher<Intent>
    private lateinit var googleDriveServer: GoogleDriveServer // for the update

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setupViewModel()
        setupAuthorizationLauncher()
    }

    private fun setupViewModel() {
        // Retrieve email from arguments and set it in ViewModel
        if (arguments == null) return

        arguments?.getString(OBJECT_KEY)?.let {
            googleDriveServer = Gson().fromJson(it, GoogleDriveServer::class.java)
            sharedViewModel.setEmail(googleDriveServer.username)
        }

        // Observe shared drives and update UI accordingly
        sharedViewModel.sharedDrives.observe(viewLifecycleOwner) { drives ->
            binding.sharedDriveBtn.isEnabled = !drives.isNullOrEmpty()

            if (!drives.isNullOrEmpty()) {
                binding.sharedDriveBtn.alpha = 1f
                binding.sharedDriveBtn.isClickable = true
                binding.sharedDriveBtn.setOnClickListener { onSharedDriveSelected() }

            } else {
                binding.sharedDriveBtn.alpha = 0.65f
                binding.sharedDriveBtn.isClickable = false
                binding.sharedDriveBtn.setOnClickListener({})
            }
        }

        // Fetch shared drives
        sharedViewModel.fetchSharedDrives()
    }

    private fun setupAuthorizationLauncher() {
        requestAuthorizationLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    sharedViewModel.fetchSharedDrives()  // Retry fetching shared drives after authorization
                } else {
                    Timber.e("Authorization denied by user.")
                }
            }
    }

    private fun initView() {
        with(binding) {
            toolbar.run { setStartTextTitle(context.getString(R.string.select_google_drive)) }
            toolbar.backClickListener = { baseActivity.onBackPressed() }
            backBtn.setOnClickListener(this@SelectGoogleDriveFragment)
            nextBtn.visibility = View.GONE
            learnMoreTextView.setOnClickListener(this@SelectGoogleDriveFragment)
            sharedDriveBtn.setOnClickListener {
                onSharedDriveSelected()
            }
            binding.createFolderBtn.setOnClickListener {
                onCreateFolderSelected()
            }
            nextBtn.setOnClickListener(this@SelectGoogleDriveFragment)
        }

    }

    private fun onSharedDriveSelected() {
        binding.sharedDriveBtn.isChecked = true
        binding.createFolderBtn.isChecked = false
        binding.nextBtn.isVisible = true
    }

    private fun onCreateFolderSelected() {
        binding.createFolderBtn.isChecked = true
        binding.sharedDriveBtn.isChecked = false
        binding.nextBtn.isVisible = true
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.back_btn -> baseActivity.onBackPressed()
            R.id.next_btn -> {
                if (binding.createFolderBtn.isChecked) {
                    navigateToCreateFolderFragment()
                }
                if (binding.sharedDriveBtn.isChecked) {
                    navigateToSelectSharedDriveFragment()
                }
            }
        }
    }

    private fun navigateToCreateFolderFragment() {
        bundle.putSerializable(OBJECT_KEY, googleDriveServer)
        navManager().navigateFromSelectGoogleDriveToCreateFolderFragment()
    }

    private fun navigateToSelectSharedDriveFragment() {
        sharedViewModel.sharedDrives.value?.let {
            bundle.putSerializable(OBJECT_KEY, googleDriveServer)
            navManager().navigateFromSelectGoogleDriveFragmentToSelectSharedDriveFragment()
        } ?: run {
            Timber.d("No shared drives data to pass.")
        }
    }

    private fun copyFields(server: GoogleDriveServer): GoogleDriveServer {
        googleDriveServer.folderName = server.folderName
        googleDriveServer.folderId = server.folderId
        googleDriveServer.username = server.username
        return server
    }
}
