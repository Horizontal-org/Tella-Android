package rs.readahead.washington.mobile.views.fragment.vault.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Limits
import com.hzontal.tella_vault.filter.Sort
import com.hzontal.utils.MediaFile
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.EventCompositeDisposable
import rs.readahead.washington.mobile.bus.EventObserver
import rs.readahead.washington.mobile.bus.event.RecentBackgroundActivitiesEvent
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.FragmentVaultBinding
import rs.readahead.washington.mobile.domain.entity.ServerType
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.util.CleanInsightUtils
import rs.readahead.washington.mobile.util.LockTimeoutManager
import rs.readahead.washington.mobile.util.TopSheetTestUtils.showBackgroundActivitiesSheet
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.util.setMargins
import rs.readahead.washington.mobile.util.show
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.activity.camera.SharedCameraViewModel
import rs.readahead.washington.mobile.views.activity.clean_insights.CleanInsightsActions
import rs.readahead.washington.mobile.views.activity.clean_insights.CleanInsightsActivity
import rs.readahead.washington.mobile.views.activity.viewer.AudioPlayActivity
import rs.readahead.washington.mobile.views.activity.viewer.PDFReaderActivity
import rs.readahead.washington.mobile.views.activity.viewer.PhotoViewerActivity
import rs.readahead.washington.mobile.views.activity.viewer.VideoViewerActivity
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.vault.adapters.ImproveClickOptions
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.connections.ServerDataItem
import rs.readahead.washington.mobile.views.fragment.vault.home.background_activities.BackgroundActivitiesAdapter
import timber.log.Timber


const val VAULT_FILTER = "vf"

