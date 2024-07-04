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
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Limits
import com.hzontal.tella_vault.filter.Sort
import com.hzontal.utils.MediaFile
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.appbar.ToolbarComponent
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.data.CommonPreferences
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.EventCompositeDisposable
import rs.readahead.washington.mobile.bus.EventObserver
import rs.readahead.washington.mobile.bus.event.RecentBackgroundActivitiesEvent
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.data.sharedpref.Preferences.isAlreadyMigratedMainDB
import rs.readahead.washington.mobile.data.sharedpref.Preferences.isShowFailedMigrationSheet
import rs.readahead.washington.mobile.data.sharedpref.Preferences.setShowFailedMigrationSheet
import rs.readahead.washington.mobile.databinding.FragmentVaultBinding
import rs.readahead.washington.mobile.domain.entity.ServerType
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.util.LockTimeoutManager
import rs.readahead.washington.mobile.util.TopSheetTestUtils.showBackgroundActivitiesSheet
import rs.readahead.washington.mobile.util.setMargins
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.activity.clean_insights.CleanInsightsActions
import rs.readahead.washington.mobile.views.activity.clean_insights.AnalyticsIntroActivity
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
    private lateinit var disposables: EventCompositeDisposable
    private var reportServersCounted = false
    private var collectServersCounted = false
    private var uwaziServersCounted = false
    private var isBackgroundEncryptionEnabled = false;
    private var descriptionLiveData = MutableLiveData<String>()
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
        showUpdateMigrationBottomSheet()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AnalyticsIntroActivity.CLEAN_INSIGHTS_REQUEST_CODE) {
            removeImprovementSection()
            val cleanInsightsActions =
                data?.extras?.getSerializable(AnalyticsIntroActivity.RESULT_FOR_ACTIVITY) as CleanInsightsActions
            showMessageForCleanInsightsApprove(cleanInsightsActions)
        }
    }

    private fun showMessageForCleanInsightsApprove(cleanInsightsActions: CleanInsightsActions) {
        if (cleanInsightsActions == CleanInsightsActions.YES) {
            CommonPreferences.setIsAcceptedAnalytics(true)
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
        vaultAdapter.addAnalyticsBanner()
        timerDuration = resources.getInteger(R.integer.panic_countdown_duration)

        serversList = ArrayList()
        tuServers = ArrayList()
        uwaziServers = ArrayList()
        collectServers = ArrayList()
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
        binding.panicSeekContainer.setOnClickListener { onPanicClicked() }
        binding.toolbar.onLeftClickListener = { nav().navigate(R.id.main_settings) }
        binding.toolbar.onRightClickListener = {
            MyApplication.getMainKeyHolder().timeout = LockTimeoutManager.IMMEDIATE_SHUTDOWN
            Preferences.setExitTimeout(true)
            MyApplication.exit(baseActivity)
            baseActivity.finish()
        }
    }

    private fun startCleanInsightActivity() {
        val intent = Intent(context, AnalyticsIntroActivity::class.java)
        startActivityForResult(intent, AnalyticsIntroActivity.CLEAN_INSIGHTS_REQUEST_CODE)
    }

    private fun setUpToolbar() {
        val baseActivity = activity as MainActivity
        baseActivity.setSupportActionBar(binding.toolbar)
        maybeShowRecentBackgroundActivities()
    }

    private fun maybeShowRecentBackgroundActivities() {
        disposables.wire(
            RecentBackgroundActivitiesEvent::class.java,
            object : EventObserver<RecentBackgroundActivitiesEvent?>() {
                override fun onNext(event: RecentBackgroundActivitiesEvent) {
                    handleBackgroundActivityEvent(event)
                }
            })

        setupToolbarClickListener()
    }

    private fun handleBackgroundActivityEvent(event: RecentBackgroundActivitiesEvent) {
        isBackgroundEncryptionEnabled = event.hasItems()
        Timber.i("RecentBackgroundActivitiesEvent came from event")

        if (isBackgroundEncryptionEnabled) {
            descriptionLiveData.postValue(getString(R.string.current_background_activities))
        } else {
            descriptionLiveData.postValue(getString(R.string.no_background_activity))
        }
        backgroundActivitiesAdapter.updateData(event.getBackgroundActivityModels())
        updateToolbarIcon()
    }

    private fun updateToolbarIcon() {
        if (view == null) {
            Timber.i("RecentBackgroundActivitiesEvent **** view is null")

            // Fragment's view is not available, cannot update the toolbar icon
            return
        }
        val iconRes = if (isBackgroundEncryptionEnabled) {
            R.drawable.ic_notification_on
        } else {
            -1
        }
        view?.findViewById<ToolbarComponent>(R.id.toolbar)?.setRightOfLeftIcon(iconRes)
    }

    private fun setupToolbarClickListener() {
        binding.toolbar.onRightOfLeftClickListener = {
            val description = if (backgroundActivitiesAdapter.itemCount == 0) {
                getString(R.string.no_background_activity)
            } else {
                getString(R.string.current_background_activities)
            }
            showBackgroundActivitiesSheet(
                baseActivity.supportFragmentManager,
                getString(R.string.background_activities),
                description,
                backgroundActivitiesAdapter,
                descriptionLiveData,
                lifecycleOwner = this
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

            MediaFile.isPDFFile(vaultFile.name, vaultFile.mimeType) -> {
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
                navManager().navigateFromHomeScreenToUwaziScreen()
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
                CommonPreferences.setIsAcceptedAnalytics(true)
                nav().navigate(R.id.main_settings)
            }
        }
    }

    private fun removeImprovementSection() {
        CommonPreferences.setShowVaultAnalyticsSection(false)
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
        updateToolbarIcon()
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
        if (this::disposables.isInitialized) {
            disposables.dispose()
        }
        // descriptionLiveData.removeObservers(viewLifecycleOwner)
    }


    override fun onCountTUServersEnded(servers: List<TellaReportServer>?) {
        reportServersCounted = true
        tuServers?.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            serversList?.removeIf { item -> item.type == ServerType.TELLA_UPLOAD }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            serversList?.removeIf { item -> item.type == ServerType.TELLA_RESORCES }
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            serversList?.removeIf { item -> item.type == ServerType.ODK_COLLECT }
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            serversList?.removeIf { item -> item.type == ServerType.UWAZI }
        }
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
        binding.appbar.outlineProvider = null
    }

    private fun showUpdateMigrationBottomSheet() {
        if (!isAlreadyMigratedMainDB() || !com.hzontal.utils.Preferences(baseActivity.applicationContext)
                .isAlreadyMigratedVaultDB()
        ) {
            if (isShowFailedMigrationSheet()) {

                BottomSheetUtils.showStandardSheet(baseActivity.supportFragmentManager,
                    getString(R.string.Migration_Failed_Title),
                    getString(R.string.Migration_Failed_Description),
                    null,
                    getString(R.string.action_ok).uppercase(),
                    onConfirmClick = {
                        setShowFailedMigrationSheet(false)
                    },
                    onCancelClick = {
                        setShowFailedMigrationSheet(false)
                    }
                )
            }
        }

    }

}
