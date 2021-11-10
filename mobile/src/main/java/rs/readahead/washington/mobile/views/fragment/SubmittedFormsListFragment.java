package rs.readahead.washington.mobile.views.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import rs.readahead.washington.mobile.views.adapters.CollectSubmittedFormInstanceRecycleViewAdapter;
import rs.readahead.washington.mobile.views.fragment.forms.FormListFragment;
import timber.log.Timber;


public class SubmittedFormsListFragment extends FormListFragment implements
        ICollectFormInstanceListPresenterContract.IView {

    @BindView(R.id.submittFormInstances)
    RecyclerView recyclerView;
    @BindView(R.id.blank_submitted_forms_info)
    TextView blankFormsInfo;

    private Unbinder unbinder;
    private CollectSubmittedFormInstanceRecycleViewAdapter adapter;
    private CollectFormInstanceListPresenter presenter;


    public static SubmittedFormsListFragment newInstance() {
        return new SubmittedFormsListFragment();
    }

    @Override
    public Type getFormListType() {
        return Type.SUBMITTED;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new CollectSubmittedFormInstanceRecycleViewAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_submitted_forms_list, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);

        createPresenter();

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listSubmittedForms();
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

    public void listSubmittedForms() {
        if (presenter != null) {
            presenter.listSubmitFormInstances();
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

}
