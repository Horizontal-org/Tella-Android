package rs.readahead.washington.mobile.views.activity


import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.view.OrientationEventListener
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import permissions.dispatcher.NeedsPermission
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.EventCompositeDisposable
import rs.readahead.washington.mobile.bus.EventObserver
import rs.readahead.washington.mobile.bus.event.CamouflageAliasChangedEvent
import rs.readahead.washington.mobile.bus.event.LocaleChangedEvent
import rs.readahead.washington.mobile.mvp.contract.IHomeScreenPresenterContract
import rs.readahead.washington.mobile.mvp.contract.IMediaImportPresenterContract
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract
import rs.readahead.washington.mobile.mvp.presenter.HomeScreenPresenter
import rs.readahead.washington.mobile.mvp.presenter.MediaImportPresenter
import rs.readahead.washington.mobile.util.C
import rs.readahead.washington.mobile.util.CleanInsightUtils
import rs.readahead.washington.mobile.util.CleanInsightUtils.measureEvent
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

class MainActivity : MetadataActivity(),
    IHomeScreenPresenterContract.IView,
    IMediaImportPresenterContract.IView,
    IMetadataAttachPresenterContract.IView,
    IMainNavigationInterface {

    companion object {
        const val PHOTO_VIDEO_FILTER = "gallery_filter"
    }

    private var mExit = false
    private var handler = Handler()
    private lateinit var disposables : EventCompositeDisposable
    private lateinit var homeScreenPresenter: HomeScreenPresenter
    private lateinit var mediaImportPresenter: MediaImportPresenter
    private var progressDialog: ProgressDialog? = null
    private var mOrientationEventListener: OrientationEventListener? = null
    private lateinit var btmNavMain: BottomNavigationView
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        setupNavigation()
        handler = Handler()
        homeScreenPresenter = HomeScreenPresenter(this)
        mediaImportPresenter = MediaImportPresenter(this)
        initSetup()
        // todo: check this..
        //SafetyNetCheck.setApiKey(getString(R.string.share_in_report));
        if (intent.hasExtra(MainActivity.PHOTO_VIDEO_FILTER)) {
            val bundle = Bundle()
            bundle.putString(VAULT_FILTER, FilterType.PHOTO_VIDEO.name)
            navController.navigate(R.id.action_homeScreen_to_attachments_screen, bundle)
        }
    }

    private fun initSetup() {
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
        navController.addOnDestinationChangedListener { navController1: NavController?, navDestination: NavDestination, bundle: Bundle? ->
            when (navDestination.id) {
                R.id.micScreen, R.id.homeScreen, R.id.cameraScreen -> showBottomNavigation()
                else -> hideBottomNavigation()
            }
        }
    }

    private fun isLocationSettingsRequestCode(requestCode: Int): Boolean {
        return requestCode == C.START_CAMERA_CAPTURE ||
                requestCode == C.START_AUDIO_RECORD
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == C.IMPORT_VIDEO) {
            if (data != null) {
                val video = data.data
                if (video != null) {
                    mediaImportPresenter.importVideo(video)
                }
            }
            return
        }
        if (requestCode == C.IMPORT_IMAGE) {
            if (data != null) {
                val image = data.data
                if (image != null) {
                    mediaImportPresenter.importImage(image)
                }
            }
            return
        }
        if (requestCode == C.IMPORT_FILE) {
            if (data != null) {
                val file = data.data
                if (file != null) {
                    mediaImportPresenter.importFile(file)
                }
            }
            return
        }
        if (!isLocationSettingsRequestCode(requestCode) && resultCode != RESULT_OK) {
            return  // user canceled evidence acquiring
        }
        val fragments = Objects.requireNonNull(
            supportFragmentManager.primaryNavigationFragment
        )?.childFragmentManager?.fragments
        for (fragment in fragments!!) {
            fragment.onActivityResult(requestCode, resultCode, data)
        }
    }

    @SuppressLint("NeedOnRequestPermissionsResult")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    override fun onBackPressed() {
        // if (maybeCloseCamera()) return;
        if (checkCurrentFragment()) return
        if (!checkIfShouldExit()) return
        closeApp()
    }

    private fun checkCurrentFragment(): Boolean {
        val fragments = Objects.requireNonNull(
            supportFragmentManager.primaryNavigationFragment
        )?.childFragmentManager?.fragments
        for (fragment in fragments!!) {
            if (fragment is AttachmentsFragment) {
                fragment.onBackPressed()
                return true
            }
            if (fragment is DownloadedTemplatesFragment ||
                fragment is SubmittedPreviewFragment ||
                fragment is UwaziSendFragment
            ) {
                navController.popBackStack()
                return true
            }
            if (fragment is UwaziEntryFragment) {
                fragment.onBackPressed()
                return true
            }
            if (fragment is ReportsSendFragment) {
                fragment.onBackPressed()
                return true
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

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (disposables != null) {
            disposables.dispose()
        }
        stopPresenter()
        hideProgressDialog()
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

    override fun onStop() {
        super.onStop()
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

    override fun getContext(): Context? {
        return this
    }

    override fun onCountTUServersEnded(num: Long) {
        if (num > 0) {
            //  CleanInsightUtils.INSTANCE.measureEvent(CleanInsightUtils.ServerType.SERVER_TELLA);
            //  maybeShowTUserver(num);
        }
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
        if (homeScreenPresenter != null) {
            homeScreenPresenter.destroy()
         //   homeScreenPresenter = null
        }
        mediaImportPresenter.destroy()
       // mediaImportPresenter = null
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
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
