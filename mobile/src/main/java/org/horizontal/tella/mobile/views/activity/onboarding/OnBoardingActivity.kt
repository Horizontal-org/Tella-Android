package org.horizontal.tella.mobile.views.activity.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.IS_ONBOARD_LOCK_SET
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.sharedpref.Preferences
import org.horizontal.tella.mobile.databinding.ActivityOnboardingBinding
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.views.base_ui.BaseActivity
import org.horizontal.tella.mobile.views.dialog.CollectServerDialogFragment
import org.horizontal.tella.mobile.views.dialog.SharedLiveData.createReportsServer
import org.horizontal.tella.mobile.views.dialog.SharedLiveData.createServer
import org.horizontal.tella.mobile.views.dialog.TellaUploadServerDialogFragment.TellaUploadServerDialogHandler
import org.horizontal.tella.mobile.views.dialog.reports.ReportsConnectFlowActivity
import org.horizontal.tella.mobile.views.dialog.uwazi.UwaziConnectFlowActivity
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils

private const val ONBOARDING_INTRODUCTION_VIEW_INDEX = 0
private const val ONBOARDING_CAMERA_VIEW_INDEX = 1
private const val ONBOARDING_RECORDER_VIEW_INDEX = 2
private const val ONBOARDING_FILES_VIEW_INDEX = 3
private const val ONBOARDING_COLLECT_DATA_VIEW = 4
private const val ONBOARDING_LOCK_VIEW_INDEX = 5

