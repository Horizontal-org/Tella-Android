package rs.readahead.washington.mobile.views.fragment.vault.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Limits
import com.hzontal.tella_vault.filter.Sort
import com.hzontal.utils.MediaFile
import org.hzontal.shared_ui.appbar.ToolbarComponent
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.util.LockTimeoutManager
import rs.readahead.washington.mobile.util.setMargins
import rs.readahead.washington.mobile.views.activity.AudioPlayActivity
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.activity.PhotoViewerActivity
import rs.readahead.washington.mobile.views.activity.VideoViewerActivity
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.custom.CountdownTextView
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import timber.log.Timber

const val VAULT_FILTER = "vf"

class HomeVaultFragment : BaseFragment(), VaultClickListener, IHomeVaultPresenter.IView {
    private lateinit var toolbar: ToolbarComponent
    private lateinit var vaultRecyclerView: RecyclerView
    private lateinit var panicModeView: RelativeLayout
    private lateinit var countDownTextView: CountdownTextView
    private lateinit var seekBar: SeekBar
    private lateinit var seekBarContainer: View
    private var timerDuration = 0
    private var panicActivated = false
    private val vaultAdapter by lazy { VaultAdapter(this) }
    private lateinit var homeVaultPresenter: HomeVaultPresenter
    private val bundle by lazy { Bundle() }
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    private var writePermissionGranted = false
    private var vaultFile: VaultFile? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vault, container, false)
    }

    override fun initView(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        vaultRecyclerView = view.findViewById(R.id.vaultRecyclerView)
        panicModeView = view.findViewById(R.id.panic_mode_view)
        countDownTextView = view.findViewById(R.id.countdown_timer)
        seekBar = view.findViewById(R.id.panic_seek)
        seekBarContainer = view.findViewById(R.id.panicSeekContainer)
        setUpToolbar()
        initData()
        initListeners()
        initPermissions()
        fixAppBarShadow(view)
    }

    private fun initData() {
        homeVaultPresenter = HomeVaultPresenter(this)
        vaultRecyclerView.apply {
            adapter = vaultAdapter
            layoutManager = LinearLayoutManager(activity)
        }
        timerDuration = resources.getInteger(R.integer.panic_countdown_duration)
    }

    private fun initPermissions() {
        permissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                writePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]
                    ?: writePermissionGranted
                LockTimeoutManager().lockTimeout = Preferences.getLockTimeout()

                if (writePermissionGranted) {
                    vaultFile?.let { exportVaultFiles(it) }
                }
            }
    }

    private fun maybeGetFiles() {
        if (Preferences.isShowRecentFiles()) {
            val sort = Sort().apply {
                direction = Sort.Direction.DESC
                type = Sort.Type.DATE
            }
            val limits = Limits().apply {
                limit = 10
            }
            homeVaultPresenter.getRecentFiles(FilterType.ALL_WITHOUT_DIRECTORY, sort, limits)
        } else {
            vaultAdapter.removeRecentFiles()
        }
    }

    private fun maybeGetRecentForms() {
        if (Preferences.isShowFavoriteForms()) {
            homeVaultPresenter.getFavoriteCollectForms()
        } else {
            vaultAdapter.removeFavoriteForms()
        }
    }

    private fun maybeGetRecentTemplates() {
        if (Preferences.isShowFavoriteTemplates()) {
            homeVaultPresenter.getFavoriteCollectTemplates()
        } else {
            vaultAdapter.removeFavoriteTemplates()
        }
    }

    private fun initListeners() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (seekBar.progress == 100) {
                    seekBar.progress = 0
                    showPanicScreens()
                } else {
                    seekBar.progress = 0
                    hidePanicScreens()
                }
            }
        })
        panicModeView.setOnClickListener { onPanicClicked() }
        toolbar.onLeftClickListener = { nav().navigate(R.id.main_settings) }
        toolbar.onRightClickListener = {
            MyApplication.exit(activity)
            MyApplication.getMainKeyHolder().timeout = LockTimeoutManager.IMMEDIATE_SHUTDOWN
        }
        vaultAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                vaultRecyclerView.scrollToPosition(0)
            }
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                vaultRecyclerView.scrollToPosition(0)
            }
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                vaultRecyclerView.scrollToPosition(0)
            }
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                vaultRecyclerView.scrollToPosition(0)
            }
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                vaultRecyclerView.scrollToPosition(0)
            }
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                vaultRecyclerView.scrollToPosition(0)
            }
        })
    }

    private fun setUpToolbar() {
        val activity = context as MainActivity
        activity.setSupportActionBar(toolbar)
    }

    override fun onRecentFilesItemClickListener(vaultFile: VaultFile) {
        when {
            MediaFile.isImageFileType(vaultFile.mimeType) -> {
                val intent = Intent(activity, PhotoViewerActivity::class.java)
                intent.putExtra(PhotoViewerActivity.VIEW_PHOTO, vaultFile)
                startActivity(intent)
            }
            MediaFile.isAudioFileType(vaultFile.mimeType) -> {
                val intent = Intent(activity, AudioPlayActivity::class.java)
                intent.putExtra(AudioPlayActivity.PLAY_MEDIA_FILE_ID_KEY, vaultFile.id)
                startActivity(intent)
            }
            MediaFile.isVideoFileType(vaultFile.mimeType) -> {
                val intent = Intent(activity, VideoViewerActivity::class.java)
                intent.putExtra(VideoViewerActivity.VIEW_VIDEO, vaultFile)
                startActivity(intent)
            }
            else -> {
                BottomSheetUtils.showStandardSheet(
                    activity.supportFragmentManager,
                    activity.getString(R.string.Vault_Export_SheetAction) + " " + vaultFile.name + "?",
                    activity.getString(R.string.Vault_ViewerOther_SheetDesc),
                    activity.getString(R.string.Vault_Export_SheetAction),
                    activity.getString(R.string.action_cancel),
                    onConfirmClick = { exportVaultFiles(vaultFile) }
                )
            }
        }
    }

    override fun onFavoriteItemClickListener(form: CollectForm) {

    }

    override fun onFavoriteTemplateClickListener(template: CollectTemplate) {

    }


    override fun allFilesClickListener() {
        bundle.putString(VAULT_FILTER, FilterType.ALL.name)
        navigateToAttachmentsList(bundle)
    }

    override fun imagesClickListener() {
        bundle.putString(VAULT_FILTER, FilterType.PHOTO.name)
        navigateToAttachmentsList(bundle)
    }

    override fun audioClickListener() {
        bundle.putString(VAULT_FILTER, FilterType.AUDIO.name)
        navigateToAttachmentsList(bundle)
    }

    override fun documentsClickListener() {
        bundle.putString(VAULT_FILTER, FilterType.DOCUMENTS.name)
        navigateToAttachmentsList(bundle)
    }

    override fun othersClickListener() {
        bundle.putString(VAULT_FILTER, FilterType.OTHERS.name)
        navigateToAttachmentsList(bundle)
    }

    override fun videoClickListener() {
        bundle.putString(VAULT_FILTER, FilterType.VIDEO.name)
        navigateToAttachmentsList(bundle)
    }

    private fun stopPanicking() {
        countDownTextView.cancel()
        countDownTextView.setCountdownNumber(timerDuration)
        panicActivated = false
        // showMainControls()
    }

    override fun onResume() {
        super.onResume()
        setupPanicView()
        if (panicActivated) {
            showPanicScreens()
            panicActivated = false
        } else {
            maybeClosePanic()
        }
        maybeGetFiles()
        maybeGetRecentForms()
        maybeHideFilesTitle()
        maybeGetRecentTemplates()
    }

    private fun maybeClosePanic(): Boolean {
        if (panicModeView.visibility == View.VISIBLE) {
            stopPanicking()
            hidePanicScreens()
        }
        return false // todo: check panic state here
    }

    private fun hidePanicScreens() {
        (activity as MainActivity).showBottomNavigation()
        setupPanicView()
        panicModeView.visibility = View.GONE
        toolbar.visibility = View.VISIBLE
    }

    private fun showPanicScreens() {
        // really show panic screen
        (activity as MainActivity).hideBottomNavigation()
        toolbar.visibility = View.GONE
        panicModeView.visibility = View.VISIBLE
        panicModeView.alpha = 1f
        countDownTextView.start(
            timerDuration
        ) {
            executePanicMode()
        }
    }

    private fun setupPanicView() {
        if (Preferences.isQuickExit()) {
            seekBarContainer.visibility = View.VISIBLE
            vaultRecyclerView.setMargins(null, null, null, 110)
        } else {
            seekBarContainer.visibility = View.GONE
            vaultRecyclerView.setMargins(null, null, null, 55)
        }
    }

    private fun executePanicMode() {
        try {
            homeVaultPresenter.executePanicMode()
        } catch (ignored: Throwable) {
            panicActivated = true
        }
    }

    private fun onPanicClicked() {
        hidePanicScreens()
        stopPanicking()
    }

    override fun onCountTUServersEnded(num: Long?) {
    }

    override fun onCountTUServersFailed(throwable: Throwable?) {
    }

    override fun onCountCollectServersEnded(num: Long?) {
    }

    override fun onCountCollectServersFailed(throwable: Throwable?) {
    }

    override fun onGetFilesSuccess(files: List<VaultFile?>) {
        if (!files.isNullOrEmpty()) {
            vaultAdapter.addRecentFiles(files)
        }else{
            vaultAdapter.removeRecentFiles()
        }
    }

    override fun onGetFilesError(error: Throwable?) {
        Timber.d(error, javaClass.name)

    }

    override fun onMediaExported(num: Int) {
        activity.toggleLoading(false)
    }

    override fun onExportError(error: Throwable?) {
        DialogUtils.showBottomMessage(
            activity,
            getString(R.string.gallery_toast_fail_exporting_to_device),
            false
        )
    }

    override fun onExportStarted() {
        activity.toggleLoading(true)
    }

    override fun onExportEnded() {
        activity.toggleLoading(false)
    }

    override fun onGetFavoriteCollectFormsSuccess(files: List<CollectForm>) {
        if (!files.isNullOrEmpty()) {
            vaultAdapter.addFavoriteForms(files)
        } else {
            vaultAdapter.removeFavoriteForms()
        }
    }

    override fun onGetFavoriteCollectFormsError(error: Throwable?) {
        Timber.d(error)
    }

    override fun onGetFavoriteCollectTemplatesSuccess(files: List<CollectTemplate>?) {
        if (!files.isNullOrEmpty()) {
            vaultAdapter.addFavoriteTemplates(files)
        } else {
            vaultAdapter.removeFavoriteTemplates()
        }
    }

    override fun onGetFavoriteCollectTemplateError(error: Throwable?) {
        Timber.d(error)
    }

    private fun navigateToAttachmentsList(bundle: Bundle?) {
        nav().navigate(R.id.action_homeScreen_to_attachments_screen, bundle)
    }

    private fun exportVaultFiles(vaultFile: VaultFile) {
        this.vaultFile = vaultFile
        if (writePermissionGranted) {
            vaultFile.let { homeVaultPresenter.exportMediaFiles(arrayListOf(vaultFile)) }
        } else {
            updateOrRequestPermissions()
        }
    }

    private fun updateOrRequestPermissions() {
        activity.changeTemporaryTimeout()

        val hasWritePermission = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        writePermissionGranted = hasWritePermission || minSdk29

        val permissionsToRequest = mutableListOf<String>()
        if (!writePermissionGranted) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)

            if (permissionsToRequest.isNotEmpty()) {
                permissionsLauncher.launch(permissionsToRequest.toTypedArray())
            }
        }
    }

    private fun maybeHideFilesTitle() {
        if (!Preferences.isShowRecentFiles() && !Preferences.isShowFavoriteForms()) {
            vaultAdapter.removeTitle()
        } else {
            vaultAdapter.addTitle()
        }
    }

    private fun fixAppBarShadow(view: View) {
        val appBar = view.findViewById<View>(R.id.appbar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appBar.outlineProvider = null
        } else {
            appBar.bringToFront()
        }
    }
}