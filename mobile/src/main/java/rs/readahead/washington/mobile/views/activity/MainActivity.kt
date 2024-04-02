package rs.readahead.washington.mobile.views.activity


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.OrientationEventListener
import android.view.View
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import dagger.hilt.android.AndroidEntryPoint
import permissions.dispatcher.NeedsPermission
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.EventCompositeDisposable
import rs.readahead.washington.mobile.bus.EventObserver
import rs.readahead.washington.mobile.bus.event.CamouflageAliasChangedEvent
import rs.readahead.washington.mobile.bus.event.LocaleChangedEvent
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.mvp.contract.IHomeScreenPresenterContract
import rs.readahead.washington.mobile.mvp.contract.IMediaImportPresenterContract
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract
import rs.readahead.washington.mobile.mvp.presenter.HomeScreenPresenter
import rs.readahead.washington.mobile.mvp.presenter.MediaImportPresenter
import rs.readahead.washington.mobile.util.C
import rs.readahead.washington.mobile.util.CleanInsightUtils
import rs.readahead.washington.mobile.util.CleanInsightUtils.measureEvent
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.views.fragment.reports.send.ReportsSendFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.SubmittedPreviewFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.VAULT_FILE_KEY
import rs.readahead.washington.mobile.views.fragment.uwazi.download.DownloadedTemplatesFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.send.UwaziSendFragment
import rs.readahead.washington.mobile.views.fragment.vault.attachements.AttachmentsFragment
import rs.readahead.washington.mobile.views.fragment.vault.home.VAULT_FILTER
import rs.readahead.washington.mobile.views.interfaces.IMainNavigationInterface
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class MainActivity : MetadataActivity(),
    IHomeScreenPresenterContract.IView,
    IMediaImportPresenterContract.IView,
    IMetadataAttachPresenterContract.IView,
    IMainNavigationInterface {

    companion object {
        const val PHOTO_VIDEO_FILTER = "gallery_filter"
    }

    private var mExit = false
    private val handler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }
    private lateinit var disposables: EventCompositeDisposable
    private lateinit var homeScreenPresenter: HomeScreenPresenter
    private lateinit var mediaImportPresenter: MediaImportPresenter
    private var progressBar: ProgressBar? = null
    private var mOrientationEventListener: OrientationEventListener? = null
    private lateinit var btmNavMain: BottomNavigationView
    private lateinit var navController: NavController
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Your onBackPressed logic here
            if (checkCurrentFragment()) return
            if (!checkIfShouldExit()) return
            closeApp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        setupNavigation()
        homeScreenPresenter = HomeScreenPresenter(this)
        mediaImportPresenter = MediaImportPresenter(this)
        initializeListeners()
        // todo: check this..
        //SafetyNetCheck.setApiKey(getString(R.string.share_in_report));
        if (intent.hasExtra(PHOTO_VIDEO_FILTER)) {
            val bundle = Bundle()
            bundle.putString(VAULT_FILTER, FilterType.PHOTO_VIDEO.name)
            navController.navigate(R.id.action_homeScreen_to_attachments_screen, bundle)
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    }

    private fun initializeListeners() {
        setOrientationListener()
        disposables = MyApplication.bus().createCompositeDisposable()
        disposables.wire(
            LocaleChangedEvent::class.java,
            object : EventObserver<LocaleChangedEvent?>() {
                override fun onNext(event: LocaleChangedEvent) {
                    recreate()
                }
            })
        disposables.wire(
            CamouflageAliasChangedEvent::class.java,
            object : EventObserver<CamouflageAliasChangedEvent?>() {
                override fun onNext(event: CamouflageAliasChangedEvent) {
                    closeApp()
                }
            })
    }

    private fun setupNavigation() {
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.fragment_nav_host) as NavHostFragment?)!!
        navController = navHostFragment.navController
        btmNavMain = findViewById(R.id.btm_nav_main)
        setupWithNavController(btmNavMain, navController)
        navController.addOnDestinationChangedListener { _: NavController?, navDestination: NavDestination, _: Bundle? ->
            when (navDestination.id) {
                R.id.micScreen, R.id.homeScreen, R.id.main_settings-> showBottomNavigation()
                else -> hideBottomNavigation()
            }
        }
    }

    private fun handleImportResult(requestCode: Int, data: Intent?) {
        try {
            if (data != null) {
                val uri = data.data
                if (uri != null) {
                    when (requestCode) {
                        C.IMPORT_VIDEO -> mediaImportPresenter.importVideo(uri)
                        C.IMPORT_IMAGE -> mediaImportPresenter.importImage(uri)
                        C.IMPORT_FILE -> mediaImportPresenter.importFile(uri)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle import results
        if (requestCode == C.IMPORT_VIDEO || requestCode == C.IMPORT_IMAGE || requestCode == C.IMPORT_FILE) {
            handleImportResult(requestCode, data)
            return
        }

        // Handle location settings requests and non-import cases
        if (resultCode != RESULT_OK && !isLocationSettingsRequestCode(requestCode)) {
            // User canceled evidence acquiring
            return
        }

        // Delegate onActivityResult to child fragments
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.forEach {
            it.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun isLocationSettingsRequestCode(requestCode: Int): Boolean {
        return requestCode == C.START_CAMERA_CAPTURE || requestCode == C.START_AUDIO_RECORD
    }

    @SuppressLint("NeedOnRequestPermissionsResult")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun checkCurrentFragment(): Boolean {
        val fragments =
            supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments
        fragments?.forEach { fragment ->
            when (fragment) {
                is AttachmentsFragment -> {
                    if (fragment.onBackPressed()) {
                        return true
                    }
                }

                is DownloadedTemplatesFragment,
                is SubmittedPreviewFragment,
                is UwaziSendFragment -> {
                    navController.popBackStack()
                    return true
                }

                is UwaziEntryFragment -> {
                    if (fragment.onBackPressed()) {
                        return true
                    }
                }

                is ReportsSendFragment -> {
                    if (fragment.onBackPressed()) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun closeApp() {
        finish()
        lockApp()
    }

    private fun checkIfShouldExit(): Boolean {
        if (!mExit) {
            mExit = true
            showToast(R.string.home_toast_back_exit)
            handler.postDelayed({ mExit = false }, (3 * 1000).toLong())
            return false
        }
        return true
    }

    private fun lockApp() {
        if (!isLocked) {
            MyApplication.resetKeys()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
        stopPresenter()
        hideProgressBar()
    }

    override fun onResume() {
        super.onResume()
        startLocationMetadataListening()
        mOrientationEventListener!!.enable()
    }

    override fun onPause() {
        super.onPause()
        stopLocationMetadataListening()
        mOrientationEventListener!!.disable()
    }

    override fun onMetadataAttached(vaultFile: VaultFile) {
        val data = Intent()
        data.putExtra(C.CAPTURED_MEDIA_FILE_ID, vaultFile.id)
        setResult(RESULT_OK, data)
    }

    override fun onMetadataAttachError(throwable: Throwable?) {
        // onAddError(throwable);
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startCollectFormEntryActivity() {
        startActivity(Intent(this, CollectFormEntryActivity::class.java))
    }

    override fun onMediaFileImported(vaultFile: VaultFile) {
        val list: MutableList<String> = ArrayList()
        list.add(vaultFile.id)
        onActivityResult(
            C.MEDIA_FILE_ID,
            RESULT_OK,
            Intent().putExtra(VAULT_FILE_KEY, Gson().toJson(list))
        )
    }

    override fun onImportError(error: Throwable?) {}

    override fun onImportStarted() {}

    override fun onImportEnded() {}

    override fun getContext(): Context {
        return this
    }

    override fun onCountTUServersEnded(num: Long) {
        //if (num > 0) {
        //  CleanInsightUtils.INSTANCE.measureEvent(CleanInsightUtils.ServerType.SERVER_TELLA);
        //  maybeShowTUserver(num);
        //   }
    }

    override fun onCountTUServersFailed(throwable: Throwable?) {
        Timber.d(throwable)
    }

    override fun onCountCollectServersEnded(num: Long) {
        // maybeShowFormsMenu(num);
        if (num > 0) {
            measureEvent(CleanInsightUtils.ServerType.SERVER_COLLECT)
        }
    }

    override fun onCountCollectServersFailed(throwable: Throwable?) {}

    override fun onCountUwaziServersEnded(num: Long) {
        // maybeShowUwaziMenu(num);
        if (num > 0) measureEvent(CleanInsightUtils.ServerType.SERVER_UWAZI)
    }

    override fun onCountUwaziServersFailed(throwable: Throwable?) {}

    private fun stopPresenter() {
        homeScreenPresenter.destroy()
        mediaImportPresenter.destroy()
        // mediaImportPresenter = null
    }

    private fun hideProgressBar() {
        progressBar?.hide()
    }

    private fun setOrientationListener() {
        mOrientationEventListener =
            object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
                override fun onOrientationChanged(orientation: Int) {
                    //if (!isInCameraMode) return;
                    if (orientation == ORIENTATION_UNKNOWN) {
                        return
                    }
                    // handle rotation for tablets;
                }
            }
    }

    override fun hideBottomNavigation() {
        btmNavMain.visibility = View.GONE
    }

    override fun showBottomNavigation() {
        btmNavMain.visibility = View.VISIBLE
    }

    fun selectNavMic() {
        btmNavMain.menu.findItem(R.id.mic).isChecked = true
    }

    fun selectHome() {
        btmNavMain.menu.findItem(R.id.home).isChecked = true
        navController.navigate(R.id.home)
    }

}
