package org.horizontal.tella.mobile.views.fragment.vault.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Limits
import com.hzontal.tella_vault.filter.Sort
import com.hzontal.utils.MediaFile
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.EventCompositeDisposable
import org.horizontal.tella.mobile.bus.EventObserver
import org.horizontal.tella.mobile.bus.event.RecentBackgroundActivitiesEvent
import org.horizontal.tella.mobile.data.sharedpref.Preferences
import org.horizontal.tella.mobile.data.sharedpref.Preferences.isAlreadyMigratedMainDB
import org.horizontal.tella.mobile.data.sharedpref.Preferences.isFreshInstall
import org.horizontal.tella.mobile.data.sharedpref.Preferences.isShowFailedMigrationSheet
import org.horizontal.tella.mobile.data.sharedpref.Preferences.setIsAcceptedAnalytics
import org.horizontal.tella.mobile.data.sharedpref.Preferences.setShowFailedMigrationSheet
import org.horizontal.tella.mobile.data.sharedpref.Preferences.setShowVaultAnalyticsSection
import org.horizontal.tella.mobile.databinding.FragmentVaultBinding
import org.horizontal.tella.mobile.domain.entity.ServerType
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.domain.entity.collect.CollectForm
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer
import org.horizontal.tella.mobile.domain.entity.googledrive.Config
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import org.horizontal.tella.mobile.domain.entity.nextcloud.NextCloudServer
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziTemplate
import org.horizontal.tella.mobile.util.LockTimeoutManager
import org.horizontal.tella.mobile.util.TopSheetTestUtils.showBackgroundActivitiesSheet
import org.horizontal.tella.mobile.util.setMargins
import org.horizontal.tella.mobile.views.activity.CollectFormEntryActivity
import org.horizontal.tella.mobile.views.activity.MainActivity
import org.horizontal.tella.mobile.views.activity.analytics.AnalyticsActions
import org.horizontal.tella.mobile.views.activity.analytics.AnalyticsIntroActivity
import org.horizontal.tella.mobile.views.activity.viewer.AudioPlayActivity
import org.horizontal.tella.mobile.views.activity.viewer.PDFReaderActivity
import org.horizontal.tella.mobile.views.activity.viewer.PhotoViewerActivity
import org.horizontal.tella.mobile.views.activity.viewer.VideoViewerActivity
import org.horizontal.tella.mobile.views.base_ui.BaseFragment
import org.horizontal.tella.mobile.views.custom.CountdownTextView
import org.horizontal.tella.mobile.views.fragment.forms.LOCATION_REQUEST_CODE
import org.horizontal.tella.mobile.views.fragment.forms.SharedFormsViewModel
import org.horizontal.tella.mobile.views.fragment.uwazi.entry.COLLECT_TEMPLATE
import org.horizontal.tella.mobile.views.fragment.vault.adapters.ImproveClickOptions
import org.horizontal.tella.mobile.views.fragment.vault.adapters.VaultAdapter
import org.horizontal.tella.mobile.views.fragment.vault.adapters.VaultClickListener
import org.horizontal.tella.mobile.views.fragment.vault.adapters.connections.ServerDataItem
import org.horizontal.tella.mobile.views.fragment.vault.home.background_activities.BackgroundActivitiesAdapter
import org.hzontal.shared_ui.appbar.ToolbarComponent
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import org.javarosa.core.model.FormDef
import permissions.dispatcher.NeedsPermission
import timber.log.Timber
import javax.inject.Inject


const val VAULT_FILTER = "vf"

@AndroidEntryPoint
class HomeVaultFragment : BaseFragment(), VaultClickListener {
    private lateinit var toolbar: ToolbarComponent
    private var timerDuration = 0
    private var panicActivated = false
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    private var writePermissionGranted = false
    private var vaultFile: VaultFile? = null
    private var serversList: MutableList<ServerDataItem>? = null
    private var tuServers: ArrayList<TellaReportServer>? = null
    private var uwaziServers: ArrayList<UWaziUploadServer>? = null
    private var collectServers: ArrayList<CollectServer>? = null
    private var googleDriveServers: ArrayList<GoogleDriveServer>? = null
    private var dropBoxServers: ArrayList<DropBoxServer>? = null
    private var nextCloudServers: ArrayList<NextCloudServer>? = null
    private var favoriteForms: ArrayList<CollectForm>? = null
    private lateinit var disposables: EventCompositeDisposable
    private var reportServersCounted = false
    private var collectServersCounted = false
    private var uwaziServersCounted = false
    private var googleDriveServersCounted = false
    private var dropBoxServersCounted = false
    private var nextCloudServersCounted = false
    private var isBackgroundEncryptionEnabled = false
    private val backgroundActivitiesAdapter by lazy { BackgroundActivitiesAdapter(mutableListOf()) }
    private lateinit var vaultRecyclerView: RecyclerView
    private lateinit var panicModeView: RelativeLayout
    private lateinit var countDownTextView: CountdownTextView
    private lateinit var seekBar: SeekBar
    private lateinit var seekBarContainer: View
    private val vaultAdapter by lazy { VaultAdapter(this) }
    private val homeVaultViewModel: HomeVaultViewModel by viewModels()
    private val sharedFormsViewModel: SharedFormsViewModel by viewModels()
    private lateinit var binding: FragmentVaultBinding
    private var descriptionLiveData = MutableLiveData<String>()

