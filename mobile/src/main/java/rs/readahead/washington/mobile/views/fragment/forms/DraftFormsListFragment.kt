package rs.readahead.washington.mobile.views.fragment.forms

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionSeleceted
import org.hzontal.shared_ui.utils.DialogUtils
import permissions.dispatcher.NeedsPermission
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.FragmentDraftFormsListBinding
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance
import rs.readahead.washington.mobile.views.activity.CollectFormEntryActivity
import rs.readahead.washington.mobile.views.adapters.CollectDraftFormInstanceRecycleViewAdapter
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.interfaces.ISavedFormsInterface
import timber.log.Timber

class DraftFormsListFragment : BaseBindingFragment<FragmentDraftFormsListBinding>(
    FragmentDraftFormsListBinding::inflate
),
    FormListInterfce, ISavedFormsInterface {

    private val model: SharedFormsViewModel by viewModels()
    private var adapter: CollectDraftFormInstanceRecycleViewAdapter? = null

    override fun getFormListType(): FormListInterfce.Type {
        return FormListInterfce.Type.DRAFT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = CollectDraftFormInstanceRecycleViewAdapter(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObservers()
        listDraftForms()
    }

    private fun initObservers() {
        model.onDraftFormInstanceListSuccess.observe(
            viewLifecycleOwner
        ) { instances: List<CollectFormInstance> ->
            onDraftFormInstanceListSuccess(
                instances
            )
        }
        model.onFormInstanceListError.observe(
            viewLifecycleOwner
        ) { error: Throwable? ->
            onFormInstanceListError(
                error
            )
        }
        model.onFormInstanceDeleteSuccess.observe(
            viewLifecycleOwner,
            { success: Boolean? ->
                onFormInstanceDeleted(
                    success!!
                )
            })
        model.onInstanceFormDefSuccess.observe(viewLifecycleOwner, { instance ->
            startCreateInstanceFormController(instance)
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
    }

    private fun startCreateInstanceFormController(instance: CollectFormInstance) {
        model.createFormController(instance)
    }

    private fun onFormInstanceDeleted(success: Boolean) {
        if (success) {
            DialogUtils.showBottomMessage(
                activity,
                getString(R.string.collect_toast_form_deleted),
                false
            )
            listDraftForms()
        }
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

    private fun onDraftFormInstanceListSuccess(instances: List<CollectFormInstance>) {
        binding.blankDraftFormsInfo.visibility =
            if (instances.isEmpty()) View.VISIBLE else View.GONE
        adapter!!.setInstances(instances)
    }

    private fun onFormInstanceListError(error: Throwable?) {
        Timber.d(error, javaClass.name)
    }

    fun listDraftForms() {
        model.listDraftFormInstances()
    }

    override fun showFormsMenu(instance: CollectFormInstance) {
        BottomSheetUtils.showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            instance.instanceName,
            requireContext().getString(R.string.Collect_Action_FillForm),
            requireContext().getString(R.string.action_delete),
            object : ActionSeleceted {
                override fun accept(action: BottomSheetUtils.Action) {
                    if (action === BottomSheetUtils.Action.EDIT) {
                        showFormInstance(instance)
                        //model.getInstanceFormDef(instance.id)
                        //MyApplication.bus().post(ShowFormInstanceEntryEvent(instance.id))
                    }
                    if (action === BottomSheetUtils.Action.DELETE) {
                        deleteFormInstance(instance.id)
                    }
                }
            },
            requireContext().getString(R.string.Collect_DeleteDraftForm_SheetTitle),
            requireContext().getString(R.string.Collect_DeleteDraftForm_SheetExpl),
            requireContext().getString(R.string.action_delete),
            requireContext().getString(R.string.action_cancel)
        )
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startCollectFormEntryActivity() {
        startActivity(Intent(activity, CollectFormEntryActivity::class.java))
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

    override fun showFormInstance(instance: CollectFormInstance) {
        model.getInstanceFormDef(instance.id)
    }

    override fun reSubmitForm(instance: CollectFormInstance?) {}
    fun deleteFormInstance(instanceId: Long) {
        model.deleteFormInstance(instanceId)
    }

    companion object {
        fun newInstance(): DraftFormsListFragment {
            return DraftFormsListFragment()
        }
    }

    fun initView() {
        val mLayoutManager: RecyclerView.LayoutManager = LinearLayoutManager(activity)
        binding.draftFormInstances.layoutManager = mLayoutManager
        binding.draftFormInstances.adapter = adapter
    }
}