@AndroidEntryPoint
class OnBoardingActivity : BaseActivity(), OnBoardActivityInterface,
    IOnBoardPresenterContract.IView, TellaUploadServerDialogHandler {

    private var viewpagerItemsCount = 0
    private val isFromSettings by lazy { intent.getBooleanExtra(IS_FROM_SETTINGS, false) }
    private val isOnboardLockSet by lazy { intent.getBooleanExtra(IS_ONBOARD_LOCK_SET, false) }
    private val presenter by lazy { OnBoardPresenter(this) }
    private lateinit var binding: ActivityOnboardingBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)

        overridePendingTransition(
            com.hzontal.tella_locking_ui.R.anim.`in`, com.hzontal.tella_locking_ui.R.anim.out
        )
        setContentView(binding.root)
        applyEdgeToEdge(binding.root)

        // Instantiate a ViewPager and a Tablayout
        if (!isOnboardLockSet && !isFromSettings) initViewPager(6)

        // Instantiate next and back buttons
        initButtons()

        if (isOnboardLockSet) {
            Preferences.setFirstStart(false)
            replaceFragmentNoAddToBackStack(OnBoardLockSetFragment(), R.id.rootOnboard)
            hideViewpager()
        } else {
            if (isFromSettings) {
                replaceFragmentNoAddToBackStack(
                    OnBoardLockFragment.newInstance(isFromSettings),
                    R.id.rootOnboard
                )
                hideViewpager()
            }
        }
        initUwaziEvents()
        initReportsEvents()
    }

    private fun initButtons() {
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        binding.nextBtn.setOnClickListener {
            onNextPressed()

        }
    }

    override fun initProgress(itemCount: Int) {
        setupIndicators(itemCount)
    }

    private fun setupIndicators(indicatorCount: Int) {
        binding.indicatorsContainer.removeAllViews()
        val indicators = arrayOfNulls<ImageView>(indicatorCount)
        val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(12, 0, 12, 0)
        for (i in indicators.indices) {
            indicators[i] = ImageView(applicationContext)
            indicators[i].apply {
                this?.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext, R.drawable.onboarding_indicator_inactive
                    )
                )
                this?.layoutParams = layoutParams
            }
            binding.indicatorsContainer.addView(indicators[i])
        }
    }

    private fun initUwaziEvents() {
        createServer.observe(
            this
        ) { server: UWaziUploadServer? ->
            if (server != null) {
                presenter.create(server)
                addFragment(OnBoardHideOptionFragment(), R.id.rootOnboard)
            }
        }
    }

    private fun initReportsEvents() {
        createReportsServer.observe(this) { server ->
            if (server != null) {
                presenter.create(server)
                addFragment(OnBoardHideOptionFragment(), R.id.rootOnboard)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.viewPager.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed()
        } else {
            // Otherwise, select the previous step.
            if (binding.viewPager.size > 0)
                binding.viewPager.currentItem -= 1
        }

    }

    fun onNextPressed() {
        if (binding.viewPager.currentItem != viewpagerItemsCount) {
            // select the Next step in the viewpager
            binding.viewPager.currentItem += 1
        }

    }

    override fun setCurrentIndicator(index: Int) {
        val childCount = binding.indicatorsContainer.childCount
        for (i in 0 until childCount) {
            val imageView = binding.indicatorsContainer[i] as ImageView
            if (i == index) {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext, R.drawable.onboarding_indicator_active
                    )
                )
            } else {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext, R.drawable.onboarding_indicator_inactive
                    )
                )
            }
        }
    }

    override fun showChooseServerTypeDialog() {
        /*
        showBinaryTypeSheet(this.supportFragmentManager, context = this,
            getString(R.string.settings_servers_add_server_dialog_title),
            getString(R.string.settings_add_server_selection_dialog_title),
            getString(R.string.settings_serv_add_server_selection_dialog_description),
            getString(R.string.action_cancel),
            getString(R.string.action_ok),
            getString(R.string.settings_docu_add_server_dialog_select_odk),
            getString(R.string.settings_docu_add_server_dialog_select_tella_web),
            getString(R.string.settings_docu_add_server_dialog_select_tella_uwazi),
            getString(R.string.settings_docu_add_server_dialog_select_tella_google_drive),
            getString(R.string.settings_docu_add_server_dialog_select_tella_dropbox),
            getString(R.string.unavailable_connections),
            getString(R.string.unavailable_connections_desc),
            false,
            false,
            getString(R.string.settings_docu_add_server_dialog_select_next_cloud),
            object : IServerChoiceActions {
                override fun addUwaziServer() {
                    showUwaziServerDialog()
                }

                override fun addGoogleDriveServer() {
                }

                override fun addDropBoxServer() {
                }

                override fun addNextCloudServer() {
                    TODO("Not yet implemented")
                }

                override fun addTellaWebServer() {
                    //   showTellaUploadServerDialog()
                }

                override fun addODKServer() {
                    showCollectServerDialog()
                }
            })*/

    }

    private fun showUwaziServerDialog() {
        startActivity(Intent(this, UwaziConnectFlowActivity::class.java))
    }

    override fun hideProgress() {
        binding.indicatorsContainer.visibility = View.INVISIBLE

    }

    override fun showProgress() {
        binding.indicatorsContainer.visibility = View.VISIBLE

    }

    override fun initViewPager(itemCount: Int) {
        viewpagerItemsCount = itemCount
        // The pager adapter, which provides the pages to the view pager widget.
        val pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager, this.lifecycle)
        binding.viewPager.adapter = pagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ ->
        }.attach()

        binding.viewPager.visibility = View.VISIBLE
    }

    override fun showLoading() {
    }

    override fun hideLoading() {
    }

    override fun onCreatedTUServer(server: TellaReportServer?) {
        addFragment(OnBoardConnectedFragment(), R.id.rootOnboard)
    }

    override fun onCreateTUServerError(throwable: Throwable?) {
        DialogUtils.showBottomMessage(
            this, getString(R.string.settings_docu_toast_fail_create_server), true
        )
    }

    override fun onCreateCollectServerError(throwable: Throwable?) {
        DialogUtils.showBottomMessage(
            this, getString(R.string.settings_docu_toast_fail_create_server), true
        )
    }

    override fun onCreatedServer(server: CollectServer?) {
        addFragment(OnBoardConnectedFragment(), R.id.rootOnboard)
    }

    override fun onCreatedUwaziServer(server: UWaziUploadServer?) {
        addFragment(OnBoardHideOptionFragment(), R.id.rootOnboard)
    }

    override fun onTellaUploadServerDialogCreate(server: TellaReportServer?) {
        presenter.create(server)
    }

    override fun onTellaUploadServerDialogUpdate(server: TellaReportServer?) {
    }


    override fun onDialogDismiss() {

    }

    private fun showCollectServerDialog() {
        CollectServerDialogFragment.newInstance(null)
            .show(supportFragmentManager, CollectServerDialogFragment.TAG)
    }

    private fun showTellaUploadServerDialog() {
        startActivity(Intent(this, ReportsConnectFlowActivity::class.java))
    }

    //TODO WHY THIS HARCODED?
    override fun enterCustomizationCode() {
        BottomSheetUtils.showEnterCustomizationCodeSheet(this.supportFragmentManager,
            "Customization",
            "Enter your customization code",
            "Your organization may provide a code for you to set up Tella according to their settings.",
            getString(R.string.action_next),
            object : BottomSheetUtils.StringConsumer {
                override fun accept(code: String) {
                    handleCustomizationCode(code)
                }
            })
    }

    override fun enableSwipe(isSwipeable: Boolean, isTabLayoutVisible: Boolean) {
        binding.viewPager.isUserInputEnabled = isSwipeable
        binding.tabLayout.isVisible = isTabLayoutVisible
    }

    override fun showButtons(isNextButtonVisible: Boolean, isBackButtonVisible: Boolean) {
        binding.nextBtn.isVisible = isNextButtonVisible
        binding.backBtn.isVisible = isBackButtonVisible
    }

    override fun hideViewpager() {
        binding.viewPager.visibility = View.GONE
    }

    private fun handleCustomizationCode(code: String) {
        DialogUtils.showBottomMessage(
            this,
            code,
            false
        )
    }


    private inner class ScreenSlidePagerAdapter(fm: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fm, lifecycle) {

        override fun getItemCount(): Int = viewpagerItemsCount
        override fun createFragment(position: Int): Fragment {
            val fragment: Fragment = when (position) {
                ONBOARDING_INTRODUCTION_VIEW_INDEX -> OnBoardIntroFragment()
                ONBOARDING_CAMERA_VIEW_INDEX -> OnBoardCameraFragment()
                ONBOARDING_RECORDER_VIEW_INDEX -> OnBoardRecorderFragment()
                ONBOARDING_FILES_VIEW_INDEX -> OnBoardFilesFragment()
                ONBOARDING_COLLECT_DATA_VIEW -> OnboardCollectDataFragment()
                ONBOARDING_LOCK_VIEW_INDEX -> OnBoardLockFragment()
                else -> OnBoardIntroFragment()
            }
            return fragment
        }

    }


}