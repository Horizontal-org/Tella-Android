package rs.readahead.washington.mobile.views.dialog.googledrive.step1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSelectGoogleDriveBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.googledrive.SharedGoogleDriveViewModel
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
        sharedViewModel.sharedDrives.observe(viewLifecycleOwner) { drives ->
            binding.sharedDriveBtn.isEnabled = !drives.isNullOrEmpty()
            if (!drives.isNullOrEmpty()) {
                binding.sharedDriveBtn.alpha = 1f
                binding.sharedDriveBtn.isClickable = true
            } else {
                binding.sharedDriveBtn.alpha = 0.65f
                binding.sharedDriveBtn.isClickable = false
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
        binding.toolbar.setStartTextTitle("Select Google drive")
        binding.toolbar.backClickListener = { baseActivity.onBackPressed() }
        binding.backBtn.setOnClickListener(this)
        binding.nextBtn.visibility = View.GONE
        binding.learnMoreTextView.setOnClickListener(this)
        binding.sharedDriveBtn.setOnClickListener {
            binding.sharedDriveBtn.isChecked = true
            binding.createFolderBtn.isChecked = false
            binding.nextBtn.visibility = View.VISIBLE
        }
        binding.createFolderBtn.setOnClickListener {
            binding.createFolderBtn.isChecked = true
            binding.sharedDriveBtn.isChecked = false
            binding.nextBtn.visibility = View.VISIBLE
        }
        binding.nextBtn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.back_btn -> baseActivity.onBackPressed()
            R.id.next_btn -> {
                if (binding?.createFolderBtn?.isChecked == true) {
                    navigateToCreateFolderFragment()
                }
                if (binding?.sharedDriveBtn?.isChecked == true) {
                    navigateToSelectSharedDriveFragment()
                }
            }
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
