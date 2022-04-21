package rs.readahead.washington.mobile.views.fragment.forms;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;
import org.hzontal.shared_ui.utils.DialogUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.ReSubmitFormInstanceEvent;
import rs.readahead.washington.mobile.bus.event.ShowFormInstanceEntryEvent;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.mvp.contract.ICollectFormInstanceListPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectFormInstanceListPresenter;
import rs.readahead.washington.mobile.views.adapters.CollectOutboxFormInstanceRecycleViewAdapter;
import rs.readahead.washington.mobile.views.interfaces.ISavedFormsInterface;
import timber.log.Timber;


public class OutboxFormListFragment extends FormListFragment implements
        ICollectFormInstanceListPresenterContract.IView, ISavedFormsInterface {

    @BindView(R.id.submittFormInstances)
    RecyclerView recyclerView;
    @BindView(R.id.blank_submitted_forms_info)
    TextView blankFormsInfo;

    private Unbinder unbinder;
    private CollectOutboxFormInstanceRecycleViewAdapter adapter;
    private CollectFormInstanceListPresenter presenter;
    private SharedFormsViewModel model = null;


    public static OutboxFormListFragment newInstance() {
        return new OutboxFormListFragment();
    }

    @Override
    public Type getFormListType() {
        return Type.OUTBOX;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new CollectOutboxFormInstanceRecycleViewAdapter(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_outbox_form_list, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        model = new ViewModelProvider(this).get(SharedFormsViewModel.class);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);

        createPresenter();

        return rootView;
    }

    private void initObservers(){
        model.getOnFormInstanceDeleteSuccess().observe(getViewLifecycleOwner(),this::onFormInstanceDeleted);
    }

    private void onFormInstanceDeleted(Boolean success) {
        if (success) {
            DialogUtils.showBottomMessage(getActivity(),getString(R.string.collect_toast_form_deleted), false);
            this.listOutboxForms();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initObservers();
        listOutboxForms();
    }

    @Override
    public void onDestroy() {
        destroyPresenter();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onFormInstanceListSuccess(List<CollectFormInstance> instances) {
        blankFormsInfo.setVisibility(instances.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.setInstances(instances);
    }

    @Override
    public void onFormInstanceListError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    public void listOutboxForms() {
        if (presenter != null) {
            presenter.listOutboxFormInstances();
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
        BottomSheetUtils.showThreeOptionMenuSheet(
                requireActivity().getSupportFragmentManager(),
                instance.getInstanceName(),
                requireContext().getString(R.string.action_view),
                null,//requireContext().getString(R.string.action_share)
                requireContext().getString(R.string.action_delete),
                action -> {
                    if (action == BottomSheetUtils.Action.VIEW) {
                        reSubmitForm(instance);
                    }
                    if (action == BottomSheetUtils.Action.SHARE) {
                        /*if (formSubmitter != null) {
                            formSubmitter.getCompactFormTextToShare();
                        }*/
                    }
                    if (action == BottomSheetUtils.Action.DELETE) {
                        deleteFormInstance(instance.getId());
                    }
                },
                requireContext().getString(R.string.Collect_RemoveForm_SheetTitle),
                String.format(requireContext().getResources().getString(R.string.Collect_Subtitle_RemoveForm), instance.getInstanceName()),
                requireContext().getString(R.string.action_remove),
                requireContext().getString(R.string.action_cancel)
        );
    }

    @Override
    public void reSubmitForm(CollectFormInstance instance) {
        MyApplication.bus().post(new ReSubmitFormInstanceEvent(instance));
    }

    public void deleteFormInstance(long instanceId) {
        model.deleteFormInstance(instanceId);
    }
}