    @Inject
    lateinit var config: Config

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentVaultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        initView(binding.root)
    }

    override fun initView(view: View) {
        // Use binding to access views
        with(binding) {
            this@HomeVaultFragment.toolbar = toolbar
            this@HomeVaultFragment.vaultRecyclerView = vaultRecyclerView
            seekBar = panicSeek
            seekBarContainer = panicSeekContainer
        }
        panicModeView = view.findViewById(R.id.panic_mode_view)
        countDownTextView = view.findViewById(R.id.countdown_timer)
        disposables = MyApplication.bus().createCompositeDisposable()

        // Initialize other components
        setUpToolbar()
        initData()
        initListeners()
        initPermissions()
        fixAppBarShadow(binding.root)
        showUpdateMigrationBottomSheet()
    }

    private fun initObservers() {
        homeVaultViewModel.apply {
            // Observe server counts
            serverCounts.observe(viewLifecycleOwner) { serverCounts ->
                handleServerCountsSuccess(serverCounts)
            }

            serverCountError.observe(viewLifecycleOwner) { error ->
                handleServerCountsError(error)
            }

            mediaExportStatus.observe(viewLifecycleOwner) { status ->
                when (status) {
                    HomeVaultViewModel.ExportStatus.STARTED -> onMediaExportStarted()
                    HomeVaultViewModel.ExportStatus.ENDED -> onMediaExportEnded()
                    HomeVaultViewModel.ExportStatus.SUCCESS -> onMediaExported()
                    HomeVaultViewModel.ExportStatus.ERROR -> onExportError()
                }
            }
            recentFiles.observe(viewLifecycleOwner) { files ->
                handleRecentFilesSuccess(files)
            }

            recentFilesError.observe(viewLifecycleOwner) { error ->
                handleRecentFilesError(error)
            }

            // Observe favorite collect forms
            favoriteCollectForms.observe(viewLifecycleOwner) { forms ->
                if (forms.isNullOrEmpty()) {
                    vaultAdapter.removeFavoriteForms()
                    return@observe
                }
                handleFavoriteCollectFormsSuccess(forms)
            }

            // Observe errors
            favoriteCollectFormsError.observe(viewLifecycleOwner) { error ->
                handleFavoriteCollectFormsError(error)
            }
            // Observe favorite collect templates
            favoriteCollectTemplates.observe(viewLifecycleOwner) { templates ->
                handleFavoriteCollectTemplatesSuccess(templates)
            }

            // Observe errors for collect templates
            favoriteCollectTemplatesError.observe(viewLifecycleOwner) { error ->
                handleFavoriteCollectTemplatesError(error)
            }
        }

        sharedFormsViewModel.onGetBlankFormDefSuccess.observe(viewLifecycleOwner) { result ->
            result.let {
                startCreateFormControllerPresenter(it.form, it.formDef)
            }
        }

        sharedFormsViewModel.onCreateFormController.observe(viewLifecycleOwner) {
            if (Preferences.isAnonymousMode()) {
                startCollectFormEntryActivity() // no need to check for permissions, as location won't be turned on
            } else {
                if (!hasLocationPermissions(baseActivity)) {
                    requestLocationPermissions()
                } else {
                    startCollectFormEntryActivity() // no need to check for permissions, as location won't be turned on
                }
            }
        }
    }

    private fun startCreateFormControllerPresenter(form: CollectForm, formDef: FormDef) {
        sharedFormsViewModel.createFormController(form, formDef)
    }

    private fun hasLocationPermissions(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        baseActivity.maybeChangeTemporaryTimeout()
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
        )
        ActivityCompat.requestPermissions(
            //1
            baseActivity,
            //2
            permissions,
            //3
            LOCATION_REQUEST_CODE
        )
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
    }

    private fun initData() {
        vaultRecyclerView.apply {
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
        googleDriveServers = ArrayList()
        dropBoxServers = ArrayList()
        nextCloudServers = ArrayList()

    }

    private fun setUpToolbar() {
        val baseActivity = activity as MainActivity
        baseActivity.setSupportActionBar(toolbar)
        maybeShowRecentBackgroundActivities()
    }

    private fun maybeShowRecentBackgroundActivities() {
        disposables.wire(RecentBackgroundActivitiesEvent::class.java,
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
        view?.findViewById<ToolbarComponent>(R.id.toolbar)?.setRightOfLeftIcon(iconRes)
    }

    private fun setupToolbarClickListener() {
        toolbar.onRightOfLeftClickListener = {
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
                BottomSheetUtils.showStandardSheet(baseActivity.supportFragmentManager,
                    baseActivity.getString(R.string.Vault_Export_SheetAction) + " " + vaultFile.name + "?",
                    baseActivity.getString(R.string.Vault_ViewerOther_SheetDesc),
                    baseActivity.getString(R.string.Vault_Export_SheetAction),
                    baseActivity.getString(R.string.action_cancel),
                    onConfirmClick = { exportVaultFiles(vaultFile) })
            }
        }
    }

    override fun onFavoriteItemClickListener(form: CollectForm) {
        sharedFormsViewModel.getBlankFormDef(form)
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun startCollectFormEntryActivity() {
        startActivity(Intent(activity, CollectFormEntryActivity::class.java))
    }

    override fun onFavoriteTemplateClickListener(template: UwaziTemplate) {
        bundle.putString(COLLECT_TEMPLATE, Gson().toJson(template))
        navManager().navigateFromHomeScreenToUwaziEntryScreen()
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

            ServerType.GOOGLE_DRIVE -> {
                nav().navigate(R.id.action_homeScreen_to_google_drive_screen)
            }

            ServerType.DROP_BOX -> {
                nav().navigate(R.id.action_homeScreen_to_drop_box_screen)
            }

            ServerType.NEXTCLOUD -> {
                nav().navigate(R.id.action_homeScreen_to_next_cloud_screen)
            }

            else -> {}
        }
    }

    override fun onImproveItemClickListener(improveClickOptions: ImproveClickOptions) {
        when (improveClickOptions) {
            ImproveClickOptions.CLOSE -> removeImprovementSection()
            ImproveClickOptions.YES -> {
                removeImprovementSection()
                showMessageForCleanInsightsApprove(AnalyticsActions.YES)
            }

            ImproveClickOptions.LEARN_MORE -> startAnalyticsActivity()
            ImproveClickOptions.SETTINGS -> {
                removeImprovementSection()
                setIsAcceptedAnalytics(true)
                nav().navigate(R.id.main_settings)
            }
        }
    }

    private fun startAnalyticsActivity() {
        val intent = Intent(context, AnalyticsIntroActivity::class.java)
        startActivityForResult(intent, AnalyticsIntroActivity.CLEAN_INSIGHTS_REQUEST_CODE)
    }

    private fun showMessageForCleanInsightsApprove(analyticsActions: AnalyticsActions) {
        if (analyticsActions == AnalyticsActions.YES) {
            setIsAcceptedAnalytics(true)
            baseActivity.divviupUtils.runInstallEvent()
            DialogUtils.showBottomMessage(
                requireActivity(), getString(R.string.Settings_Analytics_turn_on_dialog), false
            )
        }
    }

    private fun removeImprovementSection() {
        setShowVaultAnalyticsSection(false)
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
        maybeCountServers()
        maybeGetFiles()
        maybeGetRecentForms()
        maybeHideFilesTitle()
        maybeGetRecentTemplates()
        updateToolbarIcon()

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
            homeVaultViewModel.getRecentFiles(FilterType.ALL_WITHOUT_DIRECTORY, sort, limits)
        } else {
            vaultAdapter.removeRecentFiles()
        }
    }

    private fun maybeGetRecentForms() {
        if (Preferences.isShowFavoriteForms()) {
            homeVaultViewModel.getFavoriteCollectForms()
        } else {
            vaultAdapter.removeFavoriteForms()
        }
    }

    private fun maybeGetRecentTemplates() {
        if (Preferences.isShowFavoriteTemplates()) {
            homeVaultViewModel.getFavoriteCollectTemplates()
        } else {
            vaultAdapter.removeFavoriteTemplates()
        }
    }

    private fun maybeCountServers() {
        clearServerCount()
        homeVaultViewModel.countAllServers()
    }

    private fun clearServerCount() {
        googleDriveServersCounted = false
        dropBoxServersCounted = false
        nextCloudServersCounted = false
        reportServersCounted = false
        collectServersCounted = false
        uwaziServersCounted = false
        serversList?.clear()
        vaultAdapter.removeConnectionServers()
    }

    private fun maybeClosePanic(): Boolean {
        if (panicModeView.visibility == View.VISIBLE) {
            stopPanicking()
            hidePanicScreens()
        }
        return false
    }

    private fun hidePanicScreens() {
        (baseActivity as MainActivity).showBottomNavigation()
        setupPanicView()
        panicModeView.visibility = View.GONE
        toolbar.visibility = View.VISIBLE
    }

    private fun showPanicScreens() {
        (baseActivity as MainActivity).hideBottomNavigation()
        toolbar.visibility = View.GONE
        panicModeView.visibility = View.VISIBLE
        panicModeView.alpha = 1f
        countDownTextView.start(timerDuration) {
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
            baseActivity.divviupUtils.runQuickDeleteEvent()
            homeVaultViewModel.executePanicMode()
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

    private fun handleRecentFilesSuccess(files: List<VaultFile?>) {
        if (files.isNotEmpty()) {
            vaultAdapter.addRecentFiles(files)
        } else {
            vaultAdapter.removeRecentFiles()
        }
    }

    private fun handleRecentFilesError(error: Throwable?) {
        error?.let {
            Timber.e(it, "Error fetching recent files in HomeFragment")
            Toast.makeText(
                requireContext(), "Error fetching recent files: ${it.message}", Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun onMediaExportStarted() {
        baseActivity.toggleLoading(true) // Show loading indicator
    }

    private fun onMediaExportEnded() {
        baseActivity.toggleLoading(false) // Hide loading indicator
    }

    private fun onMediaExported() {
        baseActivity.toggleLoading(false)
    }

    private fun onExportError() {
        // Handle export error, e.g., show a failure message
        DialogUtils.showBottomMessage(
            baseActivity, getString(R.string.gallery_toast_fail_exporting_to_device), false
        )
    }

    private fun maybeHideFilesTitle() {
        if (!Preferences.isShowRecentFiles() && !Preferences.isShowFavoriteForms() && serversList?.isEmpty() == false) {
            vaultAdapter.removeTitle()
        } else {
            vaultAdapter.addTitle()
        }
    }

    private fun handleFavoriteCollectFormsSuccess(files: List<CollectForm>) {
        if (files.isNotEmpty()) {
            vaultAdapter.addFavoriteForms(files)
        } else {
            vaultAdapter.removeFavoriteForms()
        }
    }

    private fun handleFavoriteCollectFormsError(error: Throwable?) {
        error?.let {
            Timber.d(it, "Error fetching favorite collect forms")
        }
    }

    private fun handleFavoriteCollectTemplatesSuccess(files: List<UwaziTemplate>) {
        if (files.isNotEmpty()) {
            vaultAdapter.addFavoriteTemplates(files)
        } else {
            vaultAdapter.removeFavoriteTemplates()
        }
    }

    private fun handleFavoriteCollectTemplatesError(error: Throwable?) {
        error?.let {
            Timber.d(it, "Error fetching favorite collect templates")
        }
    }

    /**
     * This function show connections when all the server types are counted.
     **/
    private fun maybeShowConnections() {
        // If the serversList is not empty, check if it has changed
        if (serversList?.isEmpty() == false) {
            // Use the vaultAdapter to check existing connections
            vaultAdapter.addConnectionServers(serversList!!)

        } else {
            vaultAdapter.removeConnectionServers()
        }
    }

    private fun handleServerCountsSuccess(serverCounts: ServerCounts) {
        // Handle each server type
        handleGoogleDriveServers(serverCounts.googleDriveServers)
        handleDropBoxServers(serverCounts.dropBoxServers)
        handleNextCloudServers(serverCounts.nextCloudServers)
        handleTellaUploadServers(serverCounts.tellaUploadServers)
        handleCollectServers(serverCounts.collectServers)
        handleUwaziServers(serverCounts.uwaziServers)

        // Check if we need to show connections
        maybeShowConnections()
    }

    private fun handleServerCountsError(error: Throwable?) {
        error?.let {
            Timber.d("***onServerCountFailed**$it")
        }
    }

    // Handle Google Drive servers
    private fun handleGoogleDriveServers(servers: List<GoogleDriveServer>?) {
        googleDriveServersCounted = true
        googleDriveServers?.clear()
        removeOldServersFromList(ServerType.GOOGLE_DRIVE)

        if (!servers.isNullOrEmpty()) {
            googleDriveServers?.addAll(servers)
            serversList?.add(ServerDataItem(servers, ServerType.GOOGLE_DRIVE))
        }
    }

    // Handle Dropbox servers
    private fun handleDropBoxServers(servers: List<DropBoxServer>?) {
        dropBoxServersCounted = true
        dropBoxServers?.clear()
        removeOldServersFromList(ServerType.DROP_BOX)

        if (!servers.isNullOrEmpty()) {
            dropBoxServers?.addAll(servers)
            serversList?.add(ServerDataItem(servers, ServerType.DROP_BOX))
        }
    }

    // Handle Tella upload servers
    private fun handleTellaUploadServers(servers: List<TellaReportServer>?) {
        reportServersCounted = true
        tuServers?.clear()
        removeOldServersFromList(ServerType.TELLA_UPLOAD, ServerType.TELLA_RESORCES)

        if (!servers.isNullOrEmpty()) {
            tuServers?.addAll(servers)
            serversList?.add(ServerDataItem(servers, ServerType.TELLA_UPLOAD))
            serversList?.add(ServerDataItem(servers, ServerType.TELLA_RESORCES))
        }
    }

    // Handle Collect servers
    private fun handleCollectServers(servers: List<CollectServer>?) {
        collectServersCounted = true
        collectServers?.clear()
        removeOldServersFromList(ServerType.ODK_COLLECT)

        if (!servers.isNullOrEmpty()) {
            collectServers?.addAll(servers)
            serversList?.add(ServerDataItem(servers, ServerType.ODK_COLLECT))
        }
    }

    // Handle Uwazi servers
    private fun handleUwaziServers(servers: List<UWaziUploadServer>?) {
        uwaziServersCounted = true
        uwaziServers?.clear()
        removeOldServersFromList(ServerType.UWAZI)

        if (!servers.isNullOrEmpty()) {
            uwaziServers?.addAll(servers)
            serversList?.add(ServerDataItem(servers, ServerType.UWAZI))
        }
    }

    private fun handleNextCloudServers(servers: List<NextCloudServer>?) {
        nextCloudServersCounted = true
        nextCloudServers?.clear()
        removeOldServersFromList(ServerType.NEXTCLOUD)

        if (!servers.isNullOrEmpty()) {
            nextCloudServers?.addAll(servers)
            serversList?.add(ServerDataItem(servers, ServerType.NEXTCLOUD))
        }
    }

    private fun removeOldServersFromList(vararg serverTypes: ServerType) {
        serverTypes.forEach { type ->
            val iterator = serversList?.iterator()
            while (iterator?.hasNext() == true) {
                if (iterator.next().type == type) {
                    iterator.remove()
                }
            }
        }
    }


    private fun navigateToAttachmentsList(bundle: Bundle?) {
        findNavController().navigate(R.id.action_homeScreen_to_attachments_screen, bundle)
    }

    private fun exportVaultFiles(vaultFile: VaultFile) {
        this.vaultFile = vaultFile
        if (writePermissionGranted) {
            vaultFile.let { homeVaultViewModel.exportMediaFiles(arrayListOf(vaultFile)) }
        } else {
            updateOrRequestPermissions()
        }
    }

    private fun updateOrRequestPermissions() {
        baseActivity.maybeChangeTemporaryTimeout()
        val hasWritePermission = ContextCompat.checkSelfPermission(
            baseActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE
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


    private fun fixAppBarShadow(view: View) {
        val appBar = view.findViewById<View>(R.id.appbar)
        appBar.outlineProvider = null
    }

    private fun showUpdateMigrationBottomSheet() {
        if (isFreshInstall()) return

        val preferences = com.hzontal.utils.Preferences(baseActivity.applicationContext)
        val isMainDBMigrated = isAlreadyMigratedMainDB()
        val isVaultDBMigrated = preferences.isAlreadyMigratedVaultDB()

        if (isMainDBMigrated && isVaultDBMigrated) return

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
                })
        }
    }

}