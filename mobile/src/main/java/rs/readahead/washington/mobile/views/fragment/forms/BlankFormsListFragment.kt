package rs.readahead.washington.mobile.views.fragment.forms

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionConfirmed
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionSeleceted
import org.hzontal.shared_ui.utils.DialogUtils
import org.javarosa.core.model.FormDef
import permissions.dispatcher.NeedsPermission
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.BlankCollectFormRowBinding
import rs.readahead.washington.mobile.databinding.FragmentBlankFormsListBinding
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult
import rs.readahead.washington.mobile.javarosa.FormUtils
import rs.readahead.washington.mobile.util.C
import rs.readahead.washington.mobile.util.DialogsUtil
import rs.readahead.washington.mobile.views.activity.CollectFormEntryActivity
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import timber.log.Timber

class BlankFormsListFragment :
    BaseBindingFragment<FragmentBlankFormsListBinding>(FragmentBlankFormsListBinding::inflate),
    FormListInterfce {

    private val model: SharedFormsViewModel by viewModels()

    private var availableForms: MutableList<CollectForm>? = null
    private var downloadedForms: MutableList<CollectForm>? = null
    private var alertDialog: AlertDialog? = null
    private var noUpdatedForms = 0
    private var silentFormUpdates = false
    override fun getFormListType(): FormListInterfce.Type {
        return FormListInterfce.Type.BLANK
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        availableForms = ArrayList()
        downloadedForms = ArrayList()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()

        /* if (!Preferences.isJavarosa3Upgraded()) {
             model.showFab.postValue(false)
             showJavarosa2UpgradeSheet()
         } else {*/
        listBlankForms()
        //}
    }

    override fun onDestroy() {
        hideAlertDialog()
        super.onDestroy()
    }

    private fun showBlankFormDownloadingDialog(progressText: Int) {
        if (alertDialog != null) return
        if (activity != null) {
            model.showFab.postValue(false)
        }
        alertDialog = DialogsUtil.showFormUpdatingDialog(
            context,
            { _: DialogInterface?, which: Int -> model.userCancel() }, progressText
        )
    }

    private fun initObservers() {
        model.onError.observe(viewLifecycleOwner, { error ->
            Timber.d(error, javaClass.name)
        })
        model.onGetBlankFormDefSuccess.observe(viewLifecycleOwner, { result ->
            result.let {
                startCreateFormControllerPresenter(it.form, it.formDef)
            }
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
        model.showBlankFormRefreshLoading.observe(
            viewLifecycleOwner
        ) { show: Boolean? ->
            if (!show!!) {
                Preferences.setLastCollectRefresh(
                    System.currentTimeMillis()
                )
                if (silentFormUpdates) {
                    silentFormUpdates = false
                }
                hideAlertDialog()
            } else {
                if (alertDialog != null) return@observe
                if (activity != null) {
                    model.showFab.postValue(false)
                }
                if (!silentFormUpdates) {
                    alertDialog = DialogsUtil.showCollectRefreshProgressDialog(
                        context
                    ) { dialog: DialogInterface?, which: Int -> model.userCancel() }
                }
            }
        }
        model.onDownloadBlankFormDefSuccess.observe(
            viewLifecycleOwner,
            { form: CollectForm? -> updateForm(form!!) })
        model.onDownloadBlankFormDefStart.observe(
            viewLifecycleOwner,
            { show: Boolean? ->
                if (show == true) {
                    showBlankFormDownloadingDialog(R.string.collect_dialog_text_download_progress)
                } else {
                    hideAlertDialog()
                    DialogUtils.showBottomMessage(
                        activity,
                        getString(R.string.collect_toast_download_completed),
                        false
                    )
                }
            })
        model.onUpdateBlankFormDefStart.observe(
            viewLifecycleOwner
        ) { show: Boolean ->
            if (show) {
                showBlankFormDownloadingDialog(R.string.collect_blank_dialog_expl_updating_form_definitions)
            } else {
                hideAlertDialog()
                DialogUtils.showBottomMessage(
                    activity,
                    getString(R.string.collect_blank_toast_form_definition_updated),
                    false
                )
            }
        }
        model.onBlankFormDefRemoved.observe(
            viewLifecycleOwner,
            { updateFormViews() })
        model.onUpdateBlankFormDefSuccess.observe(
            viewLifecycleOwner
        ) { (first, second): Pair<CollectForm?, FormDef?> ->
            onUpdateBlankFormDefSuccess(
                first,
                second
            )
        }
        model.onUserCancel.observe(viewLifecycleOwner) { cancel: Boolean? ->
            hideAlertDialog()
            DialogUtils.showBottomMessage(
                activity,
                getString(R.string.collect_blank_toast_refresh_canceled),
                false
            )
        }
        model.onFormDefError.observe(
            viewLifecycleOwner
        ) { error: Throwable? ->
            onFormDefError(
                error!!
            )
        }
        model.onFormCacheCleared.observe(viewLifecycleOwner) { cleared: Boolean? ->
            refreshBlankForms()
            model.showFab.postValue(true)
        }
        model.onBlankFormsListResult.observe(
            viewLifecycleOwner
        ) { listFormResult: ListFormResult? ->
            onBlankFormsListResult(
                listFormResult!!
            )
        }
        model.onNoConnectionAvailable.observe(
            viewLifecycleOwner
        ) { available: Boolean? ->
            if (!silentFormUpdates) {
                DialogUtils.showBottomMessage(
                    activity,
                    getString(R.string.collect_blank_toast_not_connected),
                    true
                )
            }
        }
    }

    private fun onUpdateBlankFormDefSuccess(collectForm: CollectForm?, formDef: FormDef?) {
        noUpdatedForms -= 1
        showBanner()
        updateDownloadedFormList()
    }

    private fun onFormDefError(error: Throwable) {
        val errorMessage = FormUtils.getFormDefErrorMessage(requireContext(), error)
        DialogUtils.showBottomMessage(
            activity,
            errorMessage,
            true
        )
    }

    private fun onBlankFormsListResult(listFormResult: ListFormResult) {
        updateFormLists(listFormResult)
        showBanner()
        updateFormViews()
        if (context != null && MyApplication.isConnectedToInternet(context) && checkIfDayHasPassed()) {
            silentFormUpdates = true
            refreshBlankForms()
        }
    }

    private fun updateFormLists(listFormResult: ListFormResult) {
        noUpdatedForms = 0
        binding.blankFormView.visibility = View.VISIBLE
        downloadedForms!!.clear()
        availableForms!!.clear()
        binding.blankFormsInfo.visibility =
            if (listFormResult.forms.isEmpty()) View.VISIBLE else View.GONE
        for (form in listFormResult.forms) {
            if (form.isDownloaded) {
                downloadedForms!!.add(form)
                if (form.isUpdated) {
                    noUpdatedForms += 1
                }
            } else {
                availableForms!!.add(form)
            }
        }
        // todo: make this multiply errors friendly
        if (!silentFormUpdates) {
            for (error in listFormResult.errors) {
                Toast.makeText(
                    activity,
                    String.format(
                        "%s %s",
                        getString(R.string.collect_blank_toast_fail_updating_form_list),
                        error.serverName
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                Timber.d(error.exception, javaClass.name)
            }
        }
    }

    private fun updateDownloadedFormList() {
        updateFormViews()
    }

    fun listBlankForms() {
        model.listBlankForms()
    }

    fun refreshBlankForms() {
        model.refreshBlankForms()
    }

    private fun updateForm(form: CollectForm) {
        availableForms!!.remove(form)
        downloadedForms!!.add(form)
        updateFormViews()
    }

    private fun setViewsVisibility() {
        binding.downloadedFormsTitle.visibility =
            if (downloadedForms!!.size > 0) View.VISIBLE else View.GONE
        binding.downloadedForms.visibility =
            if (downloadedForms!!.size > 0) View.VISIBLE else View.GONE
        binding.avaivableFormsTitle.visibility =
            if (availableForms!!.size > 0) View.VISIBLE else View.GONE
        binding.blankForms.visibility =
            if (availableForms!!.size > 0) View.VISIBLE else View.GONE
    }

    private fun updateFormViews() {
        binding.downloadedForms.removeAllViews()
        binding.blankForms.removeAllViews()
        createCollectFormViews(availableForms!!, binding.blankForms)
        createCollectFormViews(downloadedForms!!, binding.downloadedForms)
        setViewsVisibility()
    }

    private fun hideAlertDialog() {
        if (alertDialog != null) {
            alertDialog!!.dismiss()
            alertDialog = null
        }
        if (activity != null) {
            model.showFab.postValue(true)
        }
    }

    private fun createCollectFormViews(forms: List<CollectForm>, listView: LinearLayout) {
        for (form in forms) {
            val view = getCollectFormItem(form)
            listView.addView(view, forms.indexOf(form))
        }
    }

    private fun getCollectFormItem(collectForm: CollectForm?): View {
        val itemBinding = BlankCollectFormRowBinding.inflate(LayoutInflater.from(context), binding.forms, false)
        val row = itemBinding.formRow
        val name = itemBinding.name
        val organization = itemBinding.organization
        val dlOpenButton = itemBinding.dlOpenButton
        val pinnedIcon = itemBinding.favoritesButton
        val rowLayout = itemBinding.rowLayout
        val updateButton = itemBinding.laterButton
        if (collectForm != null) {
            name.text = collectForm.form.name
            organization.text = collectForm.serverName
            if (collectForm.isDownloaded) {
                dlOpenButton.setImageDrawable(ResourcesCompat.getDrawable(resources,R.drawable.ic_more,null))
                dlOpenButton.contentDescription =
                    getString(R.string.collect_blank_action_desc_more_options)
                dlOpenButton.setOnClickListener { view: View? ->
                    showDownloadedMenu(
                        collectForm
                    )
                }
                rowLayout.setOnClickListener { view: View? ->
                    model.getBlankFormDef(
                        collectForm
                    )
                }
                pinnedIcon.setOnClickListener { view: View? ->
                    model.toggleFavorite(collectForm)
                    updateFormViews()
                }
                if (collectForm.isUpdated) {
                    pinnedIcon.visibility = View.VISIBLE
                    updateButton.visibility = View.VISIBLE
                    updateButton.setOnClickListener { view: View? ->
                        if (MyApplication.isConnectedToInternet(requireContext())) {
                            model.updateBlankFormDef(collectForm)
                        } else {
                            DialogUtils.showBottomMessage(
                                activity,
                                getString(R.string.collect_blank_toast_not_connected),
                                true
                            )
                        }
                    }
                } else {
                    updateButton.visibility = View.GONE
                }
            } else {
                pinnedIcon.visibility = View.GONE
                dlOpenButton.setImageDrawable(ResourcesCompat.getDrawable(resources,R.drawable.ic_download,null))
                dlOpenButton.contentDescription =
                    getString(R.string.collect_blank_action_download_form)
                dlOpenButton.setOnClickListener { view: View? ->
                    if (MyApplication.isConnectedToInternet(requireContext())) {
                        model.downloadBlankFormDef(collectForm)
                    } else {
                        DialogUtils.showBottomMessage(
                            activity,
                            getString(R.string.collect_blank_toast_not_connected),
                            true
                        )
                    }
                }
            }
            if (collectForm.isPinned) {
                pinnedIcon.setImageDrawable(ResourcesCompat.getDrawable(resources,R.drawable.star_filled_24dp,null))
                pinnedIcon.contentDescription = getString(R.string.action_unfavorite)
            } else {
                pinnedIcon.setImageDrawable(ResourcesCompat.getDrawable(resources,R.drawable.star_border_24dp,null))
                pinnedIcon.contentDescription = getString(R.string.action_favorite)
            }
        }
        return itemBinding.root
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

    private fun startCreateFormControllerPresenter(form: CollectForm, formDef: FormDef) {
        model.createFormController(form, formDef)
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startCollectFormEntryActivity() {
        startActivity(Intent(activity, CollectFormEntryActivity::class.java))
    }

    private fun showDownloadedMenu(collectForm: CollectForm) {
        BottomSheetUtils.showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            collectForm.form.name,
            requireContext().getString(R.string.Collect_Action_FillForm),
            requireContext().getString(R.string.action_delete),
            object : ActionSeleceted {
                override fun accept(action: BottomSheetUtils.Action) {
                    if (action === BottomSheetUtils.Action.EDIT) {
                        model.getBlankFormDef(collectForm)
                    }
                    if (action === BottomSheetUtils.Action.DELETE) {
                        downloadedForms!!.remove(collectForm)
                        model.removeBlankFormDef(collectForm)
                        updateFormViews()
                    }
                }
            },
            requireContext().getString(R.string.Collect_RemoveForm_SheetTitle),
            String.format(
                requireContext().resources.getString(R.string.Collect_Subtitle_RemoveForm),
                collectForm.form.name
            ),
            requireContext().getString(R.string.action_remove),
            requireContext().getString(R.string.action_cancel)
        )
    }

    private fun checkIfDayHasPassed(): Boolean {
        val lastRefresh = Preferences.getLastCollectRefresh()
        return System.currentTimeMillis() - lastRefresh > C.DAY
    }

    private fun showBanner() {
        if (noUpdatedForms > 0) {
            binding.banner.visibility = View.VISIBLE
        } else {
            binding.banner.visibility = View.GONE
        }
    }

    private fun showJavarosa2UpgradeSheet() {
        BottomSheetUtils.showConfirmSheet(
            requireActivity().supportFragmentManager,
            null,
            getString(R.string.Javarosa_Upgrade_Warning_Description),
            getString(R.string.action_continue),
            getString(R.string.action_cancel),
            object : ActionConfirmed {
                override fun accept(isConfirmed: Boolean) {
                    if (isConfirmed) {
                        upgradeJavarosa2()
                    } else {
                        goHome()
                    }
                }
            })
    }

    private fun upgradeJavarosa2() {
        try {
            Toast.makeText(context, getString(R.string.Javarosa_Upgrade_Toast), Toast.LENGTH_LONG)
                .show()
            model.deleteCachedForms()
        } catch (t: Throwable) {
            Timber.d(t)
        }
    }

    private fun goHome() {
        if (activity == null) return
        (requireActivity() as MainActivity).selectHome()
    }

    companion object {
        fun newInstance(): BlankFormsListFragment {
            return BlankFormsListFragment()
        }
    }
}