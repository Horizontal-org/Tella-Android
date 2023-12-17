package rs.readahead.washington.mobile.views.fragment.forms

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionSeleceted
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.ReSubmitFormInstanceEvent
import rs.readahead.washington.mobile.databinding.FragmentOutboxFormListBinding
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance
import rs.readahead.washington.mobile.views.adapters.CollectOutboxFormInstanceRecycleViewAdapter
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.interfaces.ISavedFormsInterface
import timber.log.Timber

@AndroidEntryPoint
class OutboxFormListFragment : BaseBindingFragment<FragmentOutboxFormListBinding>(
    FragmentOutboxFormListBinding::inflate
),
    FormListInterface, ISavedFormsInterface {

    private val model: SharedFormsViewModel by viewModels()
    private var adapter: CollectOutboxFormInstanceRecycleViewAdapter? = null

    override fun getFormListType(): FormListInterface.Type {
        return FormListInterface.Type.OUTBOX
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = CollectOutboxFormInstanceRecycleViewAdapter(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObservers()
    }

    override fun onResume() {
        super.onResume()
        listOutboxForms()
    }

    private fun initObservers() {
        model.onFormInstanceDeleteSuccess.observe(
            viewLifecycleOwner,
            { success: Boolean? ->
                onFormInstanceDeleted(
                    success!!
                )
            })

        model.onOutboxFormInstanceListSuccess.observe(
            viewLifecycleOwner
        ) { instances: List<CollectFormInstance> ->
            onFormInstanceListSuccess(
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
    }

    private fun onFormInstanceDeleted(success: Boolean) {
        if (success) {
            DialogUtils.showBottomMessage(
                baseActivity,
                getString(R.string.collect_toast_form_deleted),
                false
            )
            listOutboxForms()
        }
    }

    private fun onFormInstanceListSuccess(instances: List<CollectFormInstance?>) {
        binding.blankOutboxFormsInfo.visibility =
            if (instances.isEmpty()) View.VISIBLE else View.GONE
        adapter!!.setInstances(instances)
    }

    private fun onFormInstanceListError(error: Throwable?) {
        Timber.d(error, javaClass.name)
    }

    fun listOutboxForms() {
        model.listOutboxFormInstances()
    }

    override fun showFormsMenu(instance: CollectFormInstance) {
        BottomSheetUtils.showThreeOptionMenuSheet(
            requireActivity().supportFragmentManager,
            instance.instanceName,
            requireContext().getString(R.string.action_view),
            null,  //requireContext().getString(R.string.action_share)
            requireContext().getString(R.string.action_delete),
            object : ActionSeleceted {
                override fun accept(action: BottomSheetUtils.Action) {
                    if (action === BottomSheetUtils.Action.VIEW) {
                        reSubmitForm(instance)
                    }
                    /* This is for a sharing form over SMS
                    if (action === BottomSheetUtils.Action.SHARE) {
                        if (formSubmitter != null) {
                            formSubmitter.getCompactFormTextToShare();
                        }
                    }*/
                    if (action === BottomSheetUtils.Action.DELETE) {
                        deleteFormInstance(instance.id)
                    }
                }
            },
            requireContext().getString(R.string.Collect_DeleteForm_SheetTitle),
            requireContext().getString(R.string.Collect_DeleteForm_SheetExpl),
            requireContext().getString(R.string.action_delete),
            requireContext().getString(R.string.action_cancel)
        )
    }

    override fun showFormInstance(instance: CollectFormInstance?) {
        if (instance != null) {
            model.getInstanceFormDef(instance.id)
        }
    }

    override fun reSubmitForm(instance: CollectFormInstance?) {
        MyApplication.bus().post(ReSubmitFormInstanceEvent(instance))
    }

    fun deleteFormInstance(instanceId: Long) {
        model.deleteFormInstance(instanceId)
    }

    companion object {
        fun newInstance(): OutboxFormListFragment {
            return OutboxFormListFragment()
        }
    }

    fun initView() {
        val mLayoutManager: RecyclerView.LayoutManager = LinearLayoutManager(activity)
        binding.submittFormInstances.layoutManager = mLayoutManager
        binding.submittFormInstances.adapter = adapter
    }
}