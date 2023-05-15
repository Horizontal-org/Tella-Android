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
import rs.readahead.washington.mobile.bus.EventCompositeDisposable
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.domain.entity.ServerType
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.util.CleanInsightUtils
import rs.readahead.washington.mobile.util.LockTimeoutManager
import rs.readahead.washington.mobile.util.setMargins
import rs.readahead.washington.mobile.views.activity.AudioPlayActivity
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.activity.PhotoViewerActivity
import rs.readahead.washington.mobile.views.activity.viewer.VideoViewerActivity
import rs.readahead.washington.mobile.views.activity.clean_insights.CleanInsightsActions
import rs.readahead.washington.mobile.views.activity.clean_insights.CleanInsightsActivity
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.custom.CountdownTextView
import rs.readahead.washington.mobile.views.fragment.vault.adapters.ImproveClickOptions
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.connections.ServerDataItem
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
    private var serversList: MutableList<ServerDataItem>? = null
    private var tuServers: ArrayList<TellaReportServer>? = null
    private var uwaziServers: ArrayList<UWaziUploadServer>? = null
    private var collectServers: ArrayList<CollectServer>? = null
    private var disposables: EventCompositeDisposable? = null


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
        disposables = MyApplication.bus().createCompositeDisposable()

        setUpToolbar()
        initData()
        initListeners()
        initPermissions()
        fixAppBarShadow(view)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CleanInsightsActivity.CLEAN_INSIGHTS_REQUEST_CODE) {
            removeImprovementSection()
            val cleanInsightsActions =
                data?.extras?.getSerializable(CleanInsightsActivity.RESULT_FOR_ACTIVITY) as CleanInsightsActions
            showMessageForCleanInsightsApprove(cleanInsightsActions)
        }
    }

    private fun showMessageForCleanInsightsApprove(cleanInsightsActions: CleanInsightsActions) {
        if (cleanInsightsActions == CleanInsightsActions.YES) {
            Preferences.setIsAcceptedImprovements(true)
            CleanInsightUtils.grantCampaign(true)
            DialogUtils.showBottomMessage(
                requireActivity(),
                getString(R.string.clean_insights_signed_for_days),
                false
            )
        }
    }

    private fun initData() {
        homeVaultPresenter = HomeVaultPresenter(this)
        vaultRecyclerView.apply {
            adapter = vaultAdapter
            layoutManager = LinearLayoutManager(baseActivity)
        }
        //Uncomment to add improvement section
        // vaultAdapter.addImprovementSection()
        timerDuration = resources.getInteger(R.integer.panic_countdown_duration)

        serversList = ArrayList()
        tuServers = ArrayList()
        uwaziServers = ArrayList()
        collectServers = ArrayList()

        CleanInsightUtils.measureEvent()
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

    private fun maybeShowConnections() {
        if (serversList?.isEmpty() == false) {
            vaultAdapter.addConnectionServers(serversList!!)
        } else {
            vaultAdapter.removeConnectionServers()
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
            MyApplication.getMainKeyHolder().timeout = LockTimeoutManager.IMMEDIATE_SHUTDOWN
            Preferences.setExitTimeout(true)
            MyApplication.exit(baseActivity)
            baseActivity.finish()
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

    private fun startCleanInsightActivity() {
        val intent = Intent(context, CleanInsightsActivity::class.java)
        startActivityForResult(intent, CleanInsightsActivity.CLEAN_INSIGHTS_REQUEST_CODE)
    }

    private fun setUpToolbar() {
        val activity = context as MainActivity
        activity.setSupportActionBar(toolbar)
    }

    override fun onRecentFilesItemClickListener(vaultFile: VaultFile) {
        when {
            MediaFile.isImageFileType(vaultFile.mimeType) -> {
                val intent = Intent(baseActivity, PhotoViewerActivity::class.java)
                intent.putExtra(PhotoViewerActivity.VIEW_PHOTO, vaultFile)
                startActivity(intent)
            }
            MediaFile.isAudioFileType(vaultFile.mimeType) -> {
                val intent = Intent(baseActivity, AudioPlayActivity::class.java)
                intent.putExtra(AudioPlayActivity.PLAY_MEDIA_FILE_ID_KEY, vaultFile.id)
                startActivity(intent)
            }
            MediaFile.isVideoFileType(vaultFile.mimeType) -> {
                val intent = Intent(baseActivity, VideoViewerActivity::class.java)
                intent.putExtra(VideoViewerActivity.VIEW_VIDEO, vaultFile)
                startActivity(intent)
            }
            else -> {
                BottomSheetUtils.showStandardSheet(
                    baseActivity.supportFragmentManager,
                    baseActivity.getString(R.string.Vault_Export_SheetAction) + " " + vaultFile.name + "?",
                    baseActivity.getString(R.string.Vault_ViewerOther_SheetDesc),
                    baseActivity.getString(R.string.Vault_Export_SheetAction),
                    baseActivity.getString(R.string.action_cancel),
                    onConfirmClick = { exportVaultFiles(vaultFile) }
                )
            }
        }
    }

    override fun onFavoriteItemClickListener(form: CollectForm) {

    }

    override fun onFavoriteTemplateClickListener(template: CollectTemplate) {

    }

    override fun onServerItemClickListener(item: ServerDataItem) {
        when (item.type) {
            ServerType.ODK_COLLECT -> {
                nav().navigate(R.id.action_homeScreen_to_forms_screen)
            }
            ServerType.TELLA_UPLOAD -> {
                nav().navigate(R.id.action_homeScreen_to_reports_screen)
            }
            ServerType.UWAZI -> {
                nav().navigate(R.id.action_homeScreen_to_uwazi_screen)
            }
            else -> {}
        }
    }


    override fun onImproveItemClickListener(improveClickOptions: ImproveClickOptions) {
        when (improveClickOptions) {
            ImproveClickOptions.CLOSE -> removeImprovementSection()
            ImproveClickOptions.YES -> {
                removeImprovementSection()
                showMessageForCleanInsightsApprove(CleanInsightsActions.YES)
            }
            ImproveClickOptions.LEARN_MORE -> startCleanInsightActivity()
            ImproveClickOptions.SETTINGS -> {
                removeImprovementSection()
                Preferences.setIsAcceptedImprovements(true)
                nav().navigate(R.id.main_settings)
            }
        }
    }

    private fun removeImprovementSection() {
        Preferences.setShowVaultImprovementSection(false)
        vaultAdapter.removeImprovementSection()
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
        maybeCountServers()
    }

    private fun maybeCountServers() {
        homeVaultPresenter.countCollectServers()
        homeVaultPresenter.countTUServers()
        homeVaultPresenter.countUwaziServers()
    }

    private fun maybeClosePanic(): Boolean {
        if (panicModeView.visibility == View.VISIBLE) {
            stopPanicking()
            hidePanicScreens()
        }
        return false // todo: check panic state here
    }

    private fun hidePanicScreens() {
        (baseActivity as MainActivity).showBottomNavigation()
        setupPanicView()
        panicModeView.visibility = View.GONE
        toolbar.visibility = View.VISIBLE
    }

    private fun showPanicScreens() {
        // really show panic screen
        (baseActivity as MainActivity).hideBottomNavigation()
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

    override fun onDestroy() {
        super.onDestroy()
        disposables?.dispose()
    }


    override fun onCountTUServersEnded(servers: List<TellaReportServer>?) {
        tuServers?.clear()
        serversList?.removeIf { item -> item.type == ServerType.TELLA_UPLOAD }
        if (!servers.isNullOrEmpty()) {
            tuServers?.addAll(servers)
            serversList?.add(ServerDataItem(servers, ServerType.TELLA_UPLOAD))
        }
        maybeShowConnections()
    }

    override fun onCountTUServersFailed(throwable: Throwable?) {
        Timber.d("***onCountTUServersFailed**$throwable")
    }

    override fun onCountCollectServersEnded(servers: List<CollectServer>?) {
        collectServers?.clear()
        serversList?.removeIf { item -> item.type == ServerType.ODK_COLLECT }
        if (!servers.isNullOrEmpty()) {
            collectServers?.addAll(servers)
            serversList?.add(ServerDataItem(servers, ServerType.ODK_COLLECT))
        }
        maybeShowConnections()
    }


    override fun onCountCollectServersFailed(throwable: Throwable?) {
        Timber.d("***onCountCollectServersFailed**$throwable")
    }

    override fun onCountUwaziServersEnded(servers: List<UWaziUploadServer>?) {
        uwaziServers?.clear()
        serversList?.removeIf { item -> item.type == ServerType.UWAZI }
        if (!servers.isNullOrEmpty()) {
            uwaziServers?.addAll(servers)
            serversList?.add(ServerDataItem(servers, ServerType.UWAZI))
        }
        maybeShowConnections()
    }

    override fun onCountUwaziServersFailed(throwable: Throwable?) {
        Timber.d("***onCountUwaziServersFailed**$throwable")
    }

    override fun onGetFilesSuccess(files: List<VaultFile?>) {
        if (files.isNotEmpty()) {
            vaultAdapter.addRecentFiles(files)
        } else {
            vaultAdapter.removeRecentFiles()
        }
    }

    override fun onGetFilesError(error: Throwable?) {
        Timber.d(error, javaClass.name)

    }

    override fun onMediaExported(num: Int) {
        baseActivity.toggleLoading(false)
    }

    override fun onExportError(error: Throwable?) {
        DialogUtils.showBottomMessage(
            baseActivity,
            getString(R.string.gallery_toast_fail_exporting_to_device),
            false
        )
    }

    override fun onExportStarted() {
        baseActivity.toggleLoading(true)
    }

    override fun onExportEnded() {
        baseActivity.toggleLoading(false)
    }

    override fun onGetFavoriteCollectFormsSuccess(files: List<CollectForm>) {
        if (files.isNotEmpty()) {
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
        baseActivity.maybeChangeTemporaryTimeout()
        val hasWritePermission = ContextCompat.checkSelfPermission(
            baseActivity,
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
        if (!Preferences.isShowRecentFiles() && !Preferences.isShowFavoriteForms() && serversList?.isNullOrEmpty() == false) {
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