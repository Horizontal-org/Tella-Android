package org.horizontal.tella.mobile.views.activity.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
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
import org.horizontal.tella.mobile.views.dialog.TellaUploadServerDialogHandler
import org.horizontal.tella.mobile.views.dialog.reports.ReportsConnectFlowActivity
import org.horizontal.tella.mobile.views.dialog.uwazi.UwaziConnectFlowActivity
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils

private const val ONBOARDING_INTRODUCTION_VIEW_INDEX = 0
private const val ONBOARDING_RECORD_VIEW_INDEX = 1
private const val ONBOARDING_FILES_VIEW_INDEX = 2
private const val ONBOARDING_COLLECT_DATA_VIEW = 3
private const val ONBOARDING_NEARBY_SHARING_VIEW_INDEX = 4
private const val ONBOARDING_LOCK_VIEW_INDEX = 5
private const val ONBOARDING_PROGRESS_DOT_COUNT = 5


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
        applyOnboardingInsets()
        // Instantiate a ViewPager and a Tablayout
        if (!isOnboardLockSet && !isFromSettings) initViewPager(6)

        // Instantiate next and back buttons
        initButtons()

        if (isOnboardLockSet) {
            Preferences.setFirstStart(false)
            replaceFragmentNoAddToBackStack(OnBoardLockSuccessFragment(), R.id.rootOnboard)
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

    private fun applyOnboardingInsets() {
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        val navHorizontal =
            resources.getDimensionPixelSize(R.dimen.onboarding_nav_horizontal_margin)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets -> insets }

        ViewCompat.setOnApplyWindowInsetsListener(binding.viewPager) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.onboardBottomBar) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                bars.left + navHorizontal,
                view.paddingTop,
                bars.right + navHorizontal,
                bars.bottom
            )
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootOnboard) { _, insets -> insets }

        ViewCompat.requestApplyInsets(binding.root)
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
        val dotSize = resources.getDimensionPixelSize(R.dimen.onboarding_indicator_dot_size)
        val spacing = resources.getDimensionPixelSize(R.dimen.onboarding_indicator_spacing)
        for (i in 0 until indicatorCount) {
            val indicator = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                    marginStart = if (i == 0) 0 else spacing
                    marginEnd = 0
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                setImageDrawable(
                    ContextCompat.getDrawable(this@OnBoardingActivity, R.drawable.default_dot)
                )
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            binding.indicatorsContainer.addView(indicator)
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

    override fun onResume() {
        super.onResume()
        // Returning from lock setup activity: ViewPager was hidden and warning fragment was popped, so show it again
        if (binding.viewPager.visibility == View.GONE && supportFragmentManager.backStackEntryCount == 0 && !isOnboardLockSet && !isFromSettings) {
            showViewpager()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.viewPager.currentItem == 0) {
            super.onBackPressed()
        } else if (binding.viewPager.size > 0) {
            binding.viewPager.setCurrentItem(binding.viewPager.currentItem - 1, true)
        }
    }

    fun onNextPressed() {
        if (binding.viewPager.currentItem < viewpagerItemsCount - 1) {
            binding.viewPager.setCurrentItem(binding.viewPager.currentItem + 1, true)
        }
    }

    override fun setCurrentIndicator(index: Int) {
        val childCount = binding.indicatorsContainer.childCount
        for (i in 0 until childCount) {
            val imageView = binding.indicatorsContainer[i] as ImageView
            if (i == index) {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext, R.drawable.selected_dot
                    )
                )
            } else {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext, R.drawable.default_dot
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
        val pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager, this.lifecycle)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1

        initProgress(ONBOARDING_PROGRESS_DOT_COUNT)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position in ONBOARDING_RECORD_VIEW_INDEX until ONBOARDING_LOCK_VIEW_INDEX) {
                    setCurrentIndicator(position - 1)
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    updateProgressForPage(binding.viewPager.currentItem)
                }
            }
        })
        updateProgressForPage(ONBOARDING_INTRODUCTION_VIEW_INDEX)

        binding.viewPager.visibility = View.VISIBLE
    }

    private fun updateProgressForPage(position: Int) {
        when (position) {
            ONBOARDING_INTRODUCTION_VIEW_INDEX -> {
                hideProgress()
                binding.viewPager.isUserInputEnabled = false
                showButtons(isNextButtonVisible = false, isBackButtonVisible = false)
            }
            ONBOARDING_LOCK_VIEW_INDEX -> {
                showProgress()
                setCurrentIndicator(position - 1)
                binding.viewPager.isUserInputEnabled = true
                showButtons(isNextButtonVisible = false, isBackButtonVisible = true)
            }
            else -> {
                showProgress()
                setCurrentIndicator(position - 1)
                binding.viewPager.isUserInputEnabled = true
                showButtons(isNextButtonVisible = true, isBackButtonVisible = true)
            }
        }
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
    }

    override fun showButtons(isNextButtonVisible: Boolean, isBackButtonVisible: Boolean) {
        binding.nextBtn.visibility = if (isNextButtonVisible) View.VISIBLE else View.INVISIBLE
        binding.backBtn.visibility = if (isBackButtonVisible) View.VISIBLE else View.INVISIBLE
        binding.nextBtn.isEnabled = isNextButtonVisible
        binding.backBtn.isEnabled = isBackButtonVisible
        binding.nextBtn.isClickable = isNextButtonVisible
        binding.backBtn.isClickable = isBackButtonVisible
    }

    override fun hideViewpager() {
        binding.viewPager.visibility = View.GONE
        binding.onboardBottomBar.visibility = View.GONE
    }

    override fun showViewpager() {
        binding.viewPager.visibility = View.VISIBLE
        binding.onboardBottomBar.visibility = View.VISIBLE
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
                ONBOARDING_RECORD_VIEW_INDEX -> OnBoardCameraFragment()
                ONBOARDING_FILES_VIEW_INDEX -> OnBoardFilesFragment()
                ONBOARDING_COLLECT_DATA_VIEW -> OnboardCollectDataFragment()
                ONBOARDING_NEARBY_SHARING_VIEW_INDEX -> OnBoardNearbySharingFragment()
                ONBOARDING_LOCK_VIEW_INDEX -> OnBoardLockFragment()
                else -> OnBoardIntroFragment()
            }
            return fragment
        }

    }


}