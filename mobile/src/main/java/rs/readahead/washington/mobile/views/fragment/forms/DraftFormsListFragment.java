package rs.readahead.washington.mobile.views.fragment.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;
import org.hzontal.shared_ui.utils.DialogUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.ShowFormInstanceEntryEvent;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.mvp.contract.ICollectFormInstanceListPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectFormInstanceListPresenter;
import rs.readahead.washington.mobile.views.adapters.CollectDraftFormInstanceRecycleViewAdapter;
import rs.readahead.washington.mobile.views.interfaces.ISavedFormsInterface;
import timber.log.Timber;


public class DraftFormsListFragment extends FormListFragment implements
        ICollectFormInstanceListPresenterContract.IView, ISavedFormsInterface {
    @BindView(R.id.draftFormInstances)
    RecyclerView recyclerView;
    @BindView(R.id.blank_draft_forms_info)
    TextView blankFormsInfo;
    private SharedFormsViewModel model = null;
    private Unbinder unbinder;
    private CollectDraftFormInstanceRecycleViewAdapter adapter;
    private CollectFormInstanceListPresenter presenter;

    public static DraftFormsListFragment newInstance() {
        return new DraftFormsListFragment();
    }

    @Override
    public Type getFormListType() {
        return Type.DRAFT;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new CollectDraftFormInstanceRecycleViewAdapter(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_draft_forms_list, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);
        model = new ViewModelProvider(this).get(SharedFormsViewModel.class);

        createPresenter();
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initObservers();
        listDraftForms();
    }

    private void initObservers(){
        model.getOnDraftFormInstanceListSuccess().observe(getViewLifecycleOwner(),this::onDraftFormInstanceListSuccess);
        model.getOnFormInstanceListError().observe(getViewLifecycleOwner(),this::onFormInstanceListError);
        model.getOnFormInstanceDeleteSuccess().observe(getViewLifecycleOwner(),this::onFormInstanceDeleted);
    }

    private void onFormInstanceDeleted(Boolean success) {
        if (success) {
            DialogUtils.showBottomMessage(getActivity(),getString(R.string.collect_toast_form_deleted), false);
            this.listDraftForms();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        destroyPresenter();
        super.onDestroy();
    }


    private void onDraftFormInstanceListSuccess(List<CollectFormInstance> instances) {
        blankFormsInfo.setVisibility(instances.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.setInstances(instances);
    }

    @Override
    public void onFormInstanceListSuccess(List<CollectFormInstance> instances) {
        blankFormsInfo.setVisibility(instances.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.setInstances(instances);
    }

    public void onFormInstanceListError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    public void listDraftForms() {
        /*if (model != null) {
            model.listDraftFormInstances();
        }*/
        if (presenter != null) {
            presenter.listDraftFormInstances();
        }
    }

    private void createPresenter() {
        if (presenter == null) {
            presenter = new CollectFormInstanceListPresenter(this);
        }
    }

    private void destroyPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    @Override
    public void showFormsMenu(CollectFormInstance instance) {
        BottomSheetUtils.showEditDeleteMenuSheet(
                requireActivity().getSupportFragmentManager(),
                instance.getInstanceName(),
                requireContext().getString(R.string.Collect_Action_FillForm),
                requireContext().getString(R.string.action_delete),
                action -> {
                    if (action == BottomSheetUtils.Action.EDIT) {
                        MyApplication.bus().post(new ShowFormInstanceEntryEvent(instance.getId()));
                    }
                    if (action == BottomSheetUtils.Action.DELETE) {
                        deleteFormInstance(instance.getId());
                    }
                },
                requireContext().getString(R.string.Collect_DeleteDraftForm_SheetTitle),
                requireContext().getString(R.string.Collect_DeleteDraftForm_SheetExpl),
                requireContext().getString(R.string.action_delete),
                requireContext().getString(R.string.action_cancel)
        );
    }

    @Override
    public void reSubmitForm(CollectFormInstance instance) {
    }

    public void deleteFormInstance(long instanceId) {
        model.deleteFormInstance(instanceId);
    }
}
