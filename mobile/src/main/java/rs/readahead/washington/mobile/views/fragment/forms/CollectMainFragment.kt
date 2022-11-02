package rs.readahead.washington.mobile.views.fragment.forms

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.android.material.tabs.TabLayout
import org.hzontal.shared_ui.utils.DialogUtils
import org.javarosa.core.model.FormDef
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnShowRationale
import permissions.dispatcher.PermissionRequest
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.EventObserver
import rs.readahead.washington.mobile.bus.event.*
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.FragmentCollectMainBinding
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance
import rs.readahead.washington.mobile.javarosa.FormUtils
import rs.readahead.washington.mobile.util.PermissionUtil
import rs.readahead.washington.mobile.util.StringUtils
import rs.readahead.washington.mobile.views.activity.CollectFormEntryActivity
import rs.readahead.washington.mobile.views.activity.CollectHelpActivity
import rs.readahead.washington.mobile.views.activity.FormSubmitActivity
import rs.readahead.washington.mobile.views.adapters.ViewPagerAdapter
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import timber.log.Timber

const val LOCATION_REQUEST_CODE = 1003

class CollectMainFragment :
    BaseBindingFragment<FragmentCollectMainBinding>(FragmentCollectMainBinding::inflate) {
    private var blankFragmentPosition = 0
    private val disposables by lazy { MyApplication.bus().createCompositeDisposable() }
    private var alertDialog: AlertDialog? = null
    private var mViewPager: ViewPager? = null
    private val adapter by lazy { ViewPagerAdapter(baseActivity.supportFragmentManager) }
    private var numOfCollectServers: Long = 0
    private val model: SharedFormsViewModel by activityViewModels()

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

        baseActivity.setSupportActionBar(binding?.toolbar)

        val actionBar: ActionBar? = baseActivity.supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false)
            actionBar.setTitle(R.string.settings_servers_add_server_forms)
        }

        if (!hasInitializedRootView) {
            hasInitializedRootView = true
        }

        initObservers()
        initViewPageAdapter()

        mViewPager = binding?.formsViewPager
        mViewPager?.adapter = adapter
        mViewPager?.currentItem = blankFragmentPosition

        val tabLayout: TabLayout? = binding?.tabs
        if (tabLayout != null) {
            tabLayout.setupWithViewPager(mViewPager)
        }

        binding?.fab?.setOnClickListener {
            if (MyApplication.isConnectedToInternet(context)) {
                if (mViewPager?.currentItem == blankFragmentPosition) {
                    getBlankFormsListFragment().refreshBlankForms()
                }
            } else {
                baseActivity.showToast(getString(R.string.collect_blank_toast_not_connected))
            }
        }

        mViewPager?.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                binding?.fab?.visibility =
                    if (position == blankFragmentPosition && numOfCollectServers > 0) View.VISIBLE else View.GONE
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        binding?.blankFormsText?.text =
            Html.fromHtml(getString(R.string.collect_expl_not_connected_to_server))
        binding?.blankFormsText?.movementMethod = LinkMovementMethod.getInstance()
        StringUtils.stripUnderlines(binding?.blankFormsText)

        disposables.wire(
            ShowBlankFormEntryEvent::class.java,
            object : EventObserver<ShowBlankFormEntryEvent?>() {
                override fun onNext(event: ShowBlankFormEntryEvent) {
                    // this should be called on observed onGetBlankFormDefSuccess ?
                    startCreateFormControllerPresenter(event.form.form, event.form.formDef)
                }
            })
        disposables.wire(
            ShowFormInstanceEntryEvent::class.java,
            object : EventObserver<ShowFormInstanceEntryEvent?>() {
                override fun onNext(event: ShowFormInstanceEntryEvent) {
                    showFormInstanceEntry(event.instanceId)
                }
            })
        disposables.wire(
            CollectFormSubmittedEvent::class.java,
            object : EventObserver<CollectFormSubmittedEvent?>() {
                override fun onNext(event: CollectFormSubmittedEvent) {
                    getDraftFormsListFragment().listDraftForms()
                    getSubmittedFormsListFragment().listSubmittedForms()
                    setPagerToSubmittedFragment()
                }
            })
        disposables.wire(
            CollectFormSubmitStoppedEvent::class.java,
            object : EventObserver<CollectFormSubmitStoppedEvent?>() {
                override fun onNext(event: CollectFormSubmitStoppedEvent) {
                    getDraftFormsListFragment().listDraftForms()
                    getOutboxFormListFragment().listOutboxForms()
                    showStoppedMessage()
                    setPagerToOutboxFragment()
                }
            })
        disposables.wire(
            CollectFormSubmissionErrorEvent::class.java,
            object : EventObserver<CollectFormSubmissionErrorEvent?>() {
                override fun onNext(event: CollectFormSubmissionErrorEvent) {
                    getDraftFormsListFragment().listDraftForms()
                    getOutboxFormListFragment().listOutboxForms()
                    setPagerToOutboxFragment()
                }
            })
        disposables.wire(
            CollectFormSavedEvent::class.java,
            object : EventObserver<CollectFormSavedEvent?>() {
                override fun onNext(event: CollectFormSavedEvent) {
                    getDraftFormsListFragment().listDraftForms()
                }
            })
        disposables.wire(
            CollectFormInstanceDeletedEvent::class.java,
            object : EventObserver<CollectFormInstanceDeletedEvent?>() {
                override fun onNext(event: CollectFormInstanceDeletedEvent) {
                    //onFormInstanceDeleteSuccess()
                }
            })
        /*disposables.wire(
             DeleteFormInstanceEvent::class.java,
             object : EventObserver<DeleteFormInstanceEvent?>() {
                 override fun onNext(event: DeleteFormInstanceEvent) {
                     showDeleteInstanceDialog(event.instanceId, event.status)
                 }
             })*/
        disposables.wire(
            CancelPendingFormInstanceEvent::class.java,
            object : EventObserver<CancelPendingFormInstanceEvent?>() {
                override fun onNext(event: CancelPendingFormInstanceEvent) {
                    showCancelPendingFormDialog(event.instanceId)
                }
            })
        disposables.wire(
            ReSubmitFormInstanceEvent::class.java,
            object : EventObserver<ReSubmitFormInstanceEvent?>() {
                override fun onNext(event: ReSubmitFormInstanceEvent) {
                    reSubmitFormInstance(event.instance)
                }
            })
    }

    override fun onStart() {
        super.onStart()
        Handler(Looper.getMainLooper()).post {
            adapter.notifyDataSetChanged()
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            //onBackPressed()
            return true
        }
        if (id == R.id.help_item) {
            startCollectHelp()
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

    }

    private fun initObservers() {
        model.onError.observe(viewLifecycleOwner, Observer { error ->
            Timber.d(error, javaClass.name)
        })
        model.onGetBlankFormDefSuccess.observe(viewLifecycleOwner, Observer { result ->
            result.let {
                startCreateFormControllerPresenter(it.form, it.formDef)
            }
        })
        model.onInstanceFormDefSuccess.observe(viewLifecycleOwner, Observer { instance ->
            startCreateInstanceFormControllerPresenter(instance)
        })

        model.onFormDefError.observe(viewLifecycleOwner, Observer { error ->
            val errorMessage = FormUtils.getFormDefErrorMessage(baseActivity, error)
            baseActivity.showToast(errorMessage)
        })

        model.onFormDefError.observe(viewLifecycleOwner, Observer { error ->
            val errorMessage = FormUtils.getFormDefErrorMessage(baseActivity, error)
            baseActivity.showToast(errorMessage)
        })

        model.onCreateFormController.observe(viewLifecycleOwner, { form ->
            form?.let {
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
        })
        model.onToggleFavoriteSuccess.observe(viewLifecycleOwner, {
            getBlankFormsListFragment().listBlankForms()
        })

        /*model.onFormInstanceDeleteSuccess.observe(viewLifecycleOwner, Observer {
            Toast.makeText(activity, R.string.collect_toast_form_deleted, Toast.LENGTH_SHORT).show()
            getSubmittedFormsListFragment().listSubmittedForms()
            getDraftFormsListFragment().listDraftForms()
        })*/

        model.onCountCollectServersEnded.observe(viewLifecycleOwner, Observer { num ->
            numOfCollectServers = num
            if (numOfCollectServers < 1) {
                binding?.tabs?.visibility = View.GONE
                binding?.formsViewPager?.visibility = View.GONE
                binding?.fab?.visibility = View.GONE
                binding?.noServersView?.visibility = View.VISIBLE
            } else {
                binding?.tabs?.visibility = View.VISIBLE
                binding?.formsViewPager?.visibility = View.VISIBLE
                binding?.noServersView?.visibility = View.GONE
                if (mViewPager?.currentItem == blankFragmentPosition) {
                    binding?.fab?.visibility = View.VISIBLE
                }
            }
        })

        model.showFab.observe(viewLifecycleOwner, Observer { show ->
            binding?.fab?.isVisible = show
        })
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startCollectFormEntryActivity() {
        startActivity(Intent(activity, CollectFormEntryActivity::class.java))
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

    private fun showFormInstanceEntry(instanceId: Long) {
        startGetInstanceFormDefPresenter(instanceId)
    }

    /*private fun showDeleteInstanceDialog(instanceId: Long, status: CollectFormInstanceStatus) {
        if (status == CollectFormInstanceStatus.DRAFT) {
            this.model.deleteFormInstance(instanceId)
        } else {

            val msgResId = R.string.collect_dialog_text_delete_sent_form

            showStandardSheet(
                activity.getSupportFragmentManager(),
                getString(R.string.Collect_RemoveForm_SheetTitle),
                getString(msgResId),
                getString(R.string.action_remove),
                getString(R.string.action_cancel),
                { this.model.deleteFormInstance(instanceId) },
                { })
        }
    }*/

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

    private fun startGetInstanceFormDefPresenter(instanceId: Long) {
        model.getInstanceFormDef(instanceId)
    }

    private fun startCreateFormControllerPresenter(form: CollectForm, formDef: FormDef) {
        model.createFormController(form, formDef)
    }

    private fun startCreateInstanceFormControllerPresenter(instance: CollectFormInstance) {
        model.createFormController(instance)
    }

    private fun initViewPageAdapter() {
        adapter.addFragment(
            BlankFormsListFragment.newInstance(),
            getString(R.string.collect_tab_title_blank)
        )
        adapter.addFragment(
            DraftFormsListFragment.newInstance(),
            getString(R.string.collect_draft_tab_title)
        )
        adapter.addFragment(
            OutboxFormListFragment.newInstance(),
            getString(R.string.collect_outbox_tab_title)
        )
        adapter.addFragment(
            SubmittedFormsListFragment.newInstance(),
            getString(R.string.collect_sent_tab_title)
        )
        blankFragmentPosition = getFragmentPosition(FormListInterfce.Type.BLANK)
    }

    private fun getDraftFormsListFragment(): DraftFormsListFragment {
        return getFormListFragment(FormListInterfce.Type.DRAFT)
    }

    private fun getBlankFormsListFragment(): BlankFormsListFragment {
        return getFormListFragment(FormListInterfce.Type.BLANK)
    }

    private fun getSubmittedFormsListFragment(): SubmittedFormsListFragment {
        return getFormListFragment(FormListInterfce.Type.SUBMITTED)
    }

    private fun getOutboxFormListFragment(): OutboxFormListFragment {
        return getFormListFragment(FormListInterfce.Type.OUTBOX)
    }

    private fun <T> getFormListFragment(type: FormListInterfce.Type): T {
        for (i in 0 until adapter.count) {
            val fragment = adapter.getItem(i) as FormListInterfce
            if (fragment.formListType == type) {
                return fragment as T
            }
        }
        throw IllegalArgumentException()
    }

    private fun getFragmentPosition(type: FormListInterfce.Type): Int {
        for (i in 0 until adapter.count) {
            val fragment = adapter.getItem(i) as FormListInterfce
            if (fragment.formListType == type) {
                return i
            }
        }
        throw IllegalArgumentException()
    }

    private fun setPagerToSubmittedFragment() {
        mViewPager?.currentItem = getFragmentPosition(FormListInterfce.Type.SUBMITTED)
        binding?.fab?.visibility = View.GONE
    }

    private fun setPagerToOutboxFragment() {
        mViewPager?.currentItem = getFragmentPosition(FormListInterfce.Type.OUTBOX)
        binding?.fab?.visibility = View.GONE
    }

    private fun startCollectHelp() {
        startActivity(Intent(baseActivity, CollectHelpActivity::class.java))
    }

    private fun showStoppedMessage() {
        DialogUtils.showBottomMessage(
            baseActivity,
            getString(R.string.Collect_DialogInfo_FormSubmissionStopped),
            true
        )
    }

}