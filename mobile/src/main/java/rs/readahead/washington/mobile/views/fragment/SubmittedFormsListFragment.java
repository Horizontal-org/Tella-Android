package rs.readahead.washington.mobile.views.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.hzontal.shared_ui.bottomsheet.CustomBottomSheetFragment;
import org.hzontal.shared_ui.utils.DialogUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.ShowFormInstanceEntryEvent;
import rs.readahead.washington.mobile.databinding.FragmentSubmittedFormsListBinding;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.mvp.contract.ICollectFormInstanceListPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectFormInstanceListPresenter;
import rs.readahead.washington.mobile.util.ViewUtil;
import rs.readahead.washington.mobile.views.adapters.CollectSubmittedFormInstanceRecycleViewAdapter;
import rs.readahead.washington.mobile.views.custom.TopSpaceItemDecoration;
import timber.log.Timber;

public class SubmittedFormsListFragment extends FormListFragment implements
        ICollectFormInstanceListPresenterContract.IView,
        SubmittedFormListListener {
    private CollectSubmittedFormInstanceRecycleViewAdapter adapter;
    private CollectFormInstanceListPresenter presenter;

    private FragmentSubmittedFormsListBinding binding;

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
        adapter = new CollectSubmittedFormInstanceRecycleViewAdapter(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSubmittedFormsListBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        binding.submittedFormInstances.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.submittedFormInstances.addItemDecoration(new TopSpaceItemDecoration(ViewUtil.getDpInPixels(requireContext(), 16)));
        binding.submittedFormInstances.setAdapter(adapter);

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
        binding = null;
    }

    @Override
    public void onFormInstanceListSuccess(List<CollectFormInstance> instances) {
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
    public void showOptionsBottomSheet(CollectFormInstance instance) {
        final CustomBottomSheetFragment bottomSheet = CustomBottomSheetFragment.Companion.with(requireActivity().getSupportFragmentManager())
                .page(R.layout.submitted_form_fragment_btm_sheet)
                .cancellable(true);

        bottomSheet.holder(new EditDeleteBottomSheetHolder(), holder -> {
            holder.title.setText(instance.getFormName());
            holder.title2.setText(String.format("Delete \"%s\"?", instance.getFormName())); // todo: string
            holder.edit.setOnClickListener(v -> {
                MyApplication.bus().post(new ShowFormInstanceEntryEvent(instance.getId()));
                bottomSheet.dismiss();
            });
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

    static class EditDeleteBottomSheetHolder extends BlankFormsListFragment.DeleteBottomSheetHolder {
        private TextView edit;

        @Override
        public void bindView(@NonNull View view) {
            super.bindView(view);
            edit = view.findViewById(R.id.edit);
        }
    }
}
