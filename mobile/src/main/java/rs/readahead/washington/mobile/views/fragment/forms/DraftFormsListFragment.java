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

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.mvp.contract.ICollectFormInstanceListPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectFormInstanceListPresenter;
import rs.readahead.washington.mobile.views.adapters.CollectDraftFormInstanceRecycleViewAdapter;
import timber.log.Timber;


public class DraftFormsListFragment extends FormListFragment {
    @BindView(R.id.draftFormInstances)
    RecyclerView recyclerView;
    @BindView(R.id.blank_draft_forms_info)
    TextView blankFormsInfo;
    private SharedFormsViewModel model = null;
    private Unbinder unbinder;
    private CollectDraftFormInstanceRecycleViewAdapter adapter;

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
        adapter = new CollectDraftFormInstanceRecycleViewAdapter();
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


        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listDraftForms();
    }

    private void initObservers(){
        model.getOnFormInstanceListSuccess().observe(getViewLifecycleOwner(),this::onFormInstanceListSuccess);
        model.getOnFormInstanceListError().observe(getViewLifecycleOwner(),this::onFormInstanceListError);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void onFormInstanceListSuccess(List<CollectFormInstance> instances) {
        blankFormsInfo.setVisibility(instances.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.setInstances(instances);
    }

    private void onFormInstanceListError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    public void listDraftForms() {
        if (model != null) {
            model.listDraftFormInstances();
        }
    }


}
