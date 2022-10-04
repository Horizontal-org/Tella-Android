package rs.readahead.washington.mobile.views.fragment.forms

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionSeleceted
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.ReSubmitFormInstanceEvent
import rs.readahead.washington.mobile.databinding.FragmentOutboxFormListBinding
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance
import rs.readahead.washington.mobile.mvp.contract.ICollectFormInstanceListPresenterContract
import rs.readahead.washington.mobile.mvp.presenter.CollectFormInstanceListPresenter
import rs.readahead.washington.mobile.views.adapters.CollectOutboxFormInstanceRecycleViewAdapter
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.interfaces.ISavedFormsInterface
import timber.log.Timber

class OutboxFormListFragment : BaseBindingFragment<FragmentOutboxFormListBinding>(
    FragmentOutboxFormListBinding::inflate
),
    FormListInterfce, ICollectFormInstanceListPresenterContract.IView, ISavedFormsInterface {
    var recyclerView: RecyclerView? = null
    var blankFormsInfo: TextView? = null
    private var adapter: CollectOutboxFormInstanceRecycleViewAdapter? = null
    private var presenter: CollectFormInstanceListPresenter? = null
    private var model: SharedFormsViewModel? = null
    override fun getFormListType(): FormListInterfce.Type {
        return FormListInterfce.Type.OUTBOX
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = CollectOutboxFormInstanceRecycleViewAdapter(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = binding!!.submittFormInstances
        blankFormsInfo = binding!!.blankSubmittedFormsInfo
        model = ViewModelProvider(this).get(
            SharedFormsViewModel::class.java
        )
        val mLayoutManager: RecyclerView.LayoutManager = LinearLayoutManager(activity)
        recyclerView!!.layoutManager = mLayoutManager
        recyclerView!!.adapter = adapter
        createPresenter()

        initObservers()
        listOutboxForms()
    }


    private fun initObservers() {
        model!!.onFormInstanceDeleteSuccess.observe(
            viewLifecycleOwner,
            { success: Boolean? ->
                onFormInstanceDeleted(
                    success!!
                )
            })
    }

    private fun onFormInstanceDeleted(success: Boolean) {
        if (success) {
            DialogUtils.showBottomMessage(
                activity,
                getString(R.string.collect_toast_form_deleted),
                false
            )
            listOutboxForms()
        }
    }

    override fun onDestroy() {
        destroyPresenter()
        super.onDestroy()
    }

    override fun onFormInstanceListSuccess(instances: List<CollectFormInstance?>) {
        blankFormsInfo!!.visibility = if (instances.isEmpty()) View.VISIBLE else View.GONE
        adapter!!.setInstances(instances)
    }

    override fun onFormInstanceListError(error: Throwable?) {
        Timber.d(error, javaClass.name)
    }

    fun listOutboxForms() {
        if (presenter != null) {
            presenter!!.listOutboxFormInstances()
        }
    }

    private fun createPresenter() {
        if (presenter == null) {
            presenter = CollectFormInstanceListPresenter(this)
        }
    }

    private fun destroyPresenter() {
        if (presenter != null) {
            presenter!!.destroy()
            presenter = null
        }
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
                    if (action === BottomSheetUtils.Action.SHARE) {
                        /*if (formSubmitter != null) {
                            formSubmitter.getCompactFormTextToShare();
                        }*/
                    }
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

    override fun reSubmitForm(instance: CollectFormInstance?) {
        MyApplication.bus().post(ReSubmitFormInstanceEvent(instance))
    }

    fun deleteFormInstance(instanceId: Long) {
        model!!.deleteFormInstance(instanceId)
    }

    companion object {
        fun newInstance(): OutboxFormListFragment {
            return OutboxFormListFragment()
        }
    }
}