package rs.readahead.washington.mobile.views.activity.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.IS_ONBOARD_LOCK_SET
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.IServerChoiceActions
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showBinaryTypeSheet
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseActivity
import rs.readahead.washington.mobile.views.dialog.CollectServerDialogFragment
import rs.readahead.washington.mobile.views.dialog.CollectServerDialogFragment.CollectServerDialogHandler
import rs.readahead.washington.mobile.views.dialog.SharedLiveData.createReportsServer
import rs.readahead.washington.mobile.views.dialog.SharedLiveData.createServer
import rs.readahead.washington.mobile.views.dialog.TellaUploadServerDialogFragment.TellaUploadServerDialogHandler
import rs.readahead.washington.mobile.views.dialog.reports.ReportsConnectFlowActivity
import rs.readahead.washington.mobile.views.dialog.uwazi.UwaziConnectFlowActivity

class OnBoardingActivity : BaseActivity(), OnBoardActivityInterface,
    IOnBoardPresenterContract.IView, CollectServerDialogHandler,
    TellaUploadServerDialogHandler {

    private val isFromSettings by lazy { intent.getBooleanExtra(IS_FROM_SETTINGS, false) }
    private val isOnboardLockSet by lazy { intent.getBooleanExtra(IS_ONBOARD_LOCK_SET, false) }
    private val presenter by lazy { OnBoardPresenter(this) }
    private lateinit var indicatorsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(
            com.hzontal.tella_locking_ui.R.anim.`in`,
            com.hzontal.tella_locking_ui.R.anim.out
        )

        setContentView(R.layout.activity_onboarding)

        indicatorsContainer = findViewById(R.id.indicatorsContainer)

        if (isOnboardLockSet) {
            Preferences.setFirstStart(false)
            replaceFragmentNoAddToBackStack(OnBoardLockSetFragment(), R.id.rootOnboard)
        } else {
            replaceFragmentNoAddToBackStack(
                if (!isFromSettings) OnBoardIntroFragment() else OnBoardLockFragment.newInstance(
                    true
                ), R.id.rootOnboard
            )
        }
        initUwaziEvents()
        initReportsEvents()
    }

    private fun setupIndicators(indicatorCount: Int) {
        indicatorsContainer.removeAllViews()
        val indicators = arrayOfNulls<ImageView>(indicatorCount)
        val layoutParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        layoutParams.setMargins(12, 0, 12, 0)
        for (i in indicators.indices) {
            indicators[i] = ImageView(applicationContext)
            indicators[i].apply {
                this?.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.onboarding_indicator_inactive
                    )
                )
                this?.layoutParams = layoutParams
            }
            indicatorsContainer.addView(indicators[i])
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

    private fun initReportsEvents(){
        createReportsServer.observe(this){ server ->
            if (server != null) {
                presenter.create(server)
                addFragment(OnBoardHideOptionFragment(), R.id.rootOnboard)
            }
        }
    }

    override fun setCurrentIndicator(index: Int) {
        val childCount = indicatorsContainer.childCount
        for (i in 0 until childCount) {
            val imageView = indicatorsContainer[i] as ImageView
            if (i == index) {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.onboarding_indicator_active
                    )
                )
            } else {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.onboarding_indicator_inactive
                    )
                )
            }
        }
    }

    override fun showChooseServerTypeDialog() {
        showBinaryTypeSheet(this.supportFragmentManager,
            getString(R.string.settings_servers_add_server_dialog_title),
            getString(R.string.settings_serv_add_server_selection_dialog_title),
            getString(R.string.settings_serv_add_server_selection_dialog_description),
            getString(R.string.action_cancel),  //TODO CHECk THIS
            getString(R.string.action_ok),  //TODO CHECk THIS
            getString(R.string.settings_docu_add_server_dialog_select_odk),
            getString(R.string.settings_docu_add_server_dialog_select_tella_web),
            getString(R.string.settings_docu_add_server_dialog_select_tella_uwazi),
            object : IServerChoiceActions {
                override fun addUwaziServer() {
                    showUwaziServerDialog()
                }

                override fun addTellaWebServer() {
                    showTellaUploadServerDialog()
                }

                override fun addODKServer() {
                    showCollectServerDialog()
                }
            }
        )
        /* showDualChoiceTypeSheet(this.supportFragmentManager,
             getString(R.string.settings_servers_add_server_dialog_title),
             getString(R.string.settings_serv_add_server_selection_dialog_title),
             getString(R.string.settings_servers_add_server_forms),
             getString(R.string.settings_servers_add_server_reports),
             object : DualChoiceConsumer {
                 override fun accept(option: Boolean) {
                     if (option) {
                         showCollectServerDialog()
                     } else {
                         showTellaUploadServerDialog()
                     }
                 }
             })*/
    }

    private fun showUwaziServerDialog() {
        startActivity(Intent(this, UwaziConnectFlowActivity::class.java))

    }

    override fun hideProgress() {
        indicatorsContainer.visibility = View.INVISIBLE
    }

    override fun showProgress() {
        indicatorsContainer.visibility = View.VISIBLE
    }

    override fun initProgress(itemCount: Int) {
        setupIndicators(itemCount)
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
            this,
            getString(R.string.settings_docu_toast_fail_create_server),
            true
        )
    }

    override fun onCreateCollectServerError(throwable: Throwable?) {
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.settings_docu_toast_fail_create_server),
            true
        )
    }

    override fun onCreatedServer(server: CollectServer?) {
        addFragment(OnBoardConnectedFragment(), R.id.rootOnboard)
    }

    override fun onCreatedUwaziServer(server: UWaziUploadServer?) {
        addFragment(OnBoardHideOptionFragment(), R.id.rootOnboard)
    }

    override fun onCollectServerDialogCreate(server: CollectServer?) {
        presenter.create(server)
    }

    override fun onCollectServerDialogUpdate(server: CollectServer?) {
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
            }
        )
    }

    private fun handleCustomizationCode(code: String) {
        showToast(code)
    }
}