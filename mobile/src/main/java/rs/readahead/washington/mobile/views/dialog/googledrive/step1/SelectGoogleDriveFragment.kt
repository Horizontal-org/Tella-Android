package rs.readahead.washington.mobile.views.dialog.googledrive.step1

import SharedGoogleDriveViewModel
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSelectGoogleDriveBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import timber.log.Timber

class SelectGoogleDriveFragment :
    BaseBindingFragment<FragmentSelectGoogleDriveBinding>(FragmentSelectGoogleDriveBinding::inflate),
    View.OnClickListener {

    private val sharedViewModel: SharedGoogleDriveViewModel by activityViewModels()
    private lateinit var requestAuthorizationLauncher: ActivityResultLauncher<Intent>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setupViewModel()
        setupAuthorizationLauncher()
    }

    private fun setupViewModel() {
        // Retrieve email from arguments and set it in ViewModel
        arguments?.getString("email_key")?.let { email ->
            sharedViewModel.setEmail(email)
        }

        // Initialize Drive service in ViewModel
        context?.let { sharedViewModel.initializeDriveService(it) }

        // Observe shared drives and update UI accordingly
        sharedViewModel.sharedDrives.observe(viewLifecycleOwner,  { drives ->
            binding.sharedDriveBtn.isEnabled = !drives.isNullOrEmpty()
            if (drives.isNullOrEmpty()) {
                Timber.d("No shared drives found.")
            }
        })

        // Fetch shared drives
        sharedViewModel.fetchSharedDrives()
    }

    private fun setupAuthorizationLauncher() {
        requestAuthorizationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                sharedViewModel.fetchSharedDrives()  // Retry fetching shared drives after authorization
            } else {
                Timber.e("Authorization denied by user.")
            }
        }
    }

    private fun initView() {
        binding.learnMoreTextView.setOnClickListener(this)
        binding.backBtn.setOnClickListener(this)
        binding.createFolderBtn.setOnClickListener(this)
        binding.sharedDriveBtn.setOnClickListener(this)
        binding.nextBtn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.back_btn -> baseActivity.onBackPressed()
            R.id.learn_more_textView -> navigateToCreateFolderFragment()
            R.id.next_btn, R.id.create_folder_btn -> navigateToSelectSharedDriveFragment()
        }
    }

    private fun navigateToCreateFolderFragment() {
        findNavController().navigate(
            R.id.action_selectGoogleDriveFragment_to_createFolderFragment,
            Bundle().apply {
                putString("email_key", sharedViewModel.email.value)
            }
        )
    }

    private fun navigateToSelectSharedDriveFragment() {
        sharedViewModel.sharedDrives.value?.let { drives ->
            findNavController().navigate(
                R.id.action_selectGoogleDriveFragment_to_selectSharedDriveFragment,
                Bundle().apply {
                    putStringArrayList("shared_drives_key", ArrayList(drives))
                }
            )
        } ?: run {
            Timber.d("No shared drives data to pass.")
        }
    }
}
