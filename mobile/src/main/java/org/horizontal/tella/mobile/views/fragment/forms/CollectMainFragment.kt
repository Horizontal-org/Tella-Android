package org.horizontal.tella.mobile.views.fragment.forms

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.utils.DialogUtils
import org.javarosa.core.model.FormDef
import permissions.dispatcher.OnShowRationale
import permissions.dispatcher.PermissionRequest
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.EventObserver
import org.horizontal.tella.mobile.bus.event.*
import org.horizontal.tella.mobile.databinding.FragmentCollectMainBinding
import org.horizontal.tella.mobile.domain.entity.collect.CollectForm
import org.horizontal.tella.mobile.domain.entity.collect.CollectFormInstance
import org.horizontal.tella.mobile.javarosa.FormUtils
import org.horizontal.tella.mobile.util.PermissionUtil
import org.horizontal.tella.mobile.util.StringUtils
import org.horizontal.tella.mobile.views.activity.FormSubmitActivity
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.forms.viewpager.BLANK_LIST_PAGE_INDEX
import org.horizontal.tella.mobile.views.fragment.uwazi.SharedLiveData
import org.horizontal.tella.mobile.views.fragment.uwazi.viewpager.DRAFT_LIST_PAGE_INDEX
import org.horizontal.tella.mobile.views.fragment.uwazi.viewpager.OUTBOX_LIST_PAGE_INDEX
import org.horizontal.tella.mobile.views.fragment.uwazi.viewpager.SUBMITTED_LIST_PAGE_INDEX
import timber.log.Timber

const val LOCATION_REQUEST_CODE = 1003

