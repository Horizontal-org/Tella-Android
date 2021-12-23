package rs.readahead.washington.mobile.views.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.hzontal.shared_ui.bottomsheet.CustomBottomSheetFragment;
import org.hzontal.shared_ui.utils.DialogUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.databinding.FragmentDraftFormsListBinding;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.mvp.contract.ICollectFormInstanceListPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectFormInstanceListPresenter;
import rs.readahead.washington.mobile.util.ViewUtil;
import rs.readahead.washington.mobile.views.adapters.CollectDraftFormInstanceRecycleViewAdapter;
import rs.readahead.washington.mobile.views.custom.TopSpaceItemDecoration;
import timber.log.Timber;

public class DraftFormsListFragment extends FormListFragment implements
        ICollectFormInstanceListPresenterContract.IView,
        DraftFormListListener {
    private FragmentDraftFormsListBinding binding;

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
        binding = FragmentDraftFormsListBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        binding.draftFormInstances.setLayoutManager(new LinearLayoutManager(getActivity()));
        binding.draftFormInstances.addItemDecoration(new TopSpaceItemDecoration(ViewUtil.getDpInPixels(requireContext(), 16)));
        binding.draftFormInstances.setAdapter(adapter);

        createPresenter();

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listDraftForms();
    }

    @Override
    public void onDestroy() {
        destroyPresenter();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onFormInstanceListSuccess(List<CollectFormInstance> instances) {
        binding.emptyContainer.setVisibility(instances.isEmpty() ? View.VISIBLE : View.GONE);
        binding.notEmptyContainer.setVisibility(instances.isEmpty() ? View.GONE : View.VISIBLE);

        adapter.setInstances(instances);
    }

    @Override
    public void onFormInstanceListError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onFormInstanceDeleteSuccess(String name) {
        DialogUtils.showBottomMessage(requireActivity(), String.format("%s has been deleted.", name), false); // todo: string
    }

    @Override
    public void onFormInstanceDeleteError(Throwable throwable) {
        // todo: do this
    }

    @Override
    public void showDeleteBottomSheet(CollectFormInstance instance) {
        final CustomBottomSheetFragment bottomSheet = CustomBottomSheetFragment.Companion.with(requireActivity().getSupportFragmentManager())
                .page(R.layout.form_fragment_delete_btm_sheet)
                .cancellable(true);

        bottomSheet.holder(new BlankFormsListFragment.DeleteBottomSheetHolder(), holder -> {
            holder.title.setText(instance.getInstanceName());
            holder.title2.setText(String.format("Delete \"%s\"?", instance.getInstanceName())); // todo: string
            holder.delete.setOnClickListener(v -> {
                holder.firstStep.setVisibility(View.GONE);
                holder.secondStep.setVisibility(View.VISIBLE);
            });
            holder.yes.setOnClickListener(v -> {
                // presenter.deleteFormInstance(instance);
                onFormInstanceDeleteSuccess(instance.getInstanceName());
                bottomSheet.dismiss();
            });
            holder.no.setOnClickListener(v -> bottomSheet.dismiss());
        }).transparentBackground().launch();
    }

    public void listDraftForms() {
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
}
