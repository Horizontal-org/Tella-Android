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
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionSeleceted
import org.hzontal.shared_ui.utils.DialogUtils
import permissions.dispatcher.NeedsPermission
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.FragmentSubmittedFormsListBinding
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance
import rs.readahead.washington.mobile.views.activity.CollectFormEntryActivity
import rs.readahead.washington.mobile.views.adapters.CollectSubmittedFormInstanceRecycleViewAdapter
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.interfaces.ISavedFormsInterface
import timber.log.Timber

@AndroidEntryPoint
class SubmittedFormsListFragment : BaseBindingFragment<FragmentSubmittedFormsListBinding>(
    FragmentSubmittedFormsListBinding::inflate
),
    FormListInterface, ISavedFormsInterface {

    private val viewModel: SharedFormsViewModel by viewModels()
    private var adapter: CollectSubmittedFormInstanceRecycleViewAdapter? = null

    override fun getFormListType(): FormListInterface.Type {
        return FormListInterface.Type.SUBMITTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = CollectSubmittedFormInstanceRecycleViewAdapter(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObservers()
    }

    override fun onResume() {
        super.onResume()
        listSubmittedForms()
    }

    private fun initObservers() {
        viewModel.onFormInstanceDeleteSuccess.observe(
            viewLifecycleOwner
        ) { success: Boolean ->
            onFormInstanceDeleted(
                success
            )
        }

        viewModel.onSubmittedFormInstanceListSuccess.observe(
            viewLifecycleOwner
        ) { instances: List<CollectFormInstance> ->
            onFormInstanceListSuccess(
                instances
            )
        }

        viewModel.onFormInstanceListError.observe(
            viewLifecycleOwner
        ) { error: Throwable? ->
            onFormInstanceListError(
                error
            )
        }

        viewModel.onInstanceFormDefSuccess.observe(viewLifecycleOwner) { instance ->
            startCreateInstanceFormController(instance)
        }

        viewModel.onCreateFormController.observe(viewLifecycleOwner) {
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

    private fun startCreateInstanceFormController(instance: CollectFormInstance) {
        viewModel.createFormController(instance)
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

    private fun onFormInstanceDeleted(success: Boolean) {
        if (success) {
            DialogUtils.showBottomMessage(
                baseActivity,
                getString(R.string.collect_toast_form_deleted),
                false
            )
            listSubmittedForms()
        }
    }

    private fun onFormInstanceListSuccess(instances: List<CollectFormInstance?>) {
        binding.blankSubmittedFormsInfo.visibility =
            if (instances.isEmpty()) View.VISIBLE else View.GONE
        adapter!!.setInstances(instances)
    }

    private fun onFormInstanceListError(error: Throwable?) {
        Timber.d(error, javaClass.name)
    }

    private fun listSubmittedForms() {
        viewModel.listSubmitFormInstances()
    }

    override fun showFormsMenu(instance: CollectFormInstance) {
        BottomSheetUtils.showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            instance.instanceName,
            requireContext().getString(R.string.collect_sent_action_edit_to_resend),
            requireContext().getString(R.string.action_delete),
            object : ActionSeleceted {
                override fun accept(action: BottomSheetUtils.Action) {
                    if (action === BottomSheetUtils.Action.EDIT) {
                        viewModel.getInstanceFormDef(instance.id)
                        // MyApplication.bus().post(ShowFormInstanceEntryEvent(instance.id))
                    }
                    if (action === BottomSheetUtils.Action.DELETE) {
                        deleteFormInstance(instance.id)
                    }
                }
            },
            requireContext().getString(R.string.Collect_DeleteForm_SheetTitle),
            requireContext().getString(R.string.collect_dialog_text_delete_sent_form),
            requireContext().getString(R.string.action_delete),
            requireContext().getString(R.string.action_cancel)
        )
    }

    override fun showFormInstance(instance: CollectFormInstance?) {
        if (instance != null) {
            viewModel.getInstanceFormDef(instance.id)
        }
    }

    override fun reSubmitForm(instance: CollectFormInstance?) {}
    fun deleteFormInstance(instanceId: Long) {
        viewModel.deleteFormInstance(instanceId)
    }

    companion object {
        fun newInstance(): SubmittedFormsListFragment {
            return SubmittedFormsListFragment()
        }
    }

    fun initView() {
        val mLayoutManager: RecyclerView.LayoutManager = LinearLayoutManager(activity)
        binding.submittFormInstances.layoutManager = mLayoutManager
        binding.submittFormInstances.adapter = adapter
    }
}