@AndroidEntryPoint
class CollectMainFragment :
    BaseBindingFragment<FragmentCollectMainBinding>(FragmentCollectMainBinding::inflate) {
    private val disposables by lazy { MyApplication.bus().createCompositeDisposable() }
    private var alertDialog: AlertDialog? = null
    private val model: SharedFormsViewModel by viewModels()

    companion object {
        // Use this function to create instance of current fragment
        @JvmStatic
        fun newInstance(): CollectMainFragment {
            val args = Bundle()
            val fragment = CollectMainFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()

        baseActivity.setSupportActionBar(binding.toolbar)

        val actionBar: ActionBar? = baseActivity.supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false)
            actionBar.setTitle(R.string.settings_servers_add_server_forms)
        }

        initObservers()

        binding.blankFormsText.text =
            Html.fromHtml(getString(R.string.collect_expl_not_connected_to_server))
        binding.blankFormsText.movementMethod = LinkMovementMethod.getInstance()
        StringUtils.stripUnderlines(binding.blankFormsText)

        disposables.wire(
            ReSubmitFormInstanceEvent::class.java,
            object : EventObserver<ReSubmitFormInstanceEvent?>() {
                override fun onNext(event: ReSubmitFormInstanceEvent) {
                    reSubmitFormInstance(event.instance)
                }
            })

        binding.toolbar.backClickListener = { nav().popBackStack() }
    }

    private fun initView() {
        val viewPagerAdapter =
            org.horizontal.tella.mobile.views.fragment.forms.viewpager.ViewPagerAdapter(this)
        with(binding) {
            viewPager.apply {
                offscreenPageLimit = 4
                isSaveEnabled = false
                adapter = viewPagerAdapter
            }
            // Set the text for each tab
            TabLayoutMediator(tabs, viewPager) { tab, position ->
                tab.text = getTabTitle(position)

            }.attach()

            tabs.setTabTextColors(
                ContextCompat.getColor(baseActivity, R.color.wa_white_50),
                ContextCompat.getColor(baseActivity, R.color.wa_white)
            )

        }

        SharedLiveData.updateViewPagerPosition.observe(baseActivity) { position ->
            when (position) {
                BLANK_LIST_PAGE_INDEX -> setCurrentTab(BLANK_LIST_PAGE_INDEX)
                DRAFT_LIST_PAGE_INDEX -> setCurrentTab(DRAFT_LIST_PAGE_INDEX)
                OUTBOX_LIST_PAGE_INDEX -> setCurrentTab(OUTBOX_LIST_PAGE_INDEX)
                SUBMITTED_LIST_PAGE_INDEX -> setCurrentTab(SUBMITTED_LIST_PAGE_INDEX)
            }
        }
    }

    private fun setCurrentTab(position: Int) {
        if (isViewInitialized) {
            binding.viewPager.post {
                binding.viewPager.setCurrentItem(position, true)
            }
        }
    }

    private fun getTabTitle(position: Int): String? {
        return when (position) {
            BLANK_LIST_PAGE_INDEX -> getString(R.string.collect_tab_title_blank)
            DRAFT_LIST_PAGE_INDEX -> getString(R.string.collect_draft_tab_title)
            OUTBOX_LIST_PAGE_INDEX -> getString(R.string.collect_outbox_tab_title)
            SUBMITTED_LIST_PAGE_INDEX -> getString(R.string.collect_sent_tab_title)
            else -> null
        }
    }

    override fun onResume() {
        super.onResume()
        countServers()
    }

    override fun onStop() {
        if (alertDialog != null && alertDialog!!.isShowing) {
            alertDialog!!.dismiss()
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (disposables != null) {
            disposables.dispose()
        }
        super.onDestroy()
    }
    /*
        @Deprecated("Deprecated in Java")
        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                //onBackPressed()
                return true
            }
            return super.onOptionsItemSelected(item)
        }

        private fun hasLocationPermissions(context: Context): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
            return true
        return false
        }

        private fun requestLocationPermissions() {
        baseActivity.maybeChangeTemporaryTimeout()
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        ActivityCompat.requestPermissions(
            //1
            baseActivity,
            //2
            permissions,
            //3
            LOCATION_REQUEST_CODE
        )
        }*/

    private fun initObservers() {
        model.onError.observe(viewLifecycleOwner) { error ->
            Timber.d(error, javaClass.name)
        }

        model.onFormDefError.observe(viewLifecycleOwner) { error ->
            val errorMessage = FormUtils.getFormDefErrorMessage(baseActivity, error)
            DialogUtils.showBottomMessage(
                baseActivity,
                errorMessage,
                true
            )
        }

        model.onToggleFavoriteSuccess.observe(viewLifecycleOwner) {
            setCurrentTab(BLANK_LIST_PAGE_INDEX)
        }

        /* model.showFab.observe(viewLifecycleOwner, { show ->
             binding.fab.isVisible = show
         })*/
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    fun showFineLocationRationale(request: PermissionRequest) {
        baseActivity.maybeChangeTemporaryTimeout()
        alertDialog = PermissionUtil.showRationale(
            baseActivity,
            request,
            getString(R.string.permission_dialog_expl_GPS)
        )
    }

    private fun showCancelPendingFormDialog(instanceId: Long) {
        alertDialog = AlertDialog.Builder(baseActivity)
            .setMessage(R.string.collect_sent_dialog_expl_discard_unsent_form)
            .setPositiveButton(R.string.action_discard) { _, _ ->
                model.deleteFormInstance(
                    instanceId
                )
            }
            .setNegativeButton(R.string.action_cancel) { dialog, which -> }
            .setCancelable(true)
            .show()
    }

    private fun reSubmitFormInstance(instance: CollectFormInstance) {
        startActivity(
            Intent(baseActivity, FormSubmitActivity::class.java)
                .putExtra(FormSubmitActivity.FORM_INSTANCE_ID_KEY, instance.id)
        )
    }

    private fun countServers() {
        model.countCollectServers()
    }

    private fun startCreateFormControllerPresenter(form: CollectForm, formDef: FormDef) {
        model.createFormController(form, formDef)
    }

    private fun showStoppedMessage() {
        DialogUtils.showBottomMessage(
            baseActivity,
            getString(R.string.Collect_DialogInfo_FormSubmissionStopped),
            true
        )
    }

}