//TODO REFACTOR THIS TO MVVM
@AndroidEntryPoint
class HomeVaultFragment : BaseBindingFragment<FragmentVaultBinding>(FragmentVaultBinding::inflate),
    VaultClickListener, IHomeVaultPresenter.IView {
    private var timerDuration = 0
    private var panicActivated = false
    private val vaultAdapter by lazy { VaultAdapter(this) }
    private lateinit var homeVaultPresenter: HomeVaultPresenter
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    private var writePermissionGranted = false
    private var vaultFile: VaultFile? = null
    private var serversList: MutableList<ServerDataItem>? = null
    private var tuServers: ArrayList<TellaReportServer>? = null
    private var uwaziServers: ArrayList<UWaziUploadServer>? = null
    private var collectServers: ArrayList<CollectServer>? = null
    private var disposables: EventCompositeDisposable? = null
    private var reportServersCounted = false
    private var collectServersCounted = false
    private var uwaziServersCounted = false
    private val viewModel by viewModels<SharedCameraViewModel>()

    private val backgroundActivitiesAdapter by lazy { BackgroundActivitiesAdapter(mutableListOf()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    fun initView() {
        disposables = MyApplication.bus().createCompositeDisposable()

        setUpToolbar()
        initData()
        initListeners()
        initPermissions()
        fixAppBarShadow()
        initObservers()
    }

    private fun initObservers() {
        viewModel.lastBackgroundActivityModel.observe(baseActivity) { activityList ->
         //   if (activityList.hasItems()) {
                binding.counterNotification.show()
               // binding.counterNotification.text = event.size().toString()
                backgroundActivitiesAdapter.updateData(arrayListOf(activityList))
          ///  } else {
             //   binding.counterNotification.hide()
           // }
        }

        binding.counterNotification.setOnClickListener {
            showBackgroundActivitiesSheet(
                baseActivity.supportFragmentManager,
                getString(R.string.background_activities),
                getString(R.string.current_background_activities),
                backgroundActivitiesAdapter = backgroundActivitiesAdapter
            )
        }
    }

    @Deprecated("Deprecated in Java")
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
        binding.vaultRecyclerView.apply {
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

    /**
     * This function show connections when all the server types are counted.
     **/
    private fun maybeShowConnections() {
        if (serversList?.isEmpty() == false && reportServersCounted && collectServersCounted && uwaziServersCounted) {
            vaultAdapter.addConnectionServers(serversList!!)
        } else {
            vaultAdapter.removeConnectionServers()
        }
    }

    private fun initListeners() {
        binding.panicSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (binding.panicSeek.progress == 100) {
                    binding.panicSeek.progress = 0
                    showPanicScreens()
                } else {
                    binding.panicSeek.progress = 0
                    hidePanicScreens()
                }
            }
        })
        binding.content.panicModeView.setOnClickListener { onPanicClicked() }
        binding.toolbar.onLeftClickListener = { nav().navigate(R.id.main_settings) }
        binding.toolbar.onRightClickListener = {
            MyApplication.getMainKeyHolder().timeout = LockTimeoutManager.IMMEDIATE_SHUTDOWN
            Preferences.setExitTimeout(true)
            MyApplication.exit(baseActivity)
            baseActivity.finish()
        }
        vaultAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                binding.vaultRecyclerView.scrollToPosition(0)
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                binding.vaultRecyclerView.scrollToPosition(0)
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                binding.vaultRecyclerView.scrollToPosition(0)
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.vaultRecyclerView.scrollToPosition(0)
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                binding.vaultRecyclerView.scrollToPosition(0)
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                binding.vaultRecyclerView.scrollToPosition(0)
            }
        })
    }

    private fun startCleanInsightActivity() {
        val intent = Intent(context, CleanInsightsActivity::class.java)
        startActivityForResult(intent, CleanInsightsActivity.CLEAN_INSIGHTS_REQUEST_CODE)
    }

    private fun setUpToolbar() {
        val baseActivity = activity as MainActivity
        baseActivity.setSupportActionBar(binding.toolbar)
        //   maybeShowRecentBackgroundActivities()
    }

    private fun maybeShowRecentBackgroundActivities() {
        disposables?.wire(
            RecentBackgroundActivitiesEvent::class.java,
            object : EventObserver<RecentBackgroundActivitiesEvent?>() {
                override fun onNext(event: RecentBackgroundActivitiesEvent) {
                    if (event.hasItems()) {
                        binding.counterNotification.show()
                        binding.counterNotification.text = event.size().toString()
                        backgroundActivitiesAdapter.updateData(event.backgroundActivityModels)
                    } else {
                        binding.counterNotification.hide()
                    }
                }
            })

        binding.counterNotification.setOnClickListener {
            showBackgroundActivitiesSheet(
                baseActivity.supportFragmentManager,
                getString(R.string.background_activities),
                getString(R.string.current_background_activities),
                backgroundActivitiesAdapter = backgroundActivitiesAdapter
            )
        }
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
            MediaFile.isPDFFile(vaultFile.name,vaultFile.mimeType) -> {
                val intent = Intent(baseActivity, PDFReaderActivity::class.java)
                intent.putExtra(PDFReaderActivity.VIEW_PDF, vaultFile)
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
            ServerType.TELLA_RESORCES -> {
                nav().navigate(R.id.action_homeScreen_to_resources_screen)
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
        binding.content.countdownTimer.cancel()
        binding.content.countdownTimer.setCountdownNumber(timerDuration)
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

    override fun onStart() {
        super.onStart()
        maybeCountServers()
    }

    private fun maybeCountServers() {
        clearServerCount()
        homeVaultPresenter.countCollectServers()
        homeVaultPresenter.countTUServers()
        homeVaultPresenter.countUwaziServers()
    }

    /**
     * This is used to start counting different connections (servers) to be shown on the home fragment.
     * At the start, the list of servers and shown connections are cleared.
     **/
    private fun clearServerCount() {
        reportServersCounted = false
        collectServersCounted = false
        uwaziServersCounted = false
        serversList?.clear()
        vaultAdapter.removeConnectionServers()
    }

    private fun maybeClosePanic(): Boolean {
        if (binding.content.panicModeView.visibility == View.VISIBLE) {
            stopPanicking()
            hidePanicScreens()
        }
        return false // todo: check panic state here
    }

    private fun hidePanicScreens() {
        (baseActivity as MainActivity).showBottomNavigation()
        setupPanicView()
        binding.content.panicModeView.visibility = View.GONE
        binding.toolbar.visibility = View.VISIBLE
    }

    private fun showPanicScreens() {
        // really show panic screen
        (baseActivity as MainActivity).hideBottomNavigation()
        binding.toolbar.visibility = View.GONE
        binding.content.panicModeView.visibility = View.VISIBLE
        binding.content.panicModeView.alpha = 1f
        binding.content.countdownTimer.start(
            timerDuration
        ) {
            executePanicMode()
        }
    }

    private fun setupPanicView() {
        if (Preferences.isQuickExit()) {
            binding.panicSeekContainer.visibility = View.VISIBLE
            binding.vaultRecyclerView.setMargins(null, null, null, 110)
        } else {
            binding.panicSeekContainer.visibility = View.GONE
            binding.vaultRecyclerView.setMargins(null, null, null, 55)
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
        reportServersCounted = true
        tuServers?.clear()
        serversList?.removeIf { item -> item.type == ServerType.TELLA_UPLOAD }
        serversList?.removeIf { item -> item.type == ServerType.TELLA_RESORCES }
        if (!servers.isNullOrEmpty()) {
            tuServers?.addAll(servers)
            serversList?.add(ServerDataItem(servers, ServerType.TELLA_UPLOAD))
            serversList?.add(ServerDataItem(servers, ServerType.TELLA_RESORCES))
        }
        maybeShowConnections()
    }

    override fun onCountTUServersFailed(throwable: Throwable?) {
        reportServersCounted = true
        Timber.d("***onCountTUServersFailed**$throwable")
    }

    override fun onCountCollectServersEnded(servers: List<CollectServer>?) {
        collectServersCounted = true
        collectServers?.clear()
        serversList?.removeIf { item -> item.type == ServerType.ODK_COLLECT }
        if (!servers.isNullOrEmpty()) {
            collectServers?.addAll(servers)
            serversList?.add(ServerDataItem(servers, ServerType.ODK_COLLECT))
        }
        maybeShowConnections()
    }


    override fun onCountCollectServersFailed(throwable: Throwable?) {
        collectServersCounted = true
        Timber.d("***onCountCollectServersFailed**$throwable")
    }

    override fun onCountUwaziServersEnded(servers: List<UWaziUploadServer>?) {
        uwaziServersCounted = true
        uwaziServers?.clear()
        serversList?.removeIf { item -> item.type == ServerType.UWAZI }
        if (!servers.isNullOrEmpty()) {
            uwaziServers?.addAll(servers)
            serversList?.add(ServerDataItem(servers, ServerType.UWAZI))
        }
        maybeShowConnections()
    }

    override fun onCountUwaziServersFailed(throwable: Throwable?) {
        uwaziServersCounted = true
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
        findNavController().navigate(R.id.action_homeScreen_to_attachments_screen, bundle)
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
        if (!Preferences.isShowRecentFiles() && !Preferences.isShowFavoriteForms() && serversList?.isEmpty() == false) {
            vaultAdapter.removeTitle()
        } else {
            vaultAdapter.addTitle()
        }
    }

    private fun fixAppBarShadow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.appbar.outlineProvider = null
        } else {
            binding.appbar.bringToFront()
        }
    }